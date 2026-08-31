package com.odontoart.rotas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class SupabaseApi(
    supabaseUrlRaw: String = BuildConfig.SUPABASE_URL,
    supabaseAnonKeyRaw: String = BuildConfig.SUPABASE_ANON_KEY,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val supabaseUrl = supabaseUrlRaw.trim().trimEnd('/')
    private val supabaseAnonKey = supabaseAnonKeyRaw.trim()

    fun isConfigured(): Boolean = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()

    suspend fun signIn(email: String, password: String): UserSession = withContext(Dispatchers.IO) {
        ensureConfigured()
        val payload = JSONObject().put("email", email).put("password", password)
        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/token?grant_type=password")
            .header("apikey", supabaseAnonKey)
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val responseJson = JSONObject(execute(request))
        val userJson = responseJson.optJSONObject("user") ?: error("Resposta de login invalida.")
        UserSession(
            accessToken = responseJson.stringOrNull("access_token") ?: error("Login sem access token."),
            refreshToken = responseJson.stringOrNull("refresh_token") ?: error("Login sem refresh token."),
            userId = userJson.stringOrNull("id") ?: error("Usuario autenticado sem id."),
            userEmail = userJson.stringOrNull("email"),
        )
    }

    suspend fun fetchProfile(session: UserSession): UserProfile = withContext(Dispatchers.IO) {
        val full = "id,user_id,role,display_name,nome,is_inactive,can_access_pre_cadastro,can_access_next_route_dashboard,force_reauth_after,supervisor_id,vendedor_id,created_at"
        val fallback = "id,user_id,role,display_name,nome,is_inactive,can_access_pre_cadastro,can_access_next_route_dashboard,supervisor_id,vendedor_id,created_at"
        val row = try {
            fetchSingleRow(session, "profiles", mapOf("select" to full, "user_id" to "eq.${session.userId}", "limit" to "1"))
        } catch (e: IllegalStateException) {
            if (!e.message.orEmpty().contains("force_reauth_after", true)) throw e
            fetchSingleRow(session, "profiles", mapOf("select" to fallback, "user_id" to "eq.${session.userId}", "limit" to "1"))
        }
        if (row == null) {
            UserProfile(null, session.userId, session.userEmail, null, null)
        } else {
            UserProfile(
                profileId = row.stringOrNull("id"),
                userId = row.stringOrNull("user_id") ?: session.userId,
                displayName = row.stringOrNull("display_name") ?: session.userEmail,
                nome = row.stringOrNull("nome"),
                role = row.stringOrNull("role"),
                isInactive = row.optBooleanSafe("is_inactive"),
                canAccessPreCadastro = row.optBooleanSafe("can_access_pre_cadastro"),
                canAccessNextRouteDashboard = row.optBooleanSafe("can_access_next_route_dashboard"),
                forceReauthAfter = row.stringOrNull("force_reauth_after"),
                supervisorId = row.stringOrNull("supervisor_id"),
                vendedorId = row.stringOrNull("vendedor_id"),
                createdAt = row.stringOrNull("created_at"),
            )
        }
    }

    suspend fun fetchRoutes(session: UserSession): List<RouteItem> = withContext(Dispatchers.IO) {
        getRows(session, "routes", mapOf("select" to "id,name,date,assigned_to_user_id,created_by,created_at", "order" to "date.desc.nullslast", "limit" to "500"))
            .mapObjects { row ->
                RouteItem(
                    id = row.stringOrNull("id") ?: return@mapObjects null,
                    name = row.stringOrNull("name") ?: "ROTA",
                    date = row.stringOrNull("date"),
                    assignedToUserId = row.stringOrNull("assigned_to_user_id"),
                    createdBy = row.stringOrNull("created_by"),
                    createdAt = row.stringOrNull("created_at"),
                )
            }
    }

    suspend fun createRoute(session: UserSession, name: String, date: String?): RouteItem = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("name", name).put("date", date ?: JSONObject.NULL)
            .put("assigned_to_user_id", JSONObject.NULL).put("created_by", session.userId)
        val row = writeRows(session, "routes", "POST", payload, true).optJSONObject(0) ?: error("Nao foi possivel criar a rota.")
        RouteItem(
            id = row.stringOrNull("id") ?: error("Rota criada sem id."),
            name = row.stringOrNull("name") ?: name,
            date = row.stringOrNull("date"),
            assignedToUserId = row.stringOrNull("assigned_to_user_id"),
            createdBy = row.stringOrNull("created_by"),
            createdAt = row.stringOrNull("created_at"),
        )
    }

    suspend fun deleteRoute(session: UserSession, routeId: String) = withContext(Dispatchers.IO) {
        deleteRows(session, "routes", mapOf("id" to "eq.$routeId"))
    }

    suspend fun fetchRouteStops(session: UserSession, routeId: String): List<RouteStopItem> = withContext(Dispatchers.IO) {
        getRows(
            session,
            "route_stops",
            mapOf(
                "select" to "id,route_id,cliente_id,stop_order,notes,cliente:cliente_id(id,codigo,empresa,nome_fantasia,endereco,complemento,bairro,cidade,uf)",
                "route_id" to "eq.$routeId",
                "order" to "stop_order.asc.nullslast",
            ),
        ).mapObjects { row ->
            RouteStopItem(
                id = row.stringOrNull("id") ?: return@mapObjects null,
                routeId = row.stringOrNull("route_id"),
                clienteId = row.stringOrNull("cliente_id"),
                stopOrder = row.optIntOrNull("stop_order"),
                notes = row.stringOrNull("notes"),
                cliente = parseCliente(row.opt("cliente")),
            )
        }
    }

    suspend fun updateRouteStopOrder(session: UserSession, stopId: String, order: Int) = withContext(Dispatchers.IO) {
        patchRows(session, "route_stops", mapOf("id" to "eq.$stopId"), JSONObject().put("stop_order", order))
    }

    suspend fun fetchVisits(session: UserSession, role: UserRole?, limit: Int = 500): List<VisitItem> = withContext(Dispatchers.IO) {
        val query = linkedMapOf(
            "select" to "id,cliente_id,visit_date,assigned_to_user_id,assigned_to_name,visit_type,supervisor_reason,register_mode,visit_time,perfil_visita,perfil_visita_opcoes,route_id,route_stop_id,route_stop_order,completed_at,completed_vidas,no_visit_reason,no_visit_observation,instructions,cliente:cliente_id(id,codigo,empresa,nome_fantasia,endereco,complemento,bairro,cidade,uf)",
            "order" to "visit_date.desc,route_stop_order.asc.nullslast",
            "limit" to limit.coerceIn(1, 1000).toString(),
        )
        if (role == UserRole.VENDEDOR) query["assigned_to_user_id"] = "eq.${session.userId}"
        getRows(session, "visits", query).mapObjects { row ->
            VisitItem(
                id = row.stringOrNull("id") ?: return@mapObjects null,
                clienteId = row.stringOrNull("cliente_id"),
                visitDate = row.stringOrNull("visit_date") ?: return@mapObjects null,
                assignedToUserId = row.stringOrNull("assigned_to_user_id"),
                assignedToName = row.stringOrNull("assigned_to_name"),
                visitType = row.stringOrNull("visit_type"),
                supervisorReason = row.stringOrNull("supervisor_reason"),
                registerMode = row.stringOrNull("register_mode"),
                visitTime = row.stringOrNull("visit_time"),
                perfilVisita = row.stringOrNull("perfil_visita"),
                perfilVisitaOpcoes = row.stringOrNull("perfil_visita_opcoes"),
                routeId = row.stringOrNull("route_id"),
                routeStopId = row.stringOrNull("route_stop_id"),
                routeStopOrder = row.optIntOrNull("route_stop_order"),
                completedAt = row.stringOrNull("completed_at"),
                completedVidas = row.optIntOrNull("completed_vidas"),
                noVisitReason = row.stringOrNull("no_visit_reason"),
                noVisitObservation = row.stringOrNull("no_visit_observation"),
                instructions = row.stringOrNull("instructions"),
                cliente = parseCliente(row.opt("cliente")),
            )
        }
    }

    suspend fun completeVisit(session: UserSession, visitId: String, vidas: Int) = withContext(Dispatchers.IO) {
        patchRows(
            session, "visits", mapOf("id" to "eq.$visitId"),
            JSONObject().put("completed_at", java.time.Instant.now().toString()).put("completed_vidas", vidas)
                .put("no_visit_reason", JSONObject.NULL).put("no_visit_observation", JSONObject.NULL),
        )
    }

    suspend fun registerNoVisit(session: UserSession, visitId: String, reason: String, observation: String?) = withContext(Dispatchers.IO) {
        patchRows(
            session, "visits", mapOf("id" to "eq.$visitId"),
            JSONObject().put("completed_at", JSONObject.NULL).put("completed_vidas", JSONObject.NULL)
                .put("no_visit_reason", reason).put("no_visit_observation", observation?.takeIf { it.isNotBlank() } ?: JSONObject.NULL),
        )
    }

    suspend fun fetchClients(session: UserSession, search: String = "", limit: Int = 150): List<ClienteListItem> = withContext(Dispatchers.IO) {
        val query = linkedMapOf(
            "select" to "id,codigo,cnpj,empresa,nome_fantasia,vidas_qtde,pessoa,contato,grupo,situacao,categoria,perfil_visita,regra_visita_observacao,endereco,complemento,bairro,cidade,uf",
            "order" to "empresa.asc.nullslast",
            "limit" to limit.coerceIn(1, 500).toString(),
        )
        val term = search.trim().replace(",", " ")
        if (term.isNotBlank()) query["or"] = "(codigo.ilike.*$term*,empresa.ilike.*$term*,nome_fantasia.ilike.*$term*,cnpj.ilike.*$term*)"
        getRows(session, "clientes", query).mapObjects(::parseClientListItem)
    }

    suspend fun createClient(session: UserSession, payload: JSONObject): ClienteListItem = withContext(Dispatchers.IO) {
        val id = writeRows(session, "clientes", "POST", payload, true).optJSONObject(0)?.stringOrNull("id") ?: error("Empresa criada sem id.")
        fetchClients(session, payload.optString("codigo"), 20).firstOrNull { it.id == id } ?: error("Nao foi possivel recarregar a empresa criada.")
    }

    suspend fun updateClient(session: UserSession, clientId: String, payload: JSONObject) = withContext(Dispatchers.IO) {
        patchRows(session, "clientes", mapOf("id" to "eq.$clientId"), payload)
    }

    suspend fun deleteClient(session: UserSession, clientId: String) = withContext(Dispatchers.IO) {
        deleteRows(session, "clientes", mapOf("id" to "eq.$clientId"))
    }

    suspend fun fetchVendorAcceptanceDates(session: UserSession): Pair<List<String>, List<DigitalAcceptanceItem>> = withContext(Dispatchers.IO) {
        val visits = getRows(
            session, "visits",
            mapOf("select" to "visit_date", "assigned_to_user_id" to "eq.${session.userId}", "completed_at" to "not.is.null", "no_visit_reason" to "is.null", "order" to "visit_date.asc", "limit" to "1000"),
        )
        val dates = visits.mapObjects { it.stringOrNull("visit_date") }.distinct().sorted()
        val existing = fetchAcceptanceRows(session, mapOf("vendor_user_id" to "eq.${session.userId}", "order" to "entry_date.desc"))
        dates to existing
    }

    suspend fun fetchDigitalSummary(session: UserSession, date: String): List<DigitalAcceptanceItem> = withContext(Dispatchers.IO) {
        fetchAcceptanceRows(session, mapOf("entry_date" to "eq.$date", "order" to "vendor_name.asc.nullslast"))
    }

    suspend fun registerDigitalAcceptance(session: UserSession, vendorName: String?, date: String, vidas: Int) = withContext(Dispatchers.IO) {
        writeRows(
            session, "aceite_digital", "POST",
            JSONObject().put("vendor_user_id", session.userId).put("vendor_name", vendorName ?: JSONObject.NULL)
                .put("entry_date", date).put("vidas", vidas).put("created_by", session.userId),
            false,
        )
    }

    suspend fun fetchQueueControls(session: UserSession): List<QueueControlItem> = withContext(Dispatchers.IO) {
        getRows(
            session, "queue_release_controls_view",
            mapOf("select" to "empresa_id,codigo,empresa,cnpj,data_contrato,waiting_days_snapshot,eligible_at,state,effective_state,manual_block_until,manual_reason,days_remaining,updated_at", "order" to "created_at.desc", "limit" to "500"),
        ).mapObjects { row ->
            QueueControlItem(
                empresaId = row.stringOrNull("empresa_id") ?: return@mapObjects null,
                codigo = row.stringOrNull("codigo"), empresa = row.stringOrNull("empresa"), cnpj = row.stringOrNull("cnpj"),
                dataContrato = row.stringOrNull("data_contrato"), waitingDaysSnapshot = row.optIntOrNull("waiting_days_snapshot"),
                eligibleAt = row.stringOrNull("eligible_at"), state = row.stringOrNull("state") ?: "PENDING_WAIT",
                effectiveState = row.stringOrNull("effective_state") ?: "PENDING_WAIT", manualBlockUntil = row.stringOrNull("manual_block_until"),
                manualReason = row.stringOrNull("manual_reason"), daysRemaining = row.optIntOrNull("days_remaining"), updatedAt = row.stringOrNull("updated_at"),
            )
        }
    }

    suspend fun applyQueueAction(session: UserSession, empresaId: String, action: String, waitingDays: Int? = null, blockDays: Int? = null, reason: String? = null) = withContext(Dispatchers.IO) {
        rpc(
            session, "queue_release_apply_action",
            JSONObject().put("p_empresa_id", empresaId).put("p_action", action)
                .put("p_waiting_days", waitingDays ?: JSONObject.NULL).put("p_block_days", blockDays ?: JSONObject.NULL)
                .put("p_reason", reason ?: JSONObject.NULL),
        )
    }

    suspend fun fetchKpiSnapshots(session: UserSession, periodDays: Int = 30): List<KpiSnapshotItem> = withContext(Dispatchers.IO) {
        val rows = getRows(
            session, "kpi_sync_snapshots",
            mapOf("select" to "id,codigo,empresa,categoria,vidas_qtde,status,snapshot_at,delta,vendas_qtde,cancelamentos_qtde", "period_days" to "eq.$periodDays", "order" to "snapshot_at.desc", "limit" to "1000"),
        )
        val byCode = linkedMapOf<String, KpiSnapshotItem>()
        rows.mapObjects { row ->
            val code = row.stringOrNull("codigo") ?: return@mapObjects null
            if (code in byCode) return@mapObjects null
            val item = KpiSnapshotItem(
                id = row.stringOrNull("id") ?: return@mapObjects null, codigo = code, empresa = row.stringOrNull("empresa"),
                categoria = row.stringOrNull("categoria"), vidasQtde = row.optIntOrNull("vidas_qtde"), status = row.stringOrNull("status"),
                snapshotAt = row.stringOrNull("snapshot_at"), delta = row.optIntOrNull("delta") ?: 0,
                vendasQtde = row.optIntOrNull("vendas_qtde") ?: 0, cancelamentosQtde = row.optIntOrNull("cancelamentos_qtde") ?: 0,
            )
            byCode[code] = item
            item
        }
        byCode.values.toList()
    }

    suspend fun fetchSystemNews(session: UserSession, role: UserRole?): List<SystemNewsItem> = withContext(Dispatchers.IO) {
        val query = linkedMapOf(
            "select" to "id,titulo,descricao,tipo,modulo,roles_permitidos,data_publicacao,ativo",
            "ativo" to "eq.true", "order" to "data_publicacao.desc,created_at.desc", "limit" to "100",
        )
        if (role != null) query["roles_permitidos"] = "cs.{${role.name}}"
        getRows(session, "system_news", query).mapObjects { row ->
            SystemNewsItem(
                id = row.stringOrNull("id") ?: return@mapObjects null, titulo = row.stringOrNull("titulo") ?: "Sem titulo",
                descricao = row.stringOrNull("descricao") ?: "", tipo = row.stringOrNull("tipo") ?: "AVISO",
                modulo = row.stringOrNull("modulo") ?: "Geral", rolesPermitidos = row.optJSONArray("roles_permitidos")?.toStringList().orEmpty(),
                dataPublicacao = row.stringOrNull("data_publicacao") ?: "", ativo = row.optBooleanSafe("ativo", true),
            )
        }
    }

    suspend fun markSystemNewsRead(session: UserSession, updateId: String) = withContext(Dispatchers.IO) {
        rpc(session, "system_news_mark_as_read", JSONObject().put("p_update_id", updateId))
    }

    suspend fun fetchAuditLogs(session: UserSession, action: String? = null, table: String? = null): List<AuditLogItem> = withContext(Dispatchers.IO) {
        val query = linkedMapOf("select" to "id,table_name,action,record_id,user_id,user_name,old_data,new_data,created_at", "order" to "created_at.desc", "limit" to "200")
        if (!action.isNullOrBlank() && action != "all") query["action"] = "eq.$action"
        if (!table.isNullOrBlank() && table != "all") query["table_name"] = "eq.$table"
        getRows(session, "audit_logs", query).mapObjects { row ->
            AuditLogItem(
                id = row.stringOrNull("id") ?: return@mapObjects null, tableName = row.stringOrNull("table_name") ?: "-",
                action = row.stringOrNull("action") ?: "-", recordId = row.stringOrNull("record_id"), userId = row.stringOrNull("user_id"),
                userName = row.stringOrNull("user_name"), oldData = row.optJSONObject("old_data")?.toString(2), newData = row.optJSONObject("new_data")?.toString(2),
                createdAt = row.stringOrNull("created_at") ?: "",
            )
        }
    }

    suspend fun fetchManagedProfiles(session: UserSession): List<ManagedProfileItem> = withContext(Dispatchers.IO) {
        getRows(
            session, "profiles",
            mapOf("select" to "id,user_id,role,display_name,nome,can_access_pre_cadastro,can_access_next_route_dashboard,supervisor_id,vendedor_id,is_inactive", "order" to "display_name.asc.nullslast", "limit" to "500"),
        ).mapObjects { row ->
            ManagedProfileItem(
                id = row.stringOrNull("id") ?: return@mapObjects null, userId = row.stringOrNull("user_id"), role = row.stringOrNull("role") ?: "",
                displayName = row.stringOrNull("display_name"), nome = row.stringOrNull("nome"), canAccessPreCadastro = row.optBooleanSafe("can_access_pre_cadastro"),
                canAccessNextRouteDashboard = row.optBooleanSafe("can_access_next_route_dashboard"), supervisorId = row.stringOrNull("supervisor_id"),
                vendedorId = row.stringOrNull("vendedor_id"), isInactive = row.optBooleanSafe("is_inactive"),
            )
        }
    }

    suspend fun setProfileInactive(session: UserSession, profileId: String, inactive: Boolean) = withContext(Dispatchers.IO) {
        patchRows(session, "profiles", mapOf("id" to "eq.$profileId"), JSONObject().put("is_inactive", inactive))
    }

    private fun parseClientListItem(row: JSONObject): ClienteListItem? {
        val id = row.stringOrNull("id") ?: return null
        return ClienteListItem(
            id = id, codigo = row.stringOrNull("codigo"), cnpj = row.stringOrNull("cnpj"), empresa = row.stringOrNull("empresa"),
            nomeFantasia = row.stringOrNull("nome_fantasia"), vidasQtde = row.optIntOrNull("vidas_qtde"), pessoa = row.stringOrNull("pessoa"),
            contato = row.stringOrNull("contato"), grupo = row.stringOrNull("grupo"), situacao = row.stringOrNull("situacao"), categoria = row.stringOrNull("categoria"),
            perfilVisita = row.stringOrNull("perfil_visita"), regraVisitaObservacao = row.stringOrNull("regra_visita_observacao"), endereco = row.stringOrNull("endereco"),
            complemento = row.stringOrNull("complemento"), bairro = row.stringOrNull("bairro"), cidade = row.stringOrNull("cidade"), uf = row.stringOrNull("uf"),
        )
    }

    private fun fetchAcceptanceRows(session: UserSession, extra: Map<String, String>): List<DigitalAcceptanceItem> {
        val query = linkedMapOf("select" to "id,vendor_user_id,vendor_name,entry_date,vidas")
        query.putAll(extra)
        return getRowsBlocking(session, "aceite_digital", query).mapObjects { row ->
            DigitalAcceptanceItem(
                id = row.stringOrNull("id") ?: return@mapObjects null, vendorUserId = row.stringOrNull("vendor_user_id"),
                vendorName = row.stringOrNull("vendor_name"), entryDate = row.stringOrNull("entry_date") ?: return@mapObjects null,
                vidas = row.optIntOrNull("vidas") ?: 0,
            )
        }
    }

    private fun parseCliente(value: Any?): ClienteInfo? {
        return when (value) {
            is JSONObject -> {
                val id = value.stringOrNull("id") ?: return null
                ClienteInfo(
                    id = id, codigo = value.stringOrNull("codigo"), empresa = value.stringOrNull("empresa"), nomeFantasia = value.stringOrNull("nome_fantasia"),
                    endereco = value.stringOrNull("endereco"), complemento = value.stringOrNull("complemento"), bairro = value.stringOrNull("bairro"),
                    cidade = value.stringOrNull("cidade"), uf = value.stringOrNull("uf"),
                )
            }
            is JSONArray -> if (value.length() > 0) parseCliente(value.optJSONObject(0)) else null
            else -> null
        }
    }

    private fun fetchSingleRow(session: UserSession, table: String, query: Map<String, String>): JSONObject? = getRowsBlocking(session, table, query).optJSONObject(0)

    private suspend fun getRows(session: UserSession, table: String, query: Map<String, String> = emptyMap()): JSONArray =
        withContext(Dispatchers.IO) { getRowsBlocking(session, table, query) }

    private fun getRowsBlocking(session: UserSession, table: String, query: Map<String, String>): JSONArray {
        ensureConfigured()
        val body = execute(authorizedRequestBuilder(restUrl(table, query), session.accessToken).get().build())
        return if (body.isBlank()) JSONArray() else JSONArray(body)
    }

    private fun writeRows(session: UserSession, table: String, method: String, payload: JSONObject, preferRepresentation: Boolean, query: Map<String, String> = emptyMap()): JSONArray {
        ensureConfigured()
        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val builder = authorizedRequestBuilder(restUrl(table, query), session.accessToken)
        if (preferRepresentation) builder.header("Prefer", "return=representation")
        when (method) {
            "POST" -> builder.post(body)
            "PATCH" -> builder.patch(body)
            else -> builder.method(method, body)
        }
        val raw = execute(builder.build())
        return when {
            raw.isBlank() -> JSONArray()
            raw.trimStart().startsWith("[") -> JSONArray(raw)
            raw.trimStart().startsWith("{") -> JSONArray().put(JSONObject(raw))
            else -> JSONArray()
        }
    }

    private fun patchRows(session: UserSession, table: String, query: Map<String, String>, payload: JSONObject) {
        writeRows(session, table, "PATCH", payload, false, query)
    }

    private fun deleteRows(session: UserSession, table: String, query: Map<String, String>) {
        ensureConfigured()
        execute(authorizedRequestBuilder(restUrl(table, query), session.accessToken).delete().build())
    }

    private fun rpc(session: UserSession, function: String, payload: JSONObject): String {
        ensureConfigured()
        return execute(
            authorizedRequestBuilder("$supabaseUrl/rest/v1/rpc/$function", session.accessToken)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE)).build(),
        )
    }

    private fun authorizedRequestBuilder(url: String, accessToken: String): Request.Builder = Request.Builder()
        .url(url).header("apikey", supabaseAnonKey).header("Authorization", "Bearer $accessToken").header("Content-Type", "application/json")

    private fun restUrl(table: String, query: Map<String, String> = emptyMap()): String {
        val builder = "$supabaseUrl/rest/v1/$table".toHttpUrl().newBuilder()
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    private fun execute(request: Request): String {
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Falha na API (${response.code}): ${extractServerError(body)}")
            return body
        }
    }

    private fun extractServerError(body: String): String {
        if (body.isBlank()) return "sem detalhes"
        return runCatching {
            val json = JSONObject(body)
            json.stringOrNull("message") ?: json.stringOrNull("msg") ?: json.stringOrNull("error_description") ?: json.stringOrNull("error") ?: body
        }.getOrElse { body }
    }

    private fun ensureConfigured() {
        if (!isConfigured()) error("SUPABASE_URL/SUPABASE_ANON_KEY nao configurados no Android.")
    }

    private fun JSONObject.stringOrNull(key: String): String? = if (!has(key) || isNull(key)) null else optString(key).trim().ifEmpty { null }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Number -> value.toInt()
            is String -> value.toDoubleOrNull()?.toInt()
            else -> null
        }
    }

    private fun JSONObject.optBooleanSafe(key: String, defaultValue: Boolean = false): Boolean {
        if (!has(key) || isNull(key)) return defaultValue
        return when (val value = opt(key)) {
            is Boolean -> value
            is String -> value.equals("true", true)
            is Number -> value.toInt() != 0
            else -> defaultValue
        }
    }

    private fun JSONArray.toStringList(): List<String> = buildList {
        repeat(length()) { index -> optString(index).trim().takeIf { it.isNotEmpty() }?.let(::add) }
    }

    private fun <T : Any> JSONArray.mapObjects(transform: (JSONObject) -> T?): List<T> = buildList {
        repeat(length()) { index -> optJSONObject(index)?.let(transform)?.let(::add) }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
