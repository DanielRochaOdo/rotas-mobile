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

        val requestBody = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/token?grant_type=password")
            .header("apikey", supabaseAnonKey)
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val responseJson = JSONObject(execute(request))
        val userJson = responseJson.optJSONObject("user")
            ?: throw IllegalStateException("Resposta de login invalida.")

        val userId = userJson.stringOrNull("id")
            ?: throw IllegalStateException("Usuario autenticado sem id.")

        val accessToken = responseJson.stringOrNull("access_token")
            ?: throw IllegalStateException("Login sem access token.")
        val refreshToken = responseJson.stringOrNull("refresh_token")
            ?: throw IllegalStateException("Login sem refresh token.")

        UserSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            userEmail = userJson.stringOrNull("email"),
        )
    }

    suspend fun fetchProfile(session: UserSession): UserProfile = withContext(Dispatchers.IO) {
        ensureConfigured()

        val url = restUrl(
            "profiles",
            mapOf(
                "select" to "user_id,display_name,role",
                "user_id" to "eq.${session.userId}",
                "limit" to "1",
            ),
        )

        val request = authorizedRequestBuilder(url, session.accessToken)
            .get()
            .build()

        val rows = JSONArray(execute(request))
        if (rows.length() == 0) {
            return@withContext UserProfile(
                userId = session.userId,
                displayName = session.userEmail,
                role = null,
            )
        }

        val row = rows.getJSONObject(0)
        UserProfile(
            userId = row.stringOrNull("user_id") ?: session.userId,
            displayName = row.stringOrNull("display_name") ?: session.userEmail,
            role = row.stringOrNull("role"),
        )
    }

    suspend fun fetchRoutes(session: UserSession): List<RouteItem> = withContext(Dispatchers.IO) {
        ensureConfigured()

        val url = restUrl(
            "routes",
            mapOf(
                "select" to "id,name,date,assigned_to_user_id,created_by,created_at",
                "order" to "date.desc.nullslast",
            ),
        )

        val request = authorizedRequestBuilder(url, session.accessToken)
            .get()
            .build()

        val rows = JSONArray(execute(request))
        buildList(rows.length()) {
            repeat(rows.length()) { index ->
                val row = rows.getJSONObject(index)
                val routeId = row.stringOrNull("id") ?: return@repeat
                add(
                    RouteItem(
                        id = routeId,
                        name = row.stringOrNull("name") ?: "ROTA",
                        date = row.stringOrNull("date"),
                        assignedToUserId = row.stringOrNull("assigned_to_user_id"),
                        createdBy = row.stringOrNull("created_by"),
                        createdAt = row.stringOrNull("created_at"),
                    ),
                )
            }
        }
    }

    suspend fun createRoute(
        session: UserSession,
        name: String,
        date: String?,
    ): RouteItem = withContext(Dispatchers.IO) {
        ensureConfigured()

        val payload = JSONObject()
            .put("name", name)
            .put("date", date ?: JSONObject.NULL)
            .put("assigned_to_user_id", JSONObject.NULL)
            .put("created_by", session.userId)

        val request = authorizedRequestBuilder(restUrl("routes"), session.accessToken)
            .header("Prefer", "return=representation")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val rows = JSONArray(execute(request))
        if (rows.length() == 0) {
            throw IllegalStateException("Nao foi possivel criar a rota.")
        }
        val row = rows.getJSONObject(0)
        val routeId = row.stringOrNull("id")
            ?: throw IllegalStateException("Rota criada sem id.")

        RouteItem(
            id = routeId,
            name = row.stringOrNull("name") ?: name,
            date = row.stringOrNull("date"),
            assignedToUserId = row.stringOrNull("assigned_to_user_id"),
            createdBy = row.stringOrNull("created_by"),
            createdAt = row.stringOrNull("created_at"),
        )
    }

    suspend fun deleteRoute(session: UserSession, routeId: String) = withContext(Dispatchers.IO) {
        ensureConfigured()

        val url = restUrl("routes", mapOf("id" to "eq.$routeId"))
        val request = authorizedRequestBuilder(url, session.accessToken)
            .delete()
            .build()
        execute(request)
    }

    suspend fun fetchRouteStops(session: UserSession, routeId: String): List<RouteStopItem> =
        withContext(Dispatchers.IO) {
            ensureConfigured()

            val url = restUrl(
                "route_stops",
                mapOf(
                    "select" to "id,route_id,cliente_id,stop_order,notes,cliente:cliente_id(id,codigo,empresa,nome_fantasia,endereco,complemento,bairro,cidade,uf)",
                    "route_id" to "eq.$routeId",
                    "order" to "stop_order.asc.nullslast",
                ),
            )

            val request = authorizedRequestBuilder(url, session.accessToken)
                .get()
                .build()

            val rows = JSONArray(execute(request))
            buildList(rows.length()) {
                repeat(rows.length()) { index ->
                    val row = rows.getJSONObject(index)
                    val stopId = row.stringOrNull("id") ?: return@repeat
                    val clienteData = row.opt("cliente")
                    add(
                        RouteStopItem(
                            id = stopId,
                            routeId = row.stringOrNull("route_id"),
                            clienteId = row.stringOrNull("cliente_id"),
                            stopOrder = row.optIntOrNull("stop_order"),
                            notes = row.stringOrNull("notes"),
                            cliente = parseCliente(clienteData),
                        ),
                    )
                }
            }
        }

    private fun parseCliente(value: Any?): ClienteInfo? {
        return when (value) {
            is JSONObject -> {
                val id = value.stringOrNull("id") ?: return null
                ClienteInfo(
                    id = id,
                    codigo = value.stringOrNull("codigo"),
                    empresa = value.stringOrNull("empresa"),
                    nomeFantasia = value.stringOrNull("nome_fantasia"),
                    endereco = value.stringOrNull("endereco"),
                    complemento = value.stringOrNull("complemento"),
                    bairro = value.stringOrNull("bairro"),
                    cidade = value.stringOrNull("cidade"),
                    uf = value.stringOrNull("uf"),
                )
            }
            is JSONArray -> {
                if (value.length() == 0) return null
                parseCliente(value.optJSONObject(0))
            }
            else -> null
        }
    }

    private fun authorizedRequestBuilder(url: String, accessToken: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("apikey", supabaseAnonKey)
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")

    private fun restUrl(table: String, query: Map<String, String> = emptyMap()): String {
        val baseUrl = "$supabaseUrl/rest/v1/$table".toHttpUrl()
        val builder = baseUrl.newBuilder()
        query.forEach { (key, value) ->
            builder.addQueryParameter(key, value)
        }
        return builder.build().toString()
    }

    private fun execute(request: Request): String {
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = extractServerError(body)
                throw IllegalStateException("Falha na API (${response.code}): $message")
            }
            return body
        }
    }

    private fun extractServerError(body: String): String {
        if (body.isBlank()) return "sem detalhes"
        return runCatching {
            val errorJson = JSONObject(body)
            errorJson.stringOrNull("message")
                ?: errorJson.stringOrNull("msg")
                ?: errorJson.stringOrNull("error_description")
                ?: errorJson.stringOrNull("error")
                ?: body
        }.getOrElse { body }
    }

    private fun ensureConfigured() {
        if (!isConfigured()) {
            throw IllegalStateException("SUPABASE_URL/SUPABASE_ANON_KEY nao configurados no Android.")
        }
    }

    private fun JSONObject.stringOrNull(key: String): String? {
        if (isNull(key)) return null
        return optString(key).trim().ifEmpty { null }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (isNull(key)) return null
        if (!has(key)) return null
        return runCatching { getInt(key) }.getOrNull()
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
