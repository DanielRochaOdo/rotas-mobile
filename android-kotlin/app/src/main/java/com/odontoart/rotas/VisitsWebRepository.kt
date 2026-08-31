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
import java.time.Instant

/** Mantem as regras de persistencia de Visitas equivalentes ao fluxo do web. */
class VisitsWebRepository(
    supabaseUrlRaw: String = BuildConfig.SUPABASE_URL,
    supabaseAnonKeyRaw: String = BuildConfig.SUPABASE_ANON_KEY,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val supabaseUrl = supabaseUrlRaw.trim().trimEnd('/')
    private val supabaseAnonKey = supabaseAnonKeyRaw.trim()

    suspend fun completeVisit(
        session: UserSession,
        visit: VisitItem,
        vidas: Int,
        perfilVisita: String? = visit.perfilVisita,
        perfilOpcoes: String? = visit.perfilVisitaOpcoes,
        visitTime: String? = visit.visitTime,
        registerMode: String? = visit.registerMode,
    ) = withContext(Dispatchers.IO) {
        require(vidas >= 0) { "Quantidade de vidas deve ser um numero inteiro valido." }
        val completedAt = Instant.now().toString()
        val payload = JSONObject()
            .put("completed_at", completedAt)
            .put("completed_vidas", vidas)
            .put("perfil_visita", perfilVisita?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            .put("perfil_visita_opcoes", perfilOpcoes?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            .put("visit_time", visitTime?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            .put("register_mode", registerMode?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            .put("registered_by_user_id", session.userId)
            .put("no_visit_reason", JSONObject.NULL)
            .put("no_visit_observation", JSONObject.NULL)
        patch(session, "visits", mapOf("id" to "eq.${visit.id}"), payload)

        val clienteId = visit.clienteId ?: return@withContext
        val visitDateKey = visit.visitDate.take(10)
        if (visitDateKey.length != 10) return@withContext
        val current = getRows(
            session,
            "clientes",
            mapOf(
                "select" to "id,data_da_ultima_visita",
                "id" to "eq.$clienteId",
                "limit" to "1",
            ),
        ).optJSONObject(0)
        val currentLast = current?.stringOrNull("data_da_ultima_visita")?.take(10)
        if (currentLast.isNullOrBlank() || currentLast <= visitDateKey) {
            patch(
                session,
                "clientes",
                mapOf("id" to "eq.$clienteId"),
                JSONObject()
                    .put("data_da_ultima_visita", "${visitDateKey}T12:00:00.000Z")
                    .put("visit_completed_vidas", vidas),
            )
        }
    }

    suspend fun registerNoVisit(
        session: UserSession,
        visitId: String,
        reason: String,
        observation: String?,
    ) = withContext(Dispatchers.IO) {
        require(reason.isNotBlank()) { "Informe o motivo da visita nao realizada." }
        patch(
            session,
            "visits",
            mapOf("id" to "eq.$visitId"),
            JSONObject()
                .put("completed_at", Instant.now().toString())
                .put("completed_vidas", JSONObject.NULL)
                .put("no_visit_reason", reason.trim())
                .put("no_visit_observation", observation?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                .put("registered_by_user_id", session.userId),
        )
    }

    private fun getRows(session: UserSession, table: String, query: Map<String, String>): JSONArray {
        val request = authorized(restUrl(table, query), session.accessToken).get().build()
        return JSONArray(execute(request))
    }

    private fun patch(session: UserSession, table: String, query: Map<String, String>, payload: JSONObject) {
        val request = authorized(restUrl(table, query), session.accessToken)
            .header("Prefer", "return=minimal")
            .patch(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        execute(request)
    }

    private fun restUrl(table: String, query: Map<String, String>): String {
        val builder = "$supabaseUrl/rest/v1/$table".toHttpUrl().newBuilder()
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    private fun authorized(url: String, token: String): Request.Builder = Request.Builder()
        .url(url)
        .header("apikey", supabaseAnonKey)
        .header("Authorization", "Bearer $token")
        .header("Content-Type", "application/json")

    private fun execute(request: Request): String {
        ensureConfigured()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    val json = JSONObject(body)
                    json.stringOrNull("message") ?: json.stringOrNull("error") ?: body
                }.getOrDefault(body)
                error("Falha na API (${response.code}): ${message.ifBlank { "sem detalhes" }}")
            }
            return body
        }
    }

    private fun ensureConfigured() {
        check(supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()) { "Configuracao do Supabase ausente." }
    }

    private fun JSONObject.stringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().ifBlank { null }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
