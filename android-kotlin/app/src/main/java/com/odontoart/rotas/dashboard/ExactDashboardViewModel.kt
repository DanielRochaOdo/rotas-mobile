package com.odontoart.rotas.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odontoart.rotas.UserRole
import com.odontoart.rotas.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class ExactDashboardViewModel(
    private val repository: ExactDashboardRepository = ExactDashboardRepository(),
) : ViewModel() {
    private val today = LocalDate.now()
    private var dataset: DashboardDataset? = null
    private var lastSession: UserSession? = null
    private var lastRole: UserRole? = null

    private val _uiState = MutableStateFlow(
        ExactDashboardUiState(
            from = today.withDayOfMonth(1).toString(),
            to = today.toString(),
        ),
    )
    val uiState: StateFlow<ExactDashboardUiState> = _uiState.asStateFlow()

    fun load(
        session: UserSession,
        role: UserRole?,
        from: String = _uiState.value.from,
        to: String = _uiState.value.to,
        force: Boolean = false,
    ) {
        if (_uiState.value.isLoading) return
        val sameScope = dataset != null && lastSession?.userId == session.userId && lastRole == role &&
            from == _uiState.value.from && to == _uiState.value.to
        if (sameScope && !force) {
            recompute()
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, from = from, to = to) }
        viewModelScope.launch {
            runCatching { repository.fetchDataset(session, role, from, to) }
                .onSuccess { loaded ->
                    dataset = loaded
                    lastSession = session
                    lastRole = role
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            hasLoaded = true,
                            errorMessage = null,
                            model = repository.compute(
                                dataset = loaded,
                                from = current.from,
                                to = current.to,
                                selectedSupervisorId = current.selectedSupervisorId,
                                selectedSellerName = current.selectedSellerName,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message?.takeIf(String::isNotBlank) ?: "Erro ao carregar Dashboard.",
                        )
                    }
                }
        }
    }

    fun applyFilters(
        session: UserSession,
        role: UserRole?,
        from: String,
        to: String,
        supervisorId: String,
        sellerName: String,
    ) {
        val datesChanged = from != _uiState.value.from || to != _uiState.value.to
        _uiState.update {
            it.copy(
                from = from,
                to = to,
                selectedSupervisorId = supervisorId,
                selectedSellerName = sellerName,
                errorMessage = null,
            )
        }
        if (datesChanged) {
            load(session, role, from, to, force = true)
        } else {
            recompute()
        }
    }

    fun clearFilters(session: UserSession, role: UserRole?) {
        val currentToday = LocalDate.now()
        val from = currentToday.withDayOfMonth(1).toString()
        val to = currentToday.toString()
        _uiState.update {
            it.copy(
                from = from,
                to = to,
                selectedSupervisorId = "all",
                selectedSellerName = "all",
                errorMessage = null,
            )
        }
        load(session, role, from, to, force = true)
    }

    fun refresh(session: UserSession, role: UserRole?) {
        load(session, role, _uiState.value.from, _uiState.value.to, force = true)
    }

    private fun recompute() {
        val loaded = dataset ?: return
        _uiState.update { current ->
            current.copy(
                model = repository.compute(
                    dataset = loaded,
                    from = current.from,
                    to = current.to,
                    selectedSupervisorId = current.selectedSupervisorId,
                    selectedSellerName = current.selectedSellerName,
                ),
            )
        }
    }
}
