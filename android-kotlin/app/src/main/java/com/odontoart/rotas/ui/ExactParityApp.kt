package com.odontoart.rotas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Surface
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
import com.odontoart.rotas.UserRole
import com.odontoart.rotas.ui.theme.RotasCanvas
import com.odontoart.rotas.ui.theme.RotasSea
import com.odontoart.rotas.ui.theme.RotasTheme
import kotlinx.coroutines.launch

internal enum class ExactDestination(
    val title: String,
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
fun ExactParityApp(
    mainViewModel: MainViewModel,
    parityViewModel: ParityViewModel = viewModel(),
) {
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val parityState by parityViewModel.uiState.collectAsStateWithLifecycle()
    val session = mainState.session ?: return
    val profile = mainState.profile
    val role = profile?.userRole
    val systemDarkTheme = isSystemInDarkTheme()

    var darkTheme by rememberSaveable { mutableStateOf(systemDarkTheme) }
    var destinationName by rememberSaveable { mutableStateOf(ExactDestination.DASHBOARD.name) }
    val destination = remember(destinationName) { ExactDestination.valueOf(destinationName) }
    val available = remember(role) { ExactDestination.entries.filter { item -> role != null && role in item.roles } }

    LaunchedEffect(role) {
        if (role != null && destination !in available) destinationName = ExactDestination.DASHBOARD.name
    }

    LaunchedEffect(destination, session.userId) {
        when (destination) {
            ExactDestination.DASHBOARD -> Unit
            ExactDestination.ROUTES -> {
                parityViewModel.loadAgendaCompanies(session)
                mainViewModel.reloadRoutes()
            }
            ExactDestination.VISITS -> parityViewModel.loadVisits(session, profile)
            ExactDestination.ACCEPTANCE -> parityViewModel.loadAcceptance(session, profile, java.time.LocalDate.now().toString())
            ExactDestination.CLIENTS -> parityViewModel.loadClients(session)
            ExactDestination.QUEUE -> parityViewModel.loadQueue(session)
            ExactDestination.KPI -> parityViewModel.loadKpi(session, 30)
            ExactDestination.LOGS -> parityViewModel.loadLogs(session)
            ExactDestination.NEWS -> parityViewModel.loadNews(session, profile)
            ExactDestination.SETTINGS -> parityViewModel.loadManagedProfiles(session)
        }
    }

    RotasTheme(darkTheme = darkTheme) {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val pageBackground = if (darkTheme) {
            Brush.linearGradient(listOf(Color(0xFF171717), Color(0xFF121212), Color(0xFF0F0F0F)))
        } else {
            Brush.linearGradient(listOf(RotasCanvas, Color(0xFFEEF5F1), Color(0xFFF9FCFA)))
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(304.dp)) {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 14.dp)) {
                        ExactDrawerBrand()
                        Spacer(Modifier.height(14.dp))
                        ExactDrawerProfile(profile?.resolvedName ?: session.userEmail ?: "Perfil pendente", role?.label ?: "Perfil não identificado")
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(available, key = { it.name }) { item ->
                                NavigationDrawerItem(
                                    selected = item == destination,
                                    onClick = { destinationName = item.name; scope.launch { drawerState.close() } },
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    label = { Text(item.title, fontWeight = if (item == destination) FontWeight.SemiBold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(12.dp),
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (darkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Tema escuro", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
                        }
                        TextButton(onClick = { scope.launch { drawerState.close() }; mainViewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
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
                        navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Rounded.Menu, contentDescription = "Abrir menu") } },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = RotasSea.copy(alpha = 0.15f)) {
                                    Icon(Icons.Rounded.Place, contentDescription = null, tint = RotasSea, modifier = Modifier.padding(7.dp).size(17.dp))
                                }
                                Spacer(Modifier.width(9.dp))
                                Column {
                                    Text("ODONTOART", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Text("Agenda+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                }
                            }
                        },
                        actions = { IconButton(onClick = { destinationName = ExactDestination.NEWS.name }) { Icon(Icons.Rounded.Notifications, contentDescription = "Notificações") } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                    )
                },
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().background(pageBackground).padding(padding)) {
                    when (destination) {
                        ExactDestination.DASHBOARD -> StrategicDashboardScreen(session = session, role = role)
                        ExactDestination.ROUTES -> ExactRoutesScreen(
                            state = parityState,
                            mainState = mainState,
                            onSearch = { query, page -> parityViewModel.loadAgendaCompanies(session, query, page) },
                            onReloadSaved = mainViewModel::reloadRoutes,
                            onSelectSaved = mainViewModel::selectRoute,
                            onDeleteSaved = mainViewModel::deleteRoute,
                        )
                        ExactDestination.VISITS -> ExactVisitsScreen(
                            state = parityState,
                            profile = profile,
                            onRefresh = { parityViewModel.loadVisits(session, profile) },
                            onComplete = { id, vidas -> parityViewModel.completeVisit(session, profile, id, vidas) },
                            onNoVisit = { id, reason, observation -> parityViewModel.registerNoVisit(session, profile, id, reason, observation) },
                        )
                        ExactDestination.CLIENTS -> ExactClientsScreen(
                            state = parityState,
                            session = session,
                            onLoad = { search, mode, situacao, page -> parityViewModel.loadClients(session, search, mode, situacao, page) },
                            onCreate = { payload -> parityViewModel.createClient(session, payload, parityState.clientSearch) },
                            onUpdate = { id, payload -> parityViewModel.updateClient(session, id, payload, parityState.clientSearch) },
                            onDelete = { id -> parityViewModel.deleteClient(session, id, parityState.clientSearch) },
                        )
                        ExactDestination.ACCEPTANCE -> AcceptanceParityScreen(
                            state = parityState,
                            role = role,
                            onRefresh = { parityViewModel.loadAcceptance(session, profile, java.time.LocalDate.now().toString()) },
                            onRegister = { date, vidas -> parityViewModel.registerAcceptance(session, profile, date, vidas, java.time.LocalDate.now().toString()) },
                        )
                        ExactDestination.QUEUE -> QueueParityScreen(state = parityState, onRefresh = { parityViewModel.loadQueue(session) }, onAction = { id, action, waiting, block, reason -> parityViewModel.applyQueueAction(session, id, action, waiting, block, reason) })
                        ExactDestination.KPI -> KpiParityScreen(state = parityState, onPeriod = { parityViewModel.loadKpi(session, it) })
                        ExactDestination.LOGS -> LogsParityScreen(state = parityState, onFilter = { action, table -> parityViewModel.loadLogs(session, action, table) })
                        ExactDestination.NEWS -> NewsParityScreen(state = parityState, onRefresh = { parityViewModel.loadNews(session, profile) }, onMarkRead = { parityViewModel.markNewsRead(session, profile, it) })
                        ExactDestination.SETTINGS -> SettingsParityScreen(state = parityState, onRefresh = { parityViewModel.loadManagedProfiles(session) }, onSetInactive = { id, inactive -> parityViewModel.setProfileInactive(session, id, inactive) })
                    }
                    if (destination != ExactDestination.DASHBOARD && parityState.isLoading) {
                        Surface(modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp), shape = RoundedCornerShape(999.dp), shadowElevation = 3.dp) {
                            Text("Atualizando...", modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExactDrawerBrand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = RotasSea.copy(alpha = 0.15f)) {
            Icon(Icons.Rounded.Place, contentDescription = null, tint = RotasSea, modifier = Modifier.padding(10.dp).size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("Odontoart Rotas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExactDrawerProfile(name: String, role: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
        Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
