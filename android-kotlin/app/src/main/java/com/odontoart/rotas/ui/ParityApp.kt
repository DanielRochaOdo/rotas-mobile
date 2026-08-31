package com.odontoart.rotas.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odontoart.rotas.MainUiState
import com.odontoart.rotas.MainViewModel
import com.odontoart.rotas.ParityUiState
import com.odontoart.rotas.ParityViewModel
import com.odontoart.rotas.RouteItem
import com.odontoart.rotas.RouteStopItem
import com.odontoart.rotas.UserRole
import com.odontoart.rotas.ui.theme.RotasTheme
import kotlinx.coroutines.launch

internal enum class AppDestination(
    val title: String,
    val menuLabel: String,
    val icon: ImageVector,
    val roles: Set<UserRole>,
) {
    DASHBOARD("Dashboard", "Dashboard", Icons.Rounded.Dashboard, UserRole.entries.toSet()),
    ROUTES("Rotas", "Rotas", Icons.Rounded.Map, setOf(UserRole.SUPERVISOR, UserRole.ASSISTENTE)),
    VISITS("Agenda", "Agenda", Icons.Rounded.CalendarMonth, UserRole.entries.toSet()),
    ACCEPTANCE("Aceite digital", "Aceite digital", Icons.Rounded.FactCheck, setOf(UserRole.VENDEDOR)),
    CLIENTS("Empresas", "Empresas", Icons.Rounded.Business, setOf(UserRole.SUPERVISOR, UserRole.ASSISTENTE)),
    QUEUE("Fila", "Fila", Icons.Rounded.HourglassBottom, setOf(UserRole.SUPERVISOR, UserRole.ASSISTENTE)),
    KPI("KPI", "KPI", Icons.Rounded.QueryStats, setOf(UserRole.SUPERVISOR, UserRole.ASSISTENTE)),
    NEWS("Novidades", "Novidades", Icons.Rounded.Campaign, UserRole.entries.toSet()),
    SETTINGS("Configurações", "Configurações", Icons.Rounded.Settings, setOf(UserRole.SUPERVISOR)),
    LOGS("Logs", "Logs", Icons.Rounded.History, setOf(UserRole.SUPERVISOR)),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeParityApp(
    mainViewModel: MainViewModel,
    parityViewModel: ParityViewModel = viewModel(),
) {
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val parityState by parityViewModel.uiState.collectAsStateWithLifecycle()
    val session = mainState.session ?: return
    val profile = mainState.profile
    val role = profile?.userRole
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    var darkTheme by rememberSaveable { mutableStateOf(systemDark) }
    var destinationName by rememberSaveable { mutableStateOf(AppDestination.DASHBOARD.name) }
    var destination = remember(destinationName) { AppDestination.valueOf(destinationName) }
    val availableDestinations = remember(role) {
        AppDestination.entries.filter { item -> role != null && role in item.roles }
    }

    LaunchedEffect(role) {
        if (role != null && destination !in availableDestinations) {
            destinationName = AppDestination.DASHBOARD.name
        }
    }

    LaunchedEffect(destination, session.userId) {
        when (destination) {
            AppDestination.DASHBOARD -> parityViewModel.loadDashboard(session, profile, mainState.routes.size)
            AppDestination.ROUTES -> if (mainState.routes.isEmpty()) mainViewModel.reloadRoutes()
            AppDestination.VISITS -> parityViewModel.loadVisits(session, profile)
            AppDestination.ACCEPTANCE -> parityViewModel.loadAcceptance(session, profile, java.time.LocalDate.now().toString())
            AppDestination.CLIENTS -> parityViewModel.loadClients(session)
            AppDestination.QUEUE -> parityViewModel.loadQueue(session)
            AppDestination.KPI -> parityViewModel.loadKpi(session, 30)
            AppDestination.NEWS -> parityViewModel.loadNews(session, profile)
            AppDestination.SETTINGS -> parityViewModel.loadManagedProfiles(session)
            AppDestination.LOGS -> parityViewModel.loadLogs(session)
        }
    }

    RotasTheme(darkTheme = darkTheme) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(304.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 18.dp),
                    ) {
                        DrawerBrand()
                        Spacer(Modifier.height(18.dp))
                        DrawerProfile(mainState)
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(availableDestinations, key = { it.name }) { item ->
                                NavigationDrawerItem(
                                    selected = destination == item,
                                    onClick = {
                                        destinationName = item.name
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    label = { Text(item.menuLabel) },
                                    shape = RoundedCornerShape(12.dp),
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (darkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Tema escuro", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
                        }
                        TextButton(
                            onClick = {
                                scope.launch { drawerState.close() }
                                mainViewModel.signOut()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Logout, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sair")
                        }
                    }
                }
            },
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Rounded.Menu, contentDescription = "Abrir menu")
                            }
                        },
                        title = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("ODONTOART", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Agenda+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(destination.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        actions = {
                            IconButton(onClick = { destinationName = AppDestination.NEWS.name }) {
                                Icon(Icons.Rounded.Notifications, contentDescription = "Novidades")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    )
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    when (destination) {
                        AppDestination.DASHBOARD -> DashboardParityScreen(
                            state = parityState,
                            onRefresh = { parityViewModel.loadDashboard(session, profile, mainState.routes.size) },
                        )
                        AppDestination.ROUTES -> RoutesParityScreen(
                            state = mainState,
                            onRefresh = mainViewModel::reloadRoutes,
                            onSelect = mainViewModel::selectRoute,
                            onCreate = mainViewModel::createRoute,
                            onDelete = mainViewModel::deleteRoute,
                        )
                        AppDestination.VISITS -> VisitsParityScreen(
                            state = parityState,
                            profile = profile,
                            onRefresh = { parityViewModel.loadVisits(session, profile) },
                            onComplete = { id, vidas -> parityViewModel.completeVisit(session, profile, id, vidas) },
                            onNoVisit = { id, reason, observation ->
                                parityViewModel.registerNoVisit(session, profile, id, reason, observation)
                            },
                        )
                        AppDestination.ACCEPTANCE -> AcceptanceParityScreen(
                            state = parityState,
                            role = role,
                            onRefresh = { parityViewModel.loadAcceptance(session, profile, java.time.LocalDate.now().toString()) },
                            onRegister = { date, vidas ->
                                parityViewModel.registerAcceptance(
                                    session,
                                    profile,
                                    date,
                                    vidas,
                                    java.time.LocalDate.now().toString(),
                                )
                            },
                        )
                        AppDestination.CLIENTS -> ClientsParityScreen(
                            state = parityState,
                            onSearch = { parityViewModel.loadClients(session, it) },
                            onCreate = { payload, search -> parityViewModel.createClient(session, payload, search) },
                            onUpdate = { id, payload, search -> parityViewModel.updateClient(session, id, payload, search) },
                            onDelete = { id, search -> parityViewModel.deleteClient(session, id, search) },
                        )
                        AppDestination.QUEUE -> QueueParityScreen(
                            state = parityState,
                            onRefresh = { parityViewModel.loadQueue(session) },
                            onAction = { id, action, waiting, block, reason ->
                                parityViewModel.applyQueueAction(session, id, action, waiting, block, reason)
                            },
                        )
                        AppDestination.KPI -> KpiParityScreen(
                            state = parityState,
                            onPeriod = { parityViewModel.loadKpi(session, it) },
                        )
                        AppDestination.NEWS -> NewsParityScreen(
                            state = parityState,
                            onRefresh = { parityViewModel.loadNews(session, profile) },
                            onMarkRead = { parityViewModel.markNewsRead(session, profile, it) },
                        )
                        AppDestination.SETTINGS -> SettingsParityScreen(
                            state = parityState,
                            onRefresh = { parityViewModel.loadManagedProfiles(session) },
                            onSetInactive = { id, inactive -> parityViewModel.setProfileInactive(session, id, inactive) },
                        )
                        AppDestination.LOGS -> LogsParityScreen(
                            state = parityState,
                            onFilter = { action, table -> parityViewModel.loadLogs(session, action, table) },
                        )
                    }

                    if (parityState.isLoading) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 3.dp,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
}

@Composable
private fun DrawerBrand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                Icons.Rounded.Place,
                contentDescription = null,
                modifier = Modifier.padding(9.dp).size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("ODONTOART", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("Agenda+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DrawerProfile(state: MainUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                state.profile?.resolvedName ?: "Perfil pendente",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                state.profile?.userRole?.label ?: "Perfil não identificado",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DashboardParityScreen(state: ParityUiState, onRefresh: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ParitySectionHeader(
                "Dashboard",
                "Visão operacional do sistema de rotas.",
                trailing = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar") } },
            )
        }
        state.errorMessage?.let { item { ParityErrorCard(it) } }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardMetric("Visitas", state.dashboard.visits.toString(), Modifier.weight(1f))
                DashboardMetric("Concluídas", state.dashboard.completedVisits.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardMetric("Pendentes", state.dashboard.pendingVisits.toString(), Modifier.weight(1f))
                DashboardMetric("Vidas", state.dashboard.completedLives.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardMetric("Empresas", state.dashboard.companies.toString(), Modifier.weight(1f))
                DashboardMetric("Rotas", state.dashboard.routes.toString(), Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resumo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Os indicadores são calculados a partir das mesmas visitas e empresas do Supabase utilizadas no sistema web.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RoutesParityScreen(
    state: MainUiState,
    onRefresh: () -> Unit,
    onSelect: (String) -> Unit,
    onCreate: (String, String?) -> Unit,
    onDelete: (String) -> Unit,
) {
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var deleteRoute by remember { mutableStateOf<RouteItem?>(null) }
    val selectedRoute = state.routes.firstOrNull { it.id == state.selectedRouteId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ParitySectionHeader(
                "Rotas",
                "Planejamento e sequência de visitas.",
                trailing = {
                    Row {
                        IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar") }
                        if (state.canEditRoutes) {
                            IconButton(onClick = { createOpen = true }) { Icon(Icons.Rounded.Add, contentDescription = "Criar rota") }
                        }
                    }
                },
            )
        }
        state.errorMessage?.let { item { ParityErrorCard(it) } }
        if (state.routes.isEmpty() && !state.isLoadingRoutes) {
            item { ParityInfoCard("Nenhuma rota encontrada.") }
        }
        items(state.routes, key = { it.id }) { route ->
            Card(
                onClick = { onSelect(route.id) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (route.id == state.selectedRouteId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                ),
            ) {
                ListItem(
                    headlineContent = { Text(route.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(formatDateBr(route.date)) },
                    leadingContent = { Icon(Icons.Rounded.Route, contentDescription = null) },
                    trailingContent = {
                        if (state.canEditRoutes) {
                            IconButton(onClick = { deleteRoute = route }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Excluir rota")
                            }
                        }
                    },
                )
            }
        }
        if (selectedRoute != null) {
            item {
                Text("Paradas · ${selectedRoute.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (state.stops.isEmpty() && !state.isLoadingStops) {
                item { ParityInfoCard("Esta rota ainda não possui paradas.") }
            }
            items(state.stops, key = { it.id }) { stop -> RouteStopCard(stop) }
        }
    }

    if (createOpen) {
        CreateRouteParityDialog(
            saving = state.isSavingRoute,
            onDismiss = { createOpen = false },
            onConfirm = { name, date ->
                onCreate(name, date)
                createOpen = false
            },
        )
    }
    deleteRoute?.let { route ->
        AlertDialog(
            onDismissRequest = { deleteRoute = null },
            title = { Text("Excluir rota") },
            text = { Text("Deseja excluir ${route.name}?") },
            confirmButton = {
                TextButton(onClick = { onDelete(route.id); deleteRoute = null }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deleteRoute = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun RouteStopCard(stop: RouteStopItem) {
    val context = LocalContext.current
    val company = stop.cliente
    val address = listOfNotNull(company?.endereco, company?.bairro, company?.cidade, company?.uf)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        (stop.stopOrder ?: 0).toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(company?.empresa ?: company?.nomeFantasia ?: "Empresa", fontWeight = FontWeight.SemiBold)
                    Text(company?.codigo ?: "Sem código", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (address.isNotBlank()) Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (stop.notes?.isNotBlank() == true) Text(stop.notes, style = MaterialTheme.typography.bodySmall)
            if (address.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        val uri = Uri.parse("geo:0,0?q=${Uri.encode("$address, Brasil")}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Place, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir no mapa")
                }
            }
        }
    }
}

@Composable
private fun CreateRouteParityDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova rota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data (AAAA-MM-DD)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name.trim(), date.trim().ifBlank { null }) }, enabled = name.isNotBlank() && !saving) {
                Text(if (saving) "Salvando..." else "Criar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
