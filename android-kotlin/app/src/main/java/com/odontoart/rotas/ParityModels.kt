package com.odontoart.rotas

enum class UserRole(val label: String) {
    VENDEDOR("Vendedor"),
    SUPERVISOR("Supervisor"),
    ASSISTENTE("Assistente");

    companion object {
        fun from(value: String?): UserRole? = entries.firstOrNull { it.name == value?.trim()?.uppercase() }
    }
}

data class VisitItem(
    val id: String,
    val clienteId: String?,
    val visitDate: String,
    val assignedToUserId: String?,
    val assignedToName: String?,
    val visitType: String?,
    val supervisorReason: String?,
    val registerMode: String?,
    val visitTime: String?,
    val perfilVisita: String?,
    val perfilVisitaOpcoes: String?,
    val routeId: String?,
    val routeStopId: String?,
    val routeStopOrder: Int?,
    val completedAt: String?,
    val completedVidas: Int?,
    val noVisitReason: String?,
    val noVisitObservation: String?,
    val instructions: String?,
    val cliente: ClienteInfo?,
)

data class ClienteListItem(
    val id: String,
    val codigo: String?,
    val cnpj: String?,
    val empresa: String?,
    val nomeFantasia: String?,
    val vidasQtde: Int?,
    val pessoa: String?,
    val contato: String?,
    val grupo: String?,
    val situacao: String?,
    val categoria: String?,
    val perfilVisita: String?,
    val regraVisitaObservacao: String?,
    val endereco: String?,
    val complemento: String?,
    val bairro: String?,
    val cidade: String?,
    val uf: String?,
    val cep: String? = null,
    val corte: Double? = null,
    val venc: Double? = null,
    val valor: Double? = null,
    val reajustePct: Double? = null,
    val competencia: String? = null,
    val dataUltimaVisita: String? = null,
    val obsComercial: String? = null,
    val obs: String? = null,
)

data class DigitalAcceptanceItem(
    val id: String,
    val vendorUserId: String?,
    val vendorName: String?,
    val entryDate: String,
    val vidas: Int,
)

data class QueueControlItem(
    val empresaId: String,
    val codigo: String?,
    val empresa: String?,
    val cnpj: String?,
    val dataContrato: String?,
    val waitingDaysSnapshot: Int?,
    val eligibleAt: String?,
    val state: String,
    val effectiveState: String,
    val manualBlockUntil: String?,
    val manualReason: String?,
    val daysRemaining: Int?,
    val updatedAt: String?,
)

data class KpiSnapshotItem(
    val id: String,
    val codigo: String,
    val empresa: String?,
    val categoria: String?,
    val vidasQtde: Int?,
    val status: String?,
    val snapshotAt: String?,
    val delta: Int,
    val vendasQtde: Int,
    val cancelamentosQtde: Int,
)

data class SystemNewsItem(
    val id: String,
    val titulo: String,
    val descricao: String,
    val tipo: String,
    val modulo: String,
    val rolesPermitidos: List<String>,
    val dataPublicacao: String,
    val ativo: Boolean,
)

data class AuditLogItem(
    val id: String,
    val tableName: String,
    val action: String,
    val recordId: String?,
    val userId: String?,
    val userName: String?,
    val oldData: String?,
    val newData: String?,
    val createdAt: String,
)

data class ManagedProfileItem(
    val id: String,
    val userId: String?,
    val role: String,
    val displayName: String?,
    val nome: String?,
    val canAccessPreCadastro: Boolean,
    val canAccessNextRouteDashboard: Boolean,
    val supervisorId: String?,
    val vendedorId: String?,
    val isInactive: Boolean,
)

data class DashboardSummary(
    val visits: Int = 0,
    val completedVisits: Int = 0,
    val pendingVisits: Int = 0,
    val completedLives: Int = 0,
    val companies: Int = 0,
    val routes: Int = 0,
)

data class ParityUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val visits: List<VisitItem> = emptyList(),
    val clients: List<ClienteListItem> = emptyList(),
    val clientsTotal: Long = 0,
    val clientsPage: Int = 1,
    val clientsPageSize: Int = 50,
    val clientSearch: String = "",
    val clientSearchMode: String = "geral",
    val clientSituacao: String? = null,
    val agendaCompanies: List<AgendaCompanyItem> = emptyList(),
    val agendaTotal: Long = 0,
    val agendaPage: Int = 1,
    val agendaPageSize: Int = 25,
    val agendaSearch: String = "",
    val pendingAcceptanceDates: List<String> = emptyList(),
    val digitalSummary: List<DigitalAcceptanceItem> = emptyList(),
    val queue: List<QueueControlItem> = emptyList(),
    val kpiSnapshots: List<KpiSnapshotItem> = emptyList(),
    val news: List<SystemNewsItem> = emptyList(),
    val logs: List<AuditLogItem> = emptyList(),
    val managedProfiles: List<ManagedProfileItem> = emptyList(),
    val dashboard: DashboardSummary = DashboardSummary(),
)
