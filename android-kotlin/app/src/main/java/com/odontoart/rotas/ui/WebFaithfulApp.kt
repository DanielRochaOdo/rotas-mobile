package com.odontoart.rotas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odontoart.rotas.MainViewModel
import com.odontoart.rotas.ParityViewModel
import com.odontoart.rotas.RoutesWebRepository
import com.odontoart.rotas.UserRole
import com.odontoart.rotas.WebIntegrationApi
import com.odontoart.rotas.ui.theme.RotasTheme
import kotlinx.coroutines.launch

private enum class FaithfulDestination(
    val label: String,
    val icon: ImageVector,
    val roles: Set<UserRole>,
) {
    DASHBOARD("Dashboard", Icons.Rounded.Dashboard, UserRole.entries.toSet()),
    ROUTES("Rotas", Icons.Rounded.Map, setOf(UserRole.SUPERVISOR, UserRole.ASSISTENTE)),
    VISITS("Agenda", Icons.Rounded.CalendarMonth, UserRole.entries.toSet()),
    ACCEPTANCE("Aceite digital", Icons.Rounded.FactCheck, setOf(UserRole.VENDEDOR)),
    CLIENTS("Empresas", Icons.Rounded.Business, setOf(UserRole.SUPERVISOR, UserRole.ASSISTENTE)),
    QUEUE("Fila", Icons.Rounded.HourglassBottom, setOf(UserRole.SUPERVISOR, UserRole.ASSISTENTE)),
    KPI("KPI", Icons.Rounded.QueryStats, setOf(UserRole.SUPERVISOR, UserRole.ASSISTENTE)),
    LOGS("Logs", Icons.Rounded.History, setOf(UserRole.SUPERVISOR)),
    NEWS("Novidades", Icons.Rounded.Campaign, UserRole.entries.toSet()),
    SETTINGS("Configurações", Icons.Rounded.Settings, setOf(UserRole.SUPERVISOR)),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebFaithfulApp(
    mainViewModel: MainViewModel,
    parityViewModel: ParityViewModel = viewModel(),
) {
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val state by parityViewModel.uiState.collectAsStateWithLifecycle()
    val session = mainState.session ?: return
    val profile = mainState.profile
    val role = profile?.userRole
    val available = remember(role) { FaithfulDestination.entries.filter { role != null && role in it.roles } }
    var destinationName by rememberSaveable { mutableStateOf(FaithfulDestination.DASHBOARD.name) }
    var destination = remember(destinationName) { FaithfulDestination.valueOf(destinationName) }
    var darkTheme by rememberSaveable { mutableStateOf(false) }
    val routeActions = remember { RoutesWebRepository() }
    val integrations = remember { WebIntegrationApi() }

    LaunchedEffect(role) {
        if (destination !in available) destinationName = FaithfulDestination.DASHBOARD.name
    }

    LaunchedEffect(destination, session.userId) {
        when (destination) {
            FaithfulDestination.DASHBOARD -> parityViewModel.loadDashboard(session, profile, mainState.routes.size)
            FaithfulDestination.ROUTES -> {
                parityViewModel.loadAgendaCompanies(session)
                mainViewModel.reloadRoutes()
            }
            FaithfulDestination.VISITS -> parityViewModel.loadVisits(session, profile)
            FaithfulDestination.ACCEPTANCE -> parityViewModel.loadAcceptance(session, profile, java.time.LocalDate.now().toString())
            FaithfulDestination.CLIENTS -> parityViewModel.loadClients(session)
            FaithfulDestination.QUEUE -> parityViewModel.loadQueue(session)
            FaithfulDestination.KPI -> parityViewModel.loadKpi(session, 30)
            FaithfulDestination.LOGS -> parityViewModel.loadLogs(session)
            FaithfulDestination.NEWS -> parityViewModel.loadNews(session, profile)
            FaithfulDestination.SETTINGS -> parityViewModel.loadManagedProfiles(session)
        }
    }

    RotasTheme(darkTheme = darkTheme) {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val background = if (darkTheme) {
            Brush.linearGradient(listOf(Color(0xFF111715), Color(0xFF151D19), Color(0xFF101412)))
        } else {
            Brush.linearGradient(listOf(Color(0xFFF6FAF7), Color(0xFFEEF5F1), Color(0xFFF9FCFA)))
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(296.dp)) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(11.dp))
                            Column {
                                Text("Odontoart Rotas", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                Text("Agenda+", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                profile?.resolvedName ?: session.userEmail ?: "Perfil pendente",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(role?.label ?: "Perfil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider()

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            available.forEach { item ->
                                NavigationDrawerItem(
                                    selected = destination == item,
                                    onClick = {
                                        destinationName = item.name
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    label = { Text(item.label, fontWeight = if (destination == item) FontWeight.SemiBold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(12.dp),
                                )
                            }
                        }

                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(if (darkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Tema escuro", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
                        }
                        TextButton(onClick = mainViewModel::signOut, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Logout, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sair")
                        }
                    }
                }
            },
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Rounded.Menu, contentDescription = "Abrir menu")
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("ODONTOART", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text("Agenda+ · ${destination.label}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { destinationName = FaithfulDestination.NEWS.name }) {
                                Icon(Icons.Rounded.Notifications, contentDescription = "Novidades")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
                    )
                },
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().background(background).padding(padding)) {
                    when (destination) {
                        FaithfulDestination.DASHBOARD -> FaithfulDashboardScreen(
                            state = state,
                            onRefresh = { parityViewModel.loadDashboard(session, profile, mainState.routes.size) },
                        )
                        FaithfulDestination.ROUTES -> FaithfulRoutesScreen(
                            state = state,
                            mainState = mainState,
                            onSearch = { query, page -> parityViewModel.loadAgendaCompanies(session, query, page) },
                            onReloadSaved = mainViewModel::reloadRoutes,
                            onSelectSaved = mainViewModel::selectRoute,
                            onDeleteSaved = mainViewModel::deleteRoute,
                            onCreateRoute = { name, date, ids ->
                                scope.launch {
                                    runCatching { routeActions.createRouteWithStops(session, name, date, ids) }
                                        .onSuccess { mainViewModel.reloadRoutes() }
                                }
                            },
                        )
                        FaithfulDestination.VISITS -> FaithfulVisitsScreen(
                            state = state,
                            onRefresh = { parityViewModel.loadVisits(session, profile) },
                            onComplete = { id, vidas -> parityViewModel.completeVisit(session, profile, id, vidas) },
                            onNoVisit = { id, reason, observation -> parityViewModel.registerNoVisit(session, profile, id, reason, observation) },
                        )
                        FaithfulDestination.CLIENTS -> FaithfulCompaniesScreen(
                            state = state,
                            session = session,
                            integrations = integrations,
                            onLoad = { search, mode, situacao, page -> parityViewModel.loadClients(session, search, mode, situacao, page) },
                            onCreate = { payload -> parityViewModel.createClient(session, payload, state.clientSearch) },
                            onUpdate = { id, payload -> parityViewModel.updateClient(session, id, payload, state.clientSearch) },
                            onDelete = { id -> parityViewModel.deleteClient(session, id, state.clientSearch) },
                        )
                        FaithfulDestination.ACCEPTANCE -> FaithfulAcceptanceScreen(
                            state = state,
                            onRegister = { date, vidas -> parityViewModel.registerAcceptance(session, profile, date, vidas, java.time.LocalDate.now().toString()) },
                        )
                        FaithfulDestination.QUEUE -> FaithfulQueueScreen(
                            state = state,
                            onRefresh = { parityViewModel.loadQueue(session) },
                            onAction = { id, action, waiting, block, reason -> parityViewModel.applyQueueAction(session, id, action, waiting, block, reason) },
                        )
                        FaithfulDestination.KPI -> FaithfulKpiScreen(state = state, onPeriod = { parityViewModel.loadKpi(session, it) })
                        FaithfulDestination.LOGS -> FaithfulLogsScreen(state = state, onFilter = { action, table -> parityViewModel.loadLogs(session, action, table) })
                        FaithfulDestination.NEWS -> FaithfulNewsScreen(state = state, onRefresh = { parityViewModel.loadNews(session, profile) }, onRead = { parityViewModel.markNewsRead(session, profile, it) })
                        FaithfulDestination.SETTINGS -> FaithfulSettingsScreen(state = state, onRefresh = { parityViewModel.loadManagedProfiles(session) }, onInactive = { id, inactive -> parityViewModel.setProfileInactive(session, id, inactive) })
                    }

                    if (state.isLoading) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(10.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(999.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Atualizando...", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
