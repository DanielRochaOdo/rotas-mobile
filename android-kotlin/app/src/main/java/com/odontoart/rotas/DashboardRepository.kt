package com.odontoart.rotas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.time.LocalDate

class DashboardRepository(
    dashboardUrlRaw: String = BuildConfig.DASHBOARD_URL,
    dashboardAnonKeyRaw: String = BuildConfig.DASHBOARD_ANON_KEY,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val dashboardUrl = dashboardUrlRaw.trim().trimEnd('/')
    private val dashboardAnonKey = dashboardAnonKeyRaw.trim()

    suspend fun fetchCurrentMonthSummary(
        session: UserSession,
        role: UserRole?,
        routeCount: Int,
    ): DashboardSummary = withContext(Dispatchers.IO) {
        if (dashboardUrl.isBlank() || dashboardAnonKey.isBlank()) {
            throw IllegalStateException("Configuração do dashboard não encontrada no APK.")
        }

        val today = LocalDate.now()
        val from = today.withDayOfMonth(1).toString()
        val toExclusive = today.plusDays(1).toString()
        val visits = fetchVisits(from, toExclusive, if (role == UserRole.VENDEDOR) session.userId else null)
        val completed = visits.count { it.completedAt != null }
        val clients = fetchExactCount("v_dash_clientes_active")

        DashboardSummary(
            visits = visits.size,
            completedVisits = completed,
            pendingVisits = (visits.size - completed).coerceAtLeast(0),
            completedLives = visits.sumOf { it.completedLives ?: 0 },
            companies = clients,
            routes = routeCount,
        )
    }

    private fun fetchVisits(from: String, toExclusive: String, vendorUserId: String?): List<DashboardVisit> {
        val result = mutableListOf<DashboardVisit>()
        var offset = 0
        val pageSize = 1000

        while (true) {
            val query = linkedMapOf(
                "select" to "id,completed_at,completed_vidas",
                "visit_date" to "gte.$from",
                "visit_date" to "lt.$toExclusive",
                "limit" to pageSize.toString(),
                "offset" to offset.toString(),
            )
            val builder = "$dashboardUrl/rest/v1/v_dash_visits_active".toHttpUrl().newBuilder()
                .addQueryParameter("select", "id,completed_at,completed_vidas")
                .addQueryParameter("visit_date", "gte.$from")
                .addQueryParameter("visit_date", "lt.$toExclusive")
                .addQueryParameter("limit", pageSize.toString())
                .addQueryParameter("offset", offset.toString())
            if (!vendorUserId.isNullOrBlank()) {
                builder.addQueryParameter("assigned_to_user_id", "eq.$vendorUserId")
            }

            val rows = JSONArray(execute(builder.build().toString()))
            repeat(rows.length()) { index ->
                val row = rows.optJSONObject(index) ?: return@repeat
                result += DashboardVisit(
                    id = row.optString("id"),
                    completedAt = row.optString("completed_at").takeIf { it.isNotBlank() && it != "null" },
                    completedLives = if (row.isNull("completed_vidas")) null else row.optInt("completed_vidas"),
                )
            }
            if (rows.length() < pageSize) break
            offset += pageSize
        }
        return result
    }

    private fun fetchExactCount(view: String): Int {
        val url = "$dashboardUrl/rest/v1/$view".toHttpUrl().newBuilder()
            .addQueryParameter("select", "id")
            .addQueryParameter("limit", "1")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("apikey", dashboardAnonKey)
            .header("Prefer", "count=exact")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Falha ao carregar dashboard (${response.code}): ${body.take(240)}")
            }
            val total = response.header("Content-Range")
                ?.substringAfterLast('/')
                ?.toIntOrNull()
            return total ?: runCatching { JSONArray(body).length() }.getOrDefault(0)
        }
    }

    private fun execute(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("apikey", dashboardAnonKey)
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Falha ao carregar dashboard (${response.code}): ${body.take(240)}")
            }
            return body
        }
    }

    private data class DashboardVisit(
        val id: String,
        val completedAt: String?,
        val completedLives: Int?,
    )
}
