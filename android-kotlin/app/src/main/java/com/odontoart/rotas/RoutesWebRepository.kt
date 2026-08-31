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

class RoutesWebRepository(
    supabaseUrlRaw: String = BuildConfig.SUPABASE_URL,
    supabaseAnonKeyRaw: String = BuildConfig.SUPABASE_ANON_KEY,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val supabaseUrl = supabaseUrlRaw.trim().trimEnd('/')
    private val supabaseAnonKey = supabaseAnonKeyRaw.trim()

    suspend fun createRouteWithStops(
        session: UserSession,
        name: String,
        date: String?,
        clienteIds: List<String>,
    ): String = withContext(Dispatchers.IO) {
        require(name.trim().isNotBlank()) { "Informe o nome da rota." }
        val routePayload = JSONObject()
            .put("name", name.trim())
            .put("date", date?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            .put("assigned_to_user_id", JSONObject.NULL)
            .put("created_by", session.userId)
        val routeRows = writeRows(session, "routes", routePayload)
        val routeId = routeRows.optJSONObject(0)?.stringOrNull("id")
            ?: error("Rota criada sem identificador.")

        val uniqueIds = clienteIds.map(String::trim).filter(String::isNotBlank).distinct()
        if (uniqueIds.isNotEmpty()) {
            val stops = JSONArray()
            uniqueIds.forEachIndexed { index, clienteId ->
                stops.put(
                    JSONObject()
                        .put("route_id", routeId)
                        .put("cliente_id", clienteId)
                        .put("stop_order", index + 1)
                        .put("notes", JSONObject.NULL),
                )
            }
            writeRows(session, "route_stops", stops)
        }
        routeId
    }

    suspend fun addStops(
        session: UserSession,
        routeId: String,
        clienteIds: List<String>,
        startOrder: Int,
    ) = withContext(Dispatchers.IO) {
        val uniqueIds = clienteIds.map(String::trim).filter(String::isNotBlank).distinct()
        if (uniqueIds.isEmpty()) return@withContext
        val rows = JSONArray()
        uniqueIds.forEachIndexed { index, clienteId ->
            rows.put(
                JSONObject()
                    .put("route_id", routeId)
                    .put("cliente_id", clienteId)
                    .put("stop_order", startOrder + index)
                    .put("notes", JSONObject.NULL),
            )
        }
        writeRows(session, "route_stops", rows)
    }

    suspend fun deleteStop(session: UserSession, stopId: String) = withContext(Dispatchers.IO) {
        val request = authorized(restUrl("route_stops", mapOf("id" to "eq.$stopId")), session.accessToken)
            .delete()
            .build()
        execute(request)
    }

    suspend fun updateStopOrder(session: UserSession, stopId: String, order: Int) = withContext(Dispatchers.IO) {
        val request = authorized(restUrl("route_stops", mapOf("id" to "eq.$stopId")), session.accessToken)
            .header("Prefer", "return=minimal")
            .patch(JSONObject().put("stop_order", order).toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        execute(request)
    }

    private fun writeRows(session: UserSession, table: String, payload: Any): JSONArray {
        val body = when (payload) {
            is JSONObject -> payload.toString()
            is JSONArray -> payload.toString()
            else -> error("Payload invalido.")
        }
        val request = authorized(restUrl(table, emptyMap()), session.accessToken)
            .header("Prefer", "return=representation")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return JSONArray(execute(request))
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
        check(supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()) { "Configuracao do Supabase ausente." }
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

    private fun JSONObject.stringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().ifBlank { null }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
