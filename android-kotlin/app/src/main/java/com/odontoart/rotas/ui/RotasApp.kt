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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odontoart.rotas.MainUiState
import com.odontoart.rotas.MainViewModel
import com.odontoart.rotas.RouteItem
import com.odontoart.rotas.RouteStopItem

private enum class MainDestination(
    val label: String,
    val title: String,
    val icon: ImageVector,
) {
    HOME("Início", "Rotas", Icons.Rounded.Home),
    AGENDA("Agenda", "Minha agenda", Icons.Rounded.CalendarMonth),
    VISITS("Visitas", "Visitas", Icons.Rounded.Route),
    CLIENTS("Clientes", "Clientes", Icons.Rounded.Groups),
    MORE("Mais", "Mais recursos", Icons.Rounded.MoreHoriz),
}

@Composable
fun RotasApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.session == null) {
        LoginScreen(
            state = state,
            onLogin = viewModel::signIn,
            onClearError = viewModel::clearError,
        )
        return
    }

    MainShell(
        state = state,
        onLogout = viewModel::signOut,
        onReloadRoutes = viewModel::reloadRoutes,
        onSelectRoute = viewModel::selectRoute,
        onCreateRoute = viewModel::createRoute,
        onDeleteRoute = viewModel::deleteRoute,
        onClearError = viewModel::clearError,
    )
}

@Composable
private fun LoginScreen(
    state: MainUiState,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Route,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(14.dp).size(30.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Odontoart Rotas",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Acesse sua rotina de campo pelo app Android.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (!state.isConfigured) {
                        Text(
                            text = "SUPABASE_URL e SUPABASE_ANON_KEY não estão configurados no app Android.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (state.errorMessage != null) onClearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("E-mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (state.errorMessage != null) onClearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Senha") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )

                    state.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Button(
                        onClick = { onLogin(email, password) },
                        enabled = !state.isAuthenticating && state.isConfigured,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (state.isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(if (state.isAuthenticating) "Entrando..." else "Entrar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(
    state: MainUiState,
    onLogout: () -> Unit,
    onReloadRoutes: () -> Unit,
    onSelectRoute: (String) -> Unit,
    onCreateRoute: (String, String?) -> Unit,
    onDeleteRoute: (String) -> Unit,
    onClearError: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(MainDestination.HOME) }
    var showCreateRoute by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(destination.title, fontWeight = FontWeight.SemiBold)
                        state.profile?.displayName?.takeIf { it.isNotBlank() }?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Rounded.Logout, contentDescription = "Sair")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (destination == MainDestination.AGENDA && state.canEditRoutes) {
                FloatingActionButton(onClick = { showCreateRoute = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Criar rota")
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (destination) {
                MainDestination.HOME -> DashboardScreen(
                    state = state,
                    onOpenAgenda = { destination = MainDestination.AGENDA },
                    onReload = onReloadRoutes,
                )
                MainDestination.AGENDA -> AgendaScreen(
                    state = state,
                    onReload = onReloadRoutes,
                    onSelectRoute = onSelectRoute,
                    onDeleteRoute = onDeleteRoute,
                )
                MainDestination.VISITS -> NativeModuleScreen(
                    title = "Visitas",
                    description = "Fluxo de visitas em migração para uma experiência nativa, orientada a ações em campo.",
                )
                MainDestination.CLIENTS -> NativeModuleScreen(
                    title = "Clientes",
                    description = "Cadastro e consulta de clientes serão portados do web para cards, busca e ações nativas.",
                )
                MainDestination.MORE -> MoreScreen()
            }

            state.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        TextButton(onClick = onClearError) { Text("Fechar") }
                    }
                }
            }
        }
    }

    if (showCreateRoute) {
        CreateRouteDialog(
            isSaving = state.isSavingRoute,
            onDismiss = { showCreateRoute = false },
            onConfirm = { name, date ->
                onCreateRoute(name, date)
                showCreateRoute = false
            },
        )
    }
}

@Composable
private fun DashboardScreen(
    state: MainUiState,
    onOpenAgenda: () -> Unit,
    onReload: () -> Unit,
) {
    val selectedRoute = state.routes.firstOrNull { it.id == state.selectedRouteId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Sua operação na palma da mão",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = selectedRoute?.let { "Rota selecionada: ${it.name}" }
                            ?: "Selecione uma rota para começar o dia.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    FilledTonalButton(onClick = onOpenAgenda) {
                        Text("Abrir agenda")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Rotas",
                    value = state.routes.size.toString(),
                    icon = Icons.Rounded.Route,
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Paradas",
                    value = if (state.selectedRouteId == null) "—" else state.stops.size.toString(),
                    icon = Icons.Rounded.Place,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Rota atual", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Resumo rápido para uso em campo",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                IconButton(onClick = onReload, enabled = !state.isLoadingRoutes) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar")
                }
            }
        }

        if (state.isLoadingRoutes || state.isLoadingStops) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (selectedRoute == null) {
            item { EmptyCard("Nenhuma rota disponível para este usuário.") }
        } else {
            item { RouteSummaryCard(selectedRoute, state.stops.size, onOpenAgenda) }
            items(state.stops.take(3), key = { it.id }) { stop ->
                StopCard(stop = stop, compact = true)
            }
            if (state.stops.size > 3) {
                item {
                    OutlinedButton(onClick = onOpenAgenda, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver todas as ${state.stops.size} paradas")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RouteSummaryCard(route: RouteItem, stopCount: Int, onOpen: () -> Unit) {
    Card(onClick = onOpen, shape = RoundedCornerShape(20.dp)) {
        ListItem(
            headlineContent = { Text(route.name, fontWeight = FontWeight.SemiBold) },
            supportingContent = {
                Text(listOfNotNull(route.date, "$stopCount paradas").joinToString(" • "))
            },
            leadingContent = { Icon(Icons.Rounded.Route, contentDescription = null) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
        )
    }
}

@Composable
private fun AgendaScreen(
    state: MainUiState,
    onReload: () -> Unit,
    onSelectRoute: (String) -> Unit,
    onDeleteRoute: (String) -> Unit,
) {
    var routePendingDeletion by remember { mutableStateOf<RouteItem?>(null) }
    val selectedRoute = state.routes.firstOrNull { it.id == state.selectedRouteId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Rotas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Escolha a rota e siga as paradas",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onReload, enabled = !state.isLoadingRoutes) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar rotas")
                }
            }
        }

        if (state.routes.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.routes, key = { it.id }) { route ->
                        AssistChip(
                            onClick = { onSelectRoute(route.id) },
                            label = { Text(route.name, maxLines = 1) },
                            leadingIcon = if (route.id == state.selectedRouteId) {
                                { Icon(Icons.Rounded.Route, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                        )
                    }
                }
            }
        }

        if (state.isLoadingRoutes) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (selectedRoute == null) {
            item { EmptyCard("Nenhuma rota encontrada.") }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedRoute.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                selectedRoute.date?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            if (state.canEditRoutes) {
                                IconButton(onClick = { routePendingDeletion = selectedRoute }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Excluir rota")
                                }
                            }
                        }
                        Text(
                            text = "${state.stops.size} paradas",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (state.isLoadingStops) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.stops.isEmpty()) {
                item { EmptyCard("Esta rota ainda não possui paradas.") }
            } else {
                items(state.stops, key = { it.id }) { stop ->
                    StopCard(stop = stop)
                }
            }
        }
    }

    routePendingDeletion?.let { route ->
        AlertDialog(
            onDismissRequest = { routePendingDeletion = null },
            title = { Text("Excluir rota?") },
            text = { Text("A rota “${route.name}” será removida.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRoute(route.id)
                        routePendingDeletion = null
                    },
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { routePendingDeletion = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun StopCard(stop: RouteStopItem, compact: Boolean = false) {
    val context = LocalContext.current
    val client = stop.cliente
    val title = client?.nomeFantasia ?: client?.empresa ?: "Cliente"
    val address = listOfNotNull(
        client?.endereco,
        client?.complemento,
        client?.bairro,
        client?.cidade,
        client?.uf,
    ).filter { it.isNotBlank() }.joinToString(", ")

    Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = (stop.stopOrder ?: 0).toString().padStart(2, '0'),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    client?.codigo?.let {
                        Text("Código $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (address.isNotBlank()) {
                Text(
                    text = address,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (compact) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!compact && !stop.notes.isNullOrBlank()) {
                HorizontalDivider()
                Text(stop.notes, style = MaterialTheme.typography.bodyMedium)
            }

            if (!compact && address.isNotBlank()) {
                FilledTonalButton(
                    onClick = {
                        val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir no mapa")
                }
            }
        }
    }
}

@Composable
private fun NativeModuleScreen(title: String, description: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "A implementação deste módulo será feita em Kotlin/Compose, sem reutilizar páginas, CSS ou JavaScript do sistema web.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MoreScreen() {
    val modules = listOf(
        "Aceite digital" to "Assinaturas e confirmações",
        "Fila" to "Pendências operacionais",
        "KPI" to "Indicadores em formato mobile",
        "Novidades" to "Atualizações e comunicados",
        "Configurações" to "Preferências do aplicativo",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Recursos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        items(modules) { (title, subtitle) ->
            Card(shape = RoundedCornerShape(18.dp)) {
                ListItem(
                    headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(subtitle) },
                    trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CreateRouteDialog(
    isSaving: Boolean,
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da rota") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Data (AAAA-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, date.trim().takeIf { it.isNotEmpty() }) },
                enabled = name.isNotBlank() && !isSaving,
            ) {
                Text(if (isSaving) "Salvando..." else "Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
        },
    )
}
