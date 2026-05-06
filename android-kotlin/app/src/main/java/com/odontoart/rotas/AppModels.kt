package com.odontoart.rotas

data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val userEmail: String?,
)

data class UserProfile(
    val userId: String,
    val displayName: String?,
    val role: String?,
)

data class RouteItem(
    val id: String,
    val name: String,
    val date: String?,
    val assignedToUserId: String?,
    val createdBy: String?,
    val createdAt: String?,
)

data class ClienteInfo(
    val id: String,
    val codigo: String?,
    val empresa: String?,
    val nomeFantasia: String?,
    val endereco: String?,
    val complemento: String?,
    val bairro: String?,
    val cidade: String?,
    val uf: String?,
)

data class RouteStopItem(
    val id: String,
    val routeId: String?,
    val clienteId: String?,
    val stopOrder: Int?,
    val notes: String?,
    val cliente: ClienteInfo?,
)

data class MainUiState(
    val isConfigured: Boolean = true,
    val isAuthenticating: Boolean = false,
    val isLoadingRoutes: Boolean = false,
    val isLoadingStops: Boolean = false,
    val isSavingRoute: Boolean = false,
    val isDeletingRouteId: String? = null,
    val errorMessage: String? = null,
    val session: UserSession? = null,
    val profile: UserProfile? = null,
    val routes: List<RouteItem> = emptyList(),
    val selectedRouteId: String? = null,
    val stops: List<RouteStopItem> = emptyList(),
) {
    val canEditRoutes: Boolean
        get() = profile?.role == "SUPERVISOR" || profile?.role == "ASSISTENTE"
}
