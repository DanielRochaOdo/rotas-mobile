package com.odontoart.rotas.dashboard

import com.odontoart.rotas.BuildConfig
import com.odontoart.rotas.UserRole
import com.odontoart.rotas.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

class ExactDashboardRepository(
    dashboardUrlRaw: String = BuildConfig.DASHBOARD_URL,
    dashboardAnonKeyRaw: String = BuildConfig.DASHBOARD_ANON_KEY,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val dashboardUrl = dashboardUrlRaw.trim().trimEnd('/')
    private val dashboardAnonKey = dashboardAnonKeyRaw.trim()

    fun isConfigured(): Boolean = dashboardUrl.isNotBlank() && dashboardAnonKey.isNotBlank()

    suspend fun fetchDataset(
        session: UserSession,
        role: UserRole?,
        from: String,
        to: String,
    ): DashboardDataset = withContext(Dispatchers.IO) {
        ensureConfigured()
        val currentStart = parseDate(from)
        val currentEnd = parseDate(to)
        require(!currentStart.isAfter(currentEnd)) { "Período inválido no Dashboard." }
        val spanDays = ChronoUnit.DAYS.between(currentStart, currentEnd).coerceAtLeast(0)
        val previousEnd = currentStart.minusDays(1)
        val previousStart = previousEnd.minusDays(spanDays)
        val previousMonthStart = currentStart.minusMonths(1)
        val previousMonthEnd = currentEnd.minusMonths(1)
        val vendorId = if (role == UserRole.VENDEDOR) session.userId else null

        coroutineScope {
            val currentVisits = async { fetchVisitsRange(currentStart, currentEnd, vendorId) }
            val historicalVisits = async { fetchHistoricalVisits(vendorId) }
            val previousVisits = async { fetchVisitsRange(previousStart, previousEnd, vendorId) }
            val previousMonthVisits = async { fetchVisitsRange(previousMonthStart, previousMonthEnd, vendorId) }
            val currentAcceptances = async { fetchAcceptancesRange(currentStart, currentEnd, vendorId) }
            val previousAcceptances = async { fetchAcceptancesRange(previousStart, previousEnd, vendorId) }
            val clients = async { fetchAllClients() }
            val profiles = async { fetchProfiles() }
            val clientCount = async { fetchExactCount("v_dash_clientes_active") }

            DashboardDataset(
                visits = currentVisits.await(),
                historicalVisits = historicalVisits.await(),
                previousVisits = previousVisits.await(),
                previousMonthVisits = previousMonthVisits.await(),
                acceptances = currentAcceptances.await(),
                previousAcceptances = previousAcceptances.await(),
                clients = clients.await(),
                profiles = profiles.await(),
                totalClients = clientCount.await(),
            )
        }
    }

    fun compute(
        dataset: DashboardDataset,
        from: String,
        to: String,
        selectedSupervisorId: String = "all",
        selectedSellerName: String = "all",
    ): DashboardComputedModel {
        val profileNameByUser = dataset.profiles
            .filter { !it.userId.isNullOrBlank() }
            .associate { it.userId!! to (it.displayName?.takeIf(String::isNotBlank) ?: it.userId) }
        val supervisorNameById = dataset.profiles
            .filter { it.role.equals("SUPERVISOR", true) && !it.profileId.isNullOrBlank() }
            .associate { it.profileId!! to (it.displayName?.takeIf(String::isNotBlank) ?: it.userId ?: it.profileId) }
        val vendorSupervisorByUser = dataset.profiles
            .filter { it.role.equals("VENDEDOR", true) && !it.userId.isNullOrBlank() && !it.supervisorId.isNullOrBlank() }
            .associate { it.userId!! to it.supervisorId!! }

        fun sellerName(userId: String?, assignedName: String?): String =
            assignedName?.takeIf(String::isNotBlank)
                ?: userId?.let { profileNameByUser[it] ?: it }
                ?: "Sem nome"

        fun matches(userId: String?, name: String?): Boolean {
            val supervisorOk = selectedSupervisorId == "all" ||
                (userId != null && vendorSupervisorByUser[userId] == selectedSupervisorId)
            val sellerOk = selectedSellerName == "all" || normalize(sellerName(userId, name)) == normalize(selectedSellerName)
            return supervisorOk && sellerOk
        }

        val visits = dataset.visits.filter { matches(it.assignedToUserId, it.assignedToName) }
        val previousVisits = dataset.previousVisits.filter { matches(it.assignedToUserId, it.assignedToName) }
        val previousMonthVisits = dataset.previousMonthVisits.filter { matches(it.assignedToUserId, it.assignedToName) }
        val acceptances = dataset.acceptances.filter { entry ->
            val userId = entry.vendorUserId
            matches(userId, userId?.let(profileNameByUser::get))
        }
        val previousAcceptances = dataset.previousAcceptances.filter { entry ->
            val userId = entry.vendorUserId
            matches(userId, userId?.let(profileNameByUser::get))
        }
        val clientById = dataset.clients.associateBy { it.id }

        val metrics = buildMetrics(visits, acceptances, dataset.totalClients)
        val previousMetrics = buildMetrics(previousVisits, previousAcceptances, dataset.totalClients)
        val previousMonthMetrics = buildMetrics(previousMonthVisits, emptyList(), dataset.totalClients)

        val byDay = linkedMapOf<String, MutableDaily>()
        visits.forEach { visit ->
            val day = visit.visitDate?.take(10)?.takeIf(String::isNotBlank) ?: return@forEach
            val current = byDay.getOrPut(day) { MutableDaily() }
            current.visits += 1
            if (visit.completedAt != null) current.concluded += 1
            current.lives += visit.completedVidas ?: 0
        }
        acceptances.forEach { acceptance ->
            val day = acceptance.entryDate?.take(10)?.takeIf(String::isNotBlank) ?: return@forEach
            byDay.getOrPut(day) { MutableDaily() }.lives += acceptance.vidas ?: 0
        }
        val daily = buildDateRange(from, to).map { day ->
            val value = byDay[day] ?: MutableDaily()
            DashboardDailyPoint(day, value.visits, value.concluded, value.lives)
        }

        data class SellerMutable(
            var userId: String? = null,
            var supervisorId: String? = null,
            var visits: Int = 0,
            var concluded: Int = 0,
            val companies: MutableSet<String> = linkedSetOf(),
            var livesVisit: Int = 0,
            var livesAcceptance: Int = 0,
        )
        val sellerMap = linkedMapOf<String, SellerMutable>()
        visits.forEach { visit ->
            val label = sellerName(visit.assignedToUserId, visit.assignedToName)
            val row = sellerMap.getOrPut(label) { SellerMutable() }
            row.userId = row.userId ?: visit.assignedToUserId
            row.supervisorId = row.supervisorId ?: visit.assignedToUserId?.let(vendorSupervisorByUser::get)
            row.visits += 1
            if (visit.completedAt != null) row.concluded += 1
            visit.clienteId?.let(row.companies::add)
            row.livesVisit += visit.completedVidas ?: 0
        }
        acceptances.forEach { acceptance ->
            val userId = acceptance.vendorUserId
            val label = sellerName(userId, userId?.let(profileNameByUser::get))
            val row = sellerMap.getOrPut(label) { SellerMutable() }
            row.userId = row.userId ?: userId
            row.supervisorId = row.supervisorId ?: userId?.let(vendorSupervisorByUser::get)
            row.livesAcceptance += acceptance.vidas ?: 0
        }
        val sellers = sellerMap.map { (seller, value) ->
            DashboardSellerSummary(
                seller = seller,
                userId = value.userId,
                supervisorId = value.supervisorId,
                visits = value.visits,
                concluded = value.concluded,
                companies = value.companies.size,
                livesVisit = value.livesVisit,
                livesAcceptance = value.livesAcceptance,
            )
        }.sortedWith(compareByDescending<DashboardSellerSummary> { it.visits }.thenByDescending { it.livesTotal })

        val cities = visits
            .mapNotNull { it.clienteId?.let(clientById::get)?.cidade?.takeIf(String::isNotBlank) ?: "Sem cidade" }
            .groupingBy { it }
            .eachCount()
            .map { DashboardCitySummary(it.key, it.value) }
            .sortedByDescending { it.count }
            .take(8)

        val reasons = visits
            .mapNotNull { it.noVisitReason?.takeIf(String::isNotBlank) }
            .groupingBy { it }
            .eachCount()
            .map { DashboardReasonSummary(it.key, it.value) }
            .sortedByDescending { it.count }

        val countByCompany = visits.mapNotNull { it.clienteId }.groupingBy { it }.eachCount()
        val withVisits = countByCompany.map { (id, count) ->
            val client = clientById[id]
            DashboardCompanySummary(id, listOfNotNull(client?.codigo, client?.empresa).joinToString(" - ").ifBlank { id }, count)
        }
        val mostVisited = withVisits.sortedWith(compareByDescending<DashboardCompanySummary> { it.count }.thenBy { it.name }).take(10)
        val leastVisited = withVisits.sortedWith(compareBy<DashboardCompanySummary> { it.count }.thenBy { it.name }).take(10)
        val visitedIds = countByCompany.keys
        val neverVisited = dataset.clients
            .asSequence()
            .filter { it.id !in visitedIds }
            .map { DashboardCompanySummary(it.id, listOfNotNull(it.codigo, it.empresa).joinToString(" - ").ifBlank { it.id }, 0) }
            .sortedBy { it.name }
            .take(10)
            .toList()

        val supervisors = supervisorNameById.map { DashboardSupervisorOption(it.key, it.value) }.sortedBy { it.name.lowercase(Locale("pt", "BR")) }
        val vendors = dataset.profiles
            .asSequence()
            .filter { it.role.equals("VENDEDOR", true) && !it.userId.isNullOrBlank() }
            .map {
                DashboardVendorOption(
                    userId = it.userId!!,
                    supervisorId = it.supervisorId,
                    name = it.displayName?.takeIf(String::isNotBlank) ?: it.userId,
                )
            }
            .distinctBy { normalize(it.name) }
            .sortedBy { it.name.lowercase(Locale("pt", "BR")) }
            .toList()

        return DashboardComputedModel(
            metrics = metrics,
            previousMetrics = previousMetrics,
            previousMonthMetrics = previousMonthMetrics,
            daily = daily,
            sellers = sellers,
            cities = cities,
            reasons = reasons,
            mostVisitedCompanies = mostVisited,
            leastVisitedCompanies = leastVisited,
            neverVisitedCompanies = neverVisited,
            supervisors = supervisors,
            vendors = vendors,
        )
    }

    private fun buildMetrics(
        visits: List<DashboardVisitLite>,
        acceptances: List<DashboardAcceptanceLite>,
        totalClients: Int,
    ): DashboardMetricSet {
        val concluded = visits.count { it.completedAt != null }
        val notPerformed = visits.count { it.completedAt != null && !it.noVisitReason.isNullOrBlank() }
        val performed = (concluded - notPerformed).coerceAtLeast(0)
        val pending = (visits.size - concluded).coerceAtLeast(0)
        val livesVisits = visits.sumOf { it.completedVidas ?: 0 }
        val livesAcceptance = acceptances.sumOf { it.vidas ?: 0 }
        val companies = visits.mapNotNull { it.clienteId }.toSet().size
        return DashboardMetricSet(
            totalVisits = visits.size,
            concludedVisits = concluded,
            notPerformedVisits = notPerformed,
            performedVisits = performed,
            pendingVisits = pending,
            livesVisits = livesVisits,
            livesAcceptance = livesAcceptance,
            livesTotal = livesVisits + livesAcceptance,
            executionRate = if (visits.isEmpty()) 0.0 else performed * 100.0 / visits.size,
            visitedCompanies = companies,
            coverage = if (totalClients <= 0) 0.0 else companies * 100.0 / totalClients,
            totalClientsInScope = totalClients,
            missingClient = visits.count { it.clienteId.isNullOrBlank() },
            missingResponsible = visits.count { it.assignedToName.isNullOrBlank() && it.assignedToUserId.isNullOrBlank() },
            missingDate = visits.count { it.visitDate.isNullOrBlank() },
        )
    }

    private fun fetchVisitsRange(from: LocalDate, to: LocalDate, vendorUserId: String?): List<DashboardVisitLite> =
        fetchVisits(
            listOf(
                "visit_date" to "gte.${from}",
                "visit_date" to "lt.${to.plusDays(1)}",
            ),
            vendorUserId,
        )

    private fun fetchHistoricalVisits(vendorUserId: String?): List<DashboardVisitLite> =
        fetchVisits(listOf("visit_date" to "not.is.null"), vendorUserId)

    private fun fetchVisits(extraParams: List<Pair<String, String>>, vendorUserId: String?): List<DashboardVisitLite> {
        val output = mutableListOf<DashboardVisitLite>()
        var offset = 0
        val pageSize = 1000
        while (true) {
            val params = mutableListOf(
                "select" to "id,cliente_id,visit_date,completed_at,no_visit_reason,assigned_to_user_id,assigned_to_name,completed_vidas",
                "order" to "visit_date.asc",
                "limit" to pageSize.toString(),
                "offset" to offset.toString(),
            )
            params += extraParams
            if (!vendorUserId.isNullOrBlank()) params += "assigned_to_user_id" to "eq.$vendorUserId"
            val rows = fetchArray("v_dash_visits_active", params)
            repeat(rows.length()) { index ->
                val row = rows.optJSONObject(index) ?: return@repeat
                output += DashboardVisitLite(
                    id = row.string("id") ?: return@repeat,
                    clienteId = row.string("cliente_id"),
                    visitDate = row.string("visit_date"),
                    completedAt = row.string("completed_at"),
                    noVisitReason = row.string("no_visit_reason"),
                    assignedToUserId = row.string("assigned_to_user_id"),
                    assignedToName = row.string("assigned_to_name"),
                    completedVidas = row.intOrNull("completed_vidas"),
                )
            }
            if (rows.length() < pageSize) break
            offset += pageSize
        }
        return output
    }

    private fun fetchAcceptancesRange(from: LocalDate, to: LocalDate, vendorUserId: String?): List<DashboardAcceptanceLite> {
        val params = mutableListOf(
            "select" to "entry_date,vendor_user_id,vidas",
            "entry_date" to "gte.$from",
            "entry_date" to "lt.${to.plusDays(1)}",
        )
        if (!vendorUserId.isNullOrBlank()) params += "vendor_user_id" to "eq.$vendorUserId"
        val rows = fetchArray("v_dash_aceite_digital_active", params)
        return buildList {
            repeat(rows.length()) { index ->
                val row = rows.optJSONObject(index) ?: return@repeat
                add(DashboardAcceptanceLite(row.string("entry_date"), row.string("vendor_user_id"), row.intOrNull("vidas")))
            }
        }
    }

    private fun fetchAllClients(): List<DashboardClientLite> {
        val output = mutableListOf<DashboardClientLite>()
        var offset = 0
        val pageSize = 300
        while (true) {
            val rows = fetchArray(
                "v_dash_clientes_active",
                listOf(
                    "select" to "id,codigo,empresa,cidade,bairro,situacao,vendedor,categoria,grupo",
                    "order" to "empresa.asc",
                    "limit" to pageSize.toString(),
                    "offset" to offset.toString(),
                ),
            )
            repeat(rows.length()) { index ->
                val row = rows.optJSONObject(index) ?: return@repeat
                output += DashboardClientLite(
                    id = row.string("id") ?: return@repeat,
                    codigo = row.string("codigo"),
                    empresa = row.string("empresa"),
                    cidade = row.string("cidade"),
                    bairro = row.string("bairro"),
                    situacao = row.string("situacao"),
                    vendedor = row.string("vendedor"),
                    categoria = row.string("categoria"),
                    grupo = row.string("grupo"),
                )
            }
            if (rows.length() < pageSize) break
            offset += pageSize
        }
        return output
    }

    private fun fetchProfiles(): List<DashboardProfileLite> {
        val rows = fetchArray(
            "v_dash_profiles_active",
            listOf(
                "select" to "id,user_id,display_name,role,supervisor_id",
                "role" to "in.(VENDEDOR,SUPERVISOR)",
            ),
        )
        return buildList {
            repeat(rows.length()) { index ->
                val row = rows.optJSONObject(index) ?: return@repeat
                add(
                    DashboardProfileLite(
                        profileId = row.string("id"),
                        userId = row.string("user_id"),
                        displayName = row.string("display_name"),
                        role = row.string("role"),
                        supervisorId = row.string("supervisor_id"),
                    ),
                )
            }
        }
    }

    private fun fetchExactCount(view: String): Int {
        val url = "$dashboardUrl/rest/v1/$view".toHttpUrl().newBuilder()
            .addQueryParameter("select", "id")
            .addQueryParameter("limit", "1")
            .build()
        val request = baseRequest(url.toString())
            .header("Prefer", "count=exact")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Falha ao contar empresas do Dashboard (${response.code}): ${body.take(240)}")
            return response.header("Content-Range")?.substringAfterLast('/')?.toIntOrNull()
                ?: runCatching { JSONArray(body).length() }.getOrDefault(0)
        }
    }

    private fun fetchArray(view: String, params: List<Pair<String, String>>): JSONArray {
        val builder = "$dashboardUrl/rest/v1/$view".toHttpUrl().newBuilder()
        params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        val request = baseRequest(builder.build().toString()).get().build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Falha ao carregar Dashboard (${response.code}): ${body.take(300)}")
            return JSONArray(body)
        }
    }

    private fun baseRequest(url: String): Request.Builder = Request.Builder()
        .url(url)
        .header("apikey", dashboardAnonKey)
        .header("Authorization", "Bearer $dashboardAnonKey")

    private fun ensureConfigured() {
        if (!isConfigured()) error("Configuração do Supabase de Dashboard não encontrada no APK.")
    }

    private fun parseDate(value: String): LocalDate = runCatching { LocalDate.parse(value) }
        .getOrElse { error("Data inválida no Dashboard: $value") }

    private fun buildDateRange(from: String, to: String): List<String> {
        val start = parseDate(from)
        val end = parseDate(to)
        if (start.isAfter(end)) return emptyList()
        return buildList {
            var cursor = start
            while (!cursor.isAfter(end)) {
                add(cursor.toString())
                cursor = cursor.plusDays(1)
            }
        }
    }

    private fun normalize(value: String?): String = value.orEmpty()
        .uppercase(Locale("pt", "BR"))
        .replace(Regex("\\s+"), " ")
        .trim()

    private data class MutableDaily(var visits: Int = 0, var concluded: Int = 0, var lives: Int = 0)
}

private fun JSONObject.string(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.intOrNull(key: String): Int? =
    if (isNull(key)) null else runCatching { getInt(key) }.getOrNull() ?: optString(key).toIntOrNull()
