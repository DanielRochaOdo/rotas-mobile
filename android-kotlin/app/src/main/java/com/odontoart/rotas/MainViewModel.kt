package com.odontoart.rotas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel(
    private val api: SupabaseApi = SupabaseApi(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState(isConfigured = api.isConfigured()))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signIn(email: String, password: String) {
        if (!api.isConfigured()) {
            _uiState.update {
                it.copy(
                    isConfigured = false,
                    errorMessage = "Configure SUPABASE_URL e SUPABASE_ANON_KEY no modulo Android.",
                )
            }
            return
        }

        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Informe email e senha.") }
            return
        }

        _uiState.update {
            it.copy(
                isAuthenticating = true,
                isLoadingRoutes = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            runCatching {
                val session = api.signIn(normalizedEmail, password)
                val profile = api.fetchProfile(session)
                val routes = api.fetchRoutes(session)
                Triple(session, profile, routes)
            }.onSuccess { (session, profile, routes) ->
                val selectedRouteId = routes.firstOrNull()?.id
                _uiState.update {
                    it.copy(
                        isConfigured = true,
                        isAuthenticating = false,
                        isLoadingRoutes = false,
                        errorMessage = null,
                        session = session,
                        profile = profile,
                        routes = routes,
                        selectedRouteId = selectedRouteId,
                        stops = emptyList(),
                    )
                }
                if (selectedRouteId != null) {
                    loadStops(session, selectedRouteId)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        isLoadingRoutes = false,
                        errorMessage = formatError(error),
                        session = null,
                        profile = null,
                        routes = emptyList(),
                        selectedRouteId = null,
                        stops = emptyList(),
                    )
                }
            }
        }
    }

    fun signOut() {
        _uiState.value = MainUiState(isConfigured = api.isConfigured())
    }

    fun reloadRoutes() {
        val session = _uiState.value.session ?: return
        _uiState.update {
            it.copy(
                isLoadingRoutes = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            runCatching {
                api.fetchRoutes(session)
            }.onSuccess { routes ->
                val previousSelection = _uiState.value.selectedRouteId
                val nextSelectedRoute = when {
                    previousSelection != null && routes.any { it.id == previousSelection } -> previousSelection
                    else -> routes.firstOrNull()?.id
                }

                _uiState.update {
                    it.copy(
                        isLoadingRoutes = false,
                        routes = routes,
                        selectedRouteId = nextSelectedRoute,
                        stops = if (nextSelectedRoute == null) emptyList() else it.stops,
                    )
                }

                if (nextSelectedRoute == null) {
                    _uiState.update { it.copy(stops = emptyList(), isLoadingStops = false) }
                } else {
                    loadStops(session, nextSelectedRoute)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingRoutes = false,
                        errorMessage = formatError(error),
                    )
                }
            }
        }
    }

    fun selectRoute(routeId: String) {
        if (_uiState.value.selectedRouteId == routeId) return
        val session = _uiState.value.session ?: return
        _uiState.update {
            it.copy(
                selectedRouteId = routeId,
                stops = emptyList(),
                errorMessage = null,
            )
        }

        loadStops(session, routeId)
    }

    fun createRoute(name: String, date: String?) {
        val state = _uiState.value
        val session = state.session ?: return
        if (!state.canEditRoutes) return

        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nome da rota e obrigatorio.") }
            return
        }

        val normalizedDate = date?.trim()?.takeIf { it.isNotBlank() }

        _uiState.update {
            it.copy(
                isSavingRoute = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            runCatching {
                api.createRoute(session, normalizedName, normalizedDate)
            }.onSuccess { createdRoute ->
                _uiState.update {
                    it.copy(
                        isSavingRoute = false,
                        routes = listOf(createdRoute) + it.routes,
                        selectedRouteId = createdRoute.id,
                        stops = emptyList(),
                    )
                }
                loadStops(session, createdRoute.id)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSavingRoute = false,
                        errorMessage = formatError(error),
                    )
                }
            }
        }
    }

    fun deleteRoute(routeId: String) {
        val state = _uiState.value
        val session = state.session ?: return
        if (!state.canEditRoutes) return

        _uiState.update {
            it.copy(
                isDeletingRouteId = routeId,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            runCatching {
                api.deleteRoute(session, routeId)
            }.onSuccess {
                val currentState = _uiState.value
                val remainingRoutes = currentState.routes.filterNot { it.id == routeId }
                val nextSelectedRoute = when {
                    currentState.selectedRouteId != routeId -> currentState.selectedRouteId
                    else -> remainingRoutes.firstOrNull()?.id
                }

                _uiState.update {
                    it.copy(
                        routes = remainingRoutes,
                        selectedRouteId = nextSelectedRoute,
                        stops = if (nextSelectedRoute == null) emptyList() else it.stops,
                        isDeletingRouteId = null,
                    )
                }

                if (nextSelectedRoute == null) {
                    _uiState.update { it.copy(stops = emptyList(), isLoadingStops = false) }
                } else {
                    loadStops(session, nextSelectedRoute)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeletingRouteId = null,
                        errorMessage = formatError(error),
                    )
                }
            }
        }
    }

    private fun loadStops(session: UserSession, routeId: String) {
        viewModelScope.launch {
            loadStopsInternal(session, routeId)
        }
    }

    private suspend fun loadStopsInternal(session: UserSession, routeId: String) {
        _uiState.update {
            it.copy(
                isLoadingStops = true,
                errorMessage = null,
            )
        }

        runCatching {
            api.fetchRouteStops(session, routeId)
        }.onSuccess { stops ->
            _uiState.update { state ->
                if (state.selectedRouteId != routeId) {
                    state
                } else {
                    state.copy(
                        isLoadingStops = false,
                        stops = stops,
                    )
                }
            }
        }.onFailure { error ->
            _uiState.update { state ->
                if (state.selectedRouteId != routeId) {
                    state
                } else {
                    state.copy(
                        isLoadingStops = false,
                        errorMessage = formatError(error),
                        stops = emptyList(),
                    )
                }
            }
        }
    }

    private fun formatError(error: Throwable): String {
        val message = error.message?.trim()
        return if (message.isNullOrBlank()) {
            "Falha inesperada. Tente novamente."
        } else {
            message
        }
    }
}
