package com.odontoart.rotas.dashboard

data class DashboardVisitLite(
    val id: String,
    val clienteId: String?,
    val visitDate: String?,
    val completedAt: String?,
    val noVisitReason: String?,
    val assignedToUserId: String?,
    val assignedToName: String?,
    val completedVidas: Int?,
)

data class DashboardAcceptanceLite(
    val entryDate: String?,
    val vendorUserId: String?,
    val vidas: Int?,
)

data class DashboardClientLite(
    val id: String,
    val codigo: String?,
    val empresa: String?,
    val cidade: String?,
    val bairro: String?,
    val situacao: String?,
    val vendedor: String?,
    val categoria: String?,
    val grupo: String?,
)

data class DashboardProfileLite(
    val profileId: String?,
    val userId: String?,
    val displayName: String?,
    val role: String?,
    val supervisorId: String?,
)

data class DashboardDataset(
    val visits: List<DashboardVisitLite>,
    val historicalVisits: List<DashboardVisitLite>,
    val previousVisits: List<DashboardVisitLite>,
    val previousMonthVisits: List<DashboardVisitLite>,
    val acceptances: List<DashboardAcceptanceLite>,
    val previousAcceptances: List<DashboardAcceptanceLite>,
    val clients: List<DashboardClientLite>,
    val profiles: List<DashboardProfileLite>,
    val totalClients: Int,
)

data class DashboardMetricSet(
    val totalVisits: Int = 0,
    val concludedVisits: Int = 0,
    val notPerformedVisits: Int = 0,
    val performedVisits: Int = 0,
    val pendingVisits: Int = 0,
    val livesVisits: Int = 0,
    val livesAcceptance: Int = 0,
    val livesTotal: Int = 0,
    val executionRate: Double = 0.0,
    val visitedCompanies: Int = 0,
    val coverage: Double = 0.0,
    val totalClientsInScope: Int = 0,
    val missingClient: Int = 0,
    val missingResponsible: Int = 0,
    val missingDate: Int = 0,
)

data class DashboardDailyPoint(
    val date: String,
    val visits: Int,
    val concluded: Int,
    val lives: Int,
)

data class DashboardSellerSummary(
    val seller: String,
    val userId: String?,
    val supervisorId: String?,
    val visits: Int,
    val concluded: Int,
    val companies: Int,
    val livesVisit: Int,
    val livesAcceptance: Int,
) {
    val livesTotal: Int get() = livesVisit + livesAcceptance
}

data class DashboardCitySummary(val city: String, val count: Int)
data class DashboardReasonSummary(val reason: String, val count: Int)
data class DashboardCompanySummary(val id: String, val name: String, val count: Int)
data class DashboardSupervisorOption(val id: String, val name: String)
data class DashboardVendorOption(val userId: String, val supervisorId: String?, val name: String)

data class DashboardComputedModel(
    val metrics: DashboardMetricSet = DashboardMetricSet(),
    val previousMetrics: DashboardMetricSet = DashboardMetricSet(),
    val previousMonthMetrics: DashboardMetricSet = DashboardMetricSet(),
    val daily: List<DashboardDailyPoint> = emptyList(),
    val sellers: List<DashboardSellerSummary> = emptyList(),
    val cities: List<DashboardCitySummary> = emptyList(),
    val reasons: List<DashboardReasonSummary> = emptyList(),
    val mostVisitedCompanies: List<DashboardCompanySummary> = emptyList(),
    val leastVisitedCompanies: List<DashboardCompanySummary> = emptyList(),
    val neverVisitedCompanies: List<DashboardCompanySummary> = emptyList(),
    val supervisors: List<DashboardSupervisorOption> = emptyList(),
    val vendors: List<DashboardVendorOption> = emptyList(),
)

data class ExactDashboardUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val errorMessage: String? = null,
    val from: String = "",
    val to: String = "",
    val selectedSupervisorId: String = "all",
    val selectedSellerName: String = "all",
    val model: DashboardComputedModel = DashboardComputedModel(),
)
