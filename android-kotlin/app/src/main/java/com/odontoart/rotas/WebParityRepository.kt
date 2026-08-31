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

/**
 * Data source de paridade. Os nomes de RPC e filtros seguem o app web e as migrations
 * do repositorio Odontoart-rotas, evitando consultas simplificadas que alteram o resultado.
 */
class WebParityRepository(
    supabaseUrlRaw: String = BuildConfig.SUPABASE_URL,
    supabaseAnonKeyRaw: String = BuildConfig.SUPABASE_ANON_KEY,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val supabaseUrl = supabaseUrlRaw.trim().trimEnd('/')
    private val supabaseAnonKey = supabaseAnonKeyRaw.trim()

    suspend fun fetchCompanies(
        session: UserSession,
        search: String = "",
        searchMode: String = "geral",
        situacao: String? = null,
        page: Int = 1,
        pageSize: Int = 50,
    ): CompanyPage = withContext(Dispatchers.IO) {
        val safePage = page.coerceAtLeast(1)
        val safeSize = pageSize.coerceIn(1, 100)
        val normalizedSearch = search.replace("%", "").trim()
        val mode = searchMode.takeIf { it in setOf("codigo", "empresa", "geral") } ?: "geral"

        val listPayload = JSONObject()
            .put("p_page_size", safeSize)
            .put("p_page_offset", (safePage - 1) * safeSize)
            .put("p_search", normalizedSearch.ifBlank { JSONObject.NULL })
            .put("p_search_mode", mode)
            .put("p_situacao", situacao?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)

        val firstPage = JSONArray(rpc(session, "get_empresas_first_page_v1", listPayload))
        val orderedIds = buildList {
            repeat(firstPage.length()) { index ->
                firstPage.optJSONObject(index)?.stringOrNull("id")?.let(::add)
            }
        }

        val enrichedById = if (orderedIds.isEmpty()) emptyMap() else fetchCompaniesByIds(session, orderedIds).associateBy { it.id }
        val rows = firstPage.mapObjects { row ->
            val id = row.stringOrNull("id") ?: return@mapObjects null
            enrichedById[id] ?: ClienteListItem(
                id = id,
                codigo = row.stringOrNull("codigo"),
                cnpj = null,
                empresa = row.stringOrNull("empresa"),
                nomeFantasia = null,
                vidasQtde = null,
                pessoa = row.stringOrNull("pessoa"),
                contato = row.stringOrNull("contato"),
                grupo = row.stringOrNull("grupo"),
                situacao = row.stringOrNull("situacao"),
                categoria = null,
                perfilVisita = row.stringOrNull("perfil_visita"),
                regraVisitaObservacao = null,
                endereco = null,
                complemento = null,
                bairro = null,
                cidade = row.stringOrNull("cidade"),
                uf = row.stringOrNull("uf"),
            )
        }

        val countPayload = JSONObject()
            .put("p_search", normalizedSearch.ifBlank { JSONObject.NULL })
            .put("p_search_mode", mode)
            .put("p_situacao", situacao?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
        val total = rpc(session, "get_empresas_count_v1", countPayload).trim().toLongOrNull() ?: rows.size.toLong()
        CompanyPage(rows, total, safePage, safeSize)
    }

    suspend fun fetchCompanyById(session: UserSession, id: String): ClienteListItem? = withContext(Dispatchers.IO) {
        fetchCompaniesByIds(session, listOf(id)).firstOrNull()
    }

    suspend fun fetchAgendaCompanies(
        session: UserSession,
        filters: JSONObject = emptyAgendaFilters(),
        companyName: String = "",
        companyCode: String = "",
        page: Int = 1,
        pageSize: Int = 25,
    ): AgendaCompanyPage = withContext(Dispatchers.IO) {
        val safePage = page.coerceAtLeast(1)
        val safeSize = pageSize.coerceIn(1, 100)
        val payload = JSONObject()
            .put("p_page_size", safeSize)
            .put("p_page_offset", (safePage - 1) * safeSize)
            .put("p_filters", filters)
            .put("p_company_name", companyName.trim().ifBlank { JSONObject.NULL })
            .put("p_company_code", companyCode.trim().ifBlank { JSONObject.NULL })
        val rowsJson = JSONArray(rpc(session, "get_rotas_agenda_first_page_v2", payload))
        val rows = rowsJson.mapObjects { row ->
            AgendaCompanyItem(
                id = row.stringOrNull("id") ?: return@mapObjects null,
                codigo = row.stringOrNull("cod_1"),
                empresa = row.stringOrNull("empresa"),
                pessoa = row.stringOrNull("pessoa"),
                contato = row.stringOrNull("contato"),
                perfilVisita = row.stringOrNull("perfil_visita"),
                corte = row.numberOrNull("corte"),
                vencimento = row.numberOrNull("venc"),
                valor = row.numberOrNull("valor"),
                endereco = row.stringOrNull("endereco"),
                complemento = row.stringOrNull("complemento"),
                bairro = row.stringOrNull("bairro"),
                cidade = row.stringOrNull("cidade"),
                uf = row.stringOrNull("uf"),
                supervisor = row.stringOrNull("supervisor"),
                vendedor = row.stringOrNull("vendedor"),
                nomeFantasia = row.stringOrNull("nome_fantasia"),
                grupo = row.stringOrNull("grupo"),
                situacao = row.stringOrNull("situacao"),
                categoria = row.stringOrNull("categoria"),
                dataUltimaVisita = row.stringOrNull("data_da_ultima_visita"),
                vidasUltimaVisita = row.optIntOrNull("visit_completed_vidas"),
                visitGeneratedAt = row.stringOrNull("visit_generated_at"),
            )
        }

        val countPayload = JSONObject()
            .put("p_filters", filters)
            .put("p_company_name", companyName.trim().ifBlank { JSONObject.NULL })
            .put("p_company_code", companyCode.trim().ifBlank { JSONObject.NULL })
        val total = rpc(session, "get_rotas_agenda_count_v1", countPayload).trim().toLongOrNull() ?: rows.size.toLong()
        AgendaCompanyPage(rows, total, safePage, safeSize)
    }

    private fun fetchCompaniesByIds(session: UserSession, ids: List<String>): List<ClienteListItem> {
        if (ids.isEmpty()) return emptyList()
        val select = listOf(
            "id", "codigo", "cnpj", "empresa", "nome_fantasia", "vidas_qtde", "pessoa", "contato", "grupo",
            "situacao", "categoria", "perfil_visita", "regra_visita_observacao", "endereco", "complemento",
            "bairro", "cidade", "uf", "cep", "corte", "venc", "valor", "reajuste_pct", "competencia",
            "data_da_ultima_visita", "obs_comercial", "obs",
        ).joinToString(",")
        val inValue = "(${ids.joinToString(",")})"
        val url = restUrl("clientes", mapOf("select" to select, "id" to "in.$inValue"))
        val request = authorizedRequest(url, session.accessToken).get().build()
        return JSONArray(execute(request)).mapObjects(::parseCompany)
    }

    private fun parseCompany(row: JSONObject): ClienteListItem? {
        val id = row.stringOrNull("id") ?: return null
        return ClienteListItem(
            id = id,
            codigo = row.stringOrNull("codigo"),
            cnpj = row.stringOrNull("cnpj"),
            empresa = row.stringOrNull("empresa"),
            nomeFantasia = row.stringOrNull("nome_fantasia"),
            vidasQtde = row.optIntOrNull("vidas_qtde"),
            pessoa = row.stringOrNull("pessoa"),
            contato = row.stringOrNull("contato"),
            grupo = row.stringOrNull("grupo"),
            situacao = row.stringOrNull("situacao"),
            categoria = row.stringOrNull("categoria"),
            perfilVisita = row.stringOrNull("perfil_visita"),
            regraVisitaObservacao = row.stringOrNull("regra_visita_observacao"),
            endereco = row.stringOrNull("endereco"),
            complemento = row.stringOrNull("complemento"),
            bairro = row.stringOrNull("bairro"),
            cidade = row.stringOrNull("cidade"),
            uf = row.stringOrNull("uf"),
            cep = row.stringOrNull("cep"),
            corte = row.numberOrNull("corte"),
            venc = row.numberOrNull("venc"),
            valor = row.numberOrNull("valor"),
            reajustePct = row.numberOrNull("reajuste_pct"),
            competencia = row.stringOrNull("competencia"),
            dataUltimaVisita = row.stringOrNull("data_da_ultima_visita"),
            obsComercial = row.stringOrNull("obs_comercial"),
            obs = row.stringOrNull("obs"),
        )
    }

    private fun rpc(session: UserSession, function: String, payload: JSONObject): String {
        ensureConfigured()
        val request = authorizedRequest("$supabaseUrl/rest/v1/rpc/$function", session.accessToken)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return execute(request)
    }

    private fun restUrl(table: String, query: Map<String, String>): String {
        val builder = "$supabaseUrl/rest/v1/$table".toHttpUrl().newBuilder()
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    private fun authorizedRequest(url: String, accessToken: String): Request.Builder = Request.Builder()
        .url(url)
        .header("apikey", supabaseAnonKey)
        .header("Authorization", "Bearer $accessToken")
        .header("Content-Type", "application/json")

    private fun execute(request: Request): String {
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val server = runCatching {
                    val json = JSONObject(body)
                    json.stringOrNull("message") ?: json.stringOrNull("error") ?: body
                }.getOrDefault(body)
                error("Falha na API (${response.code}): ${server.ifBlank { "sem detalhes" }}")
            }
            return body
        }
    }

    private fun ensureConfigured() {
        check(supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()) {
            "Configuracao do Supabase ausente no aplicativo."
        }
    }

    private fun emptyAgendaFilters(): JSONObject = JSONObject()
        .put("global", "")
        .put("columns", JSONObject())
        .put("dateRanges", JSONObject().put("data_da_ultima_visita", JSONObject()))
        .put("ranges", JSONObject().put("vidas_ultima_visita", JSONObject()))

    private fun JSONObject.stringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().ifBlank { null }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return runCatching { getInt(key) }.getOrNull() ?: optString(key).trim().toDoubleOrNull()?.toInt()
    }

    private fun JSONObject.numberOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return runCatching { getDouble(key) }.getOrNull() ?: optString(key).replace(",", ".").toDoubleOrNull()
    }

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T?): List<T> = buildList {
        repeat(length()) { index -> optJSONObject(index)?.let(transform)?.let(::add) }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

data class CompanyPage(val rows: List<ClienteListItem>, val total: Long, val page: Int, val pageSize: Int)

data class AgendaCompanyItem(
    val id: String,
    val codigo: String?,
    val empresa: String?,
    val pessoa: String?,
    val contato: String?,
    val perfilVisita: String?,
    val corte: Double?,
    val vencimento: Double?,
    val valor: Double?,
    val endereco: String?,
    val complemento: String?,
    val bairro: String?,
    val cidade: String?,
    val uf: String?,
    val supervisor: String?,
    val vendedor: String?,
    val nomeFantasia: String?,
    val grupo: String?,
    val situacao: String?,
    val categoria: String?,
    val dataUltimaVisita: String?,
    val vidasUltimaVisita: Int?,
    val visitGeneratedAt: String?,
)

data class AgendaCompanyPage(val rows: List<AgendaCompanyItem>, val total: Long, val page: Int, val pageSize: Int)
