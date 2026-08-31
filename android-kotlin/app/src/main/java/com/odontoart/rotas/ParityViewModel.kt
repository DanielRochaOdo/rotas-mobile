package com.odontoart.rotas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class ParityViewModel(
    private val api: SupabaseApi = SupabaseApi(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ParityUiState())
    val uiState: StateFlow<ParityUiState> = _uiState.asStateFlow()

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun loadDashboard(session: UserSession, profile: UserProfile?, routeCount: Int) = launchLoad {
        val (visits, clients) = coroutineScope {
            val visitsAsync = async { api.fetchVisits(session, profile?.userRole, 1000) }
            val clientsAsync = async { api.fetchClients(session, limit = 500) }
            visitsAsync.await() to clientsAsync.await()
        }
        val completed = visits.count { it.completedAt != null || it.completedVidas != null || it.noVisitReason != null }
        _uiState.update {
            it.copy(
                visits = visits,
                clients = clients,
                dashboard = DashboardSummary(
                    visits = visits.size,
                    completedVisits = completed,
                    pendingVisits = (visits.size - completed).coerceAtLeast(0),
                    completedLives = visits.sumOf { visit -> visit.completedVidas ?: 0 },
                    companies = clients.size,
                    routes = routeCount,
                ),
            )
        }
    }

    fun loadVisits(session: UserSession, profile: UserProfile?) = launchLoad {
        _uiState.update { it.copy(visits = api.fetchVisits(session, profile?.userRole, 1000)) }
    }

    fun completeVisit(session: UserSession, profile: UserProfile?, visitId: String, vidas: Int) = launchLoad {
        require(vidas >= 0) { "Quantidade de vidas deve ser um numero inteiro valido." }
        api.completeVisit(session, visitId, vidas)
        _uiState.update { it.copy(visits = api.fetchVisits(session, profile?.userRole, 1000)) }
    }

    fun registerNoVisit(
        session: UserSession,
        profile: UserProfile?,
        visitId: String,
        reason: String,
        observation: String?,
    ) = launchLoad {
        require(reason.isNotBlank()) { "Informe o motivo da visita nao realizada." }
        api.registerNoVisit(session, visitId, reason.trim(), observation?.trim())
        _uiState.update { it.copy(visits = api.fetchVisits(session, profile?.userRole, 1000)) }
    }

    fun loadClients(session: UserSession, search: String = "") = launchLoad {
        _uiState.update { it.copy(clients = api.fetchClients(session, search, 300)) }
    }

    fun createClient(session: UserSession, payload: JSONObject, search: String = "") = launchLoad {
        api.createClient(session, payload)
        _uiState.update { it.copy(clients = api.fetchClients(session, search, 300)) }
    }

    fun updateClient(session: UserSession, clientId: String, payload: JSONObject, search: String = "") = launchLoad {
        api.updateClient(session, clientId, payload)
        _uiState.update { it.copy(clients = api.fetchClients(session, search, 300)) }
    }

    fun deleteClient(session: UserSession, clientId: String, search: String = "") = launchLoad {
        api.deleteClient(session, clientId)
        _uiState.update { it.copy(clients = api.fetchClients(session, search, 300)) }
    }

    fun loadAcceptance(session: UserSession, profile: UserProfile?, today: String) = launchLoad {
        when (profile?.userRole) {
            UserRole.VENDEDOR -> {
                val (required, existing) = api.fetchVendorAcceptanceDates(session)
                val registered = existing.map { it.entryDate }.toSet()
                _uiState.update {
                    it.copy(
                        pendingAcceptanceDates = required.filterNot(registered::contains),
                        digitalSummary = existing,
                    )
                }
            }
            UserRole.SUPERVISOR, UserRole.ASSISTENTE -> {
                _uiState.update { it.copy(digitalSummary = api.fetchDigitalSummary(session, today)) }
            }
            null -> Unit
        }
    }

    fun registerAcceptance(session: UserSession, profile: UserProfile?, date: String, vidas: Int, today: String) = launchLoad {
        require(vidas >= 0) { "Quantidade de vidas deve ser um numero inteiro valido." }
        api.registerDigitalAcceptance(session, profile?.displayName ?: profile?.nome, date, vidas)
        when (profile?.userRole) {
            UserRole.VENDEDOR -> {
                val (required, existing) = api.fetchVendorAcceptanceDates(session)
                val registered = existing.map { it.entryDate }.toSet()
                _uiState.update {
                    it.copy(
                        pendingAcceptanceDates = required.filterNot(registered::contains),
                        digitalSummary = existing,
                    )
                }
            }
            else -> _uiState.update { it.copy(digitalSummary = api.fetchDigitalSummary(session, today)) }
        }
    }

    fun loadQueue(session: UserSession) = launchLoad {
        _uiState.update { it.copy(queue = api.fetchQueueControls(session)) }
    }

    fun applyQueueAction(
        session: UserSession,
        empresaId: String,
        action: String,
        waitingDays: Int? = null,
        blockDays: Int? = null,
        reason: String? = null,
    ) = launchLoad {
        api.applyQueueAction(session, empresaId, action, waitingDays, blockDays, reason)
        _uiState.update { it.copy(queue = api.fetchQueueControls(session)) }
    }

    fun loadKpi(session: UserSession, periodDays: Int = 30) = launchLoad {
        _uiState.update { it.copy(kpiSnapshots = api.fetchKpiSnapshots(session, periodDays)) }
    }

    fun loadNews(session: UserSession, profile: UserProfile?) = launchLoad {
        _uiState.update { it.copy(news = api.fetchSystemNews(session, profile?.userRole)) }
    }

    fun markNewsRead(session: UserSession, profile: UserProfile?, updateId: String) = launchLoad {
        api.markSystemNewsRead(session, updateId)
        _uiState.update { it.copy(news = api.fetchSystemNews(session, profile?.userRole)) }
    }

    fun loadLogs(session: UserSession, action: String? = null, table: String? = null) = launchLoad {
        _uiState.update { it.copy(logs = api.fetchAuditLogs(session, action, table)) }
    }

    fun loadManagedProfiles(session: UserSession) = launchLoad {
        _uiState.update { it.copy(managedProfiles = api.fetchManagedProfiles(session)) }
    }

    fun setProfileInactive(session: UserSession, profileId: String, inactive: Boolean) = launchLoad {
        api.setProfileInactive(session, profileId, inactive)
        _uiState.update { it.copy(managedProfiles = api.fetchManagedProfiles(session)) }
    }

    private fun launchLoad(block: suspend () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message?.takeIf(String::isNotBlank)
                                ?: "Falha inesperada. Tente novamente.",
                        )
                    }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
