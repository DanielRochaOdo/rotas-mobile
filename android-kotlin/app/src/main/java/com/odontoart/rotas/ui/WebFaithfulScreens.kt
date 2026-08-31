package com.odontoart.rotas.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odontoart.rotas.AgendaCompanyItem
import com.odontoart.rotas.ClienteListItem
import com.odontoart.rotas.MainUiState
import com.odontoart.rotas.ParityUiState
import com.odontoart.rotas.UserSession
import com.odontoart.rotas.VisitItem
import com.odontoart.rotas.WebIntegrationApi
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun FaithfulSectionHeader(
    title: String,
    description: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

@Composable
private fun FaithfulError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(message, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun FaithfulEmpty(message: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Text(message, modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusPill(label: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
internal fun FaithfulDashboardScreen(state: ParityUiState, onRefresh: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            FaithfulSectionHeader(
                "Dashboard",
                "Visão operacional do mês atual, usando a base de Dashboard do sistema web.",
                action = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, "Atualizar") } },
            )
        }
        state.errorMessage?.let { item { FaithfulError(it) } }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Visitas", state.dashboard.visits.toString(), Modifier.weight(1f))
                MetricCard("Concluídas", state.dashboard.completedVisits.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Pendentes", state.dashboard.pendingVisits.toString(), Modifier.weight(1f))
                MetricCard("Vidas", state.dashboard.completedLives.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Empresas", state.dashboard.companies.toString(), Modifier.weight(1f))
                MetricCard("Rotas", state.dashboard.routes.toString(), Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Fonte de dados", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Os indicadores usam o Supabase de Dashboard separado, da mesma forma que o web. Os demais módulos usam o Supabase primário autenticado.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun FaithfulRoutesScreen(
    state: ParityUiState,
    mainState: MainUiState,
    onSearch: (String, Int) -> Unit,
    onReloadSaved: () -> Unit,
    onSelectSaved: (String) -> Unit,
    onDeleteSaved: (String) -> Unit,
    onCreateRoute: (String, String?, List<String>) -> Unit,
) {
    var planning by rememberSaveable { mutableStateOf(true) }
    var search by rememberSaveable { mutableStateOf(state.agendaSearch) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FaithfulSectionHeader(
                "Rotas",
                "Planeje a rota a partir da mesma Agenda elegível do web ou consulte rotas já geradas.",
                action = { IconButton(onClick = { onSearch(search, state.agendaPage); onReloadSaved() }) { Icon(Icons.Rounded.Refresh, "Atualizar") } },
            )
        }
        state.errorMessage?.let { item { FaithfulError(it) } }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = planning, onClick = { planning = true }, label = { Text("Planejamento") })
                FilterChip(selected = !planning, onClick = { planning = false }, label = { Text("Rotas salvas") })
            }
        }

        if (planning) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Empresa") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        singleLine = true,
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSearch(search, 1) }) { Text("Buscar") }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${state.agendaTotal} empresas elegíveis", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    if (selectedIds.isNotEmpty()) {
                        Button(onClick = { createOpen = true }) {
                            Icon(Icons.Rounded.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Criar rota (${selectedIds.size})")
                        }
                    }
                }
            }
            if (state.agendaCompanies.isEmpty() && !state.isLoading) item { FaithfulEmpty("Nenhuma empresa elegível encontrada.") }
            items(state.agendaCompanies, key = { it.id }) { company ->
                val selected = company.id in selectedIds
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + company.id else selectedIds - company.id
                                },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(company.empresa ?: company.nomeFantasia ?: "Empresa", fontWeight = FontWeight.SemiBold)
                                Text(
                                    listOfNotNull(company.codigo, company.categoria, company.perfilVisita).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            company.situacao?.let(::StatusPill)
                        }
                        val address = addressOf(company.endereco, company.complemento, company.bairro, company.cidade, company.uf)
                        if (address.isNotBlank()) Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            company.vendedor?.let { Text("Vendedor: $it", style = MaterialTheme.typography.labelSmall) }
                            company.dataUltimaVisita?.let { Text("Última visita: ${formatDateShort(it)}", style = MaterialTheme.typography.labelSmall) }
                        }
                        if (address.isNotBlank()) {
                            TextButton(onClick = { openMap(context, address) }) {
                                Icon(Icons.Rounded.Place, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Abrir mapa")
                            }
                        }
                    }
                }
            }
            item {
                PaginationBar(
                    page = state.agendaPage,
                    total = state.agendaTotal,
                    pageSize = state.agendaPageSize,
                    onPage = { onSearch(search, it) },
                )
            }
        } else {
            if (mainState.routes.isEmpty() && !mainState.isLoadingRoutes) item { FaithfulEmpty("Nenhuma rota salva.") }
            items(mainState.routes, key = { it.id }) { route ->
                Card(
                    onClick = { onSelectSaved(route.id) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (route.id == mainState.selectedRouteId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Map, null)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(route.name, fontWeight = FontWeight.SemiBold)
                            Text(formatDateShort(route.date), style = MaterialTheme.typography.bodySmall)
                        }
                        if (mainState.canEditRoutes) {
                            IconButton(onClick = { onDeleteSaved(route.id) }) { Icon(Icons.Rounded.DeleteOutline, "Excluir") }
                        }
                    }
                }
            }
            if (mainState.selectedRouteId != null) {
                item { Text("Paradas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                items(mainState.stops, key = { it.id }) { stop ->
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("${stop.stopOrder ?: "-"}. ${stop.cliente?.empresa ?: stop.cliente?.nomeFantasia ?: "Empresa"}", fontWeight = FontWeight.SemiBold)
                            val address = addressOf(stop.cliente?.endereco, stop.cliente?.complemento, stop.cliente?.bairro, stop.cliente?.cidade, stop.cliente?.uf)
                            if (address.isNotBlank()) {
                                Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { openMap(context, address) }) { Text("Abrir mapa") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (createOpen) {
        RouteCreateDialog(
            count = selectedIds.size,
            onDismiss = { createOpen = false },
            onConfirm = { name, date ->
                onCreateRoute(name, date, selectedIds.toList())
                selectedIds = emptySet()
                createOpen = false
                planning = false
            },
        )
    }
}

@Composable
private fun RouteCreateDialog(count: Int, onDismiss: () -> Unit, onConfirm: (String, String?) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar rota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$count empresas selecionadas")
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome da rota") }, singleLine = true)
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data (AAAA-MM-DD)") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name.trim(), date.trim().ifBlank { null }) }, enabled = name.isNotBlank()) { Text("Criar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
internal fun FaithfulVisitsScreen(
    state: ParityUiState,
    onRefresh: () -> Unit,
    onComplete: (String, Int) -> Unit,
    onNoVisit: (String, String, String?) -> Unit,
) {
    var dateFilter by rememberSaveable { mutableStateOf("") }
    var complete by remember { mutableStateOf<VisitItem?>(null) }
    var noVisit by remember { mutableStateOf<VisitItem?>(null) }
    val context = LocalContext.current
    val filtered = if (dateFilter.isBlank()) state.visits else state.visits.filter { it.visitDate.startsWith(dateFilter) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FaithfulSectionHeader(
                "Agenda",
                "Visitas do período, sequência da rota e registro de atendimento.",
                action = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, "Atualizar") } },
            )
        }
        state.errorMessage?.let { item { FaithfulError(it) } }
        item {
            OutlinedTextField(
                value = dateFilter,
                onValueChange = { dateFilter = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Filtrar por data") },
                placeholder = { Text("AAAA-MM-DD") },
                singleLine = true,
            )
        }
        if (filtered.isEmpty() && !state.isLoading) item { FaithfulEmpty("Nenhuma visita encontrada.") }
        items(filtered, key = { it.id }) { visit ->
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(visit.cliente?.empresa ?: visit.cliente?.nomeFantasia ?: "Empresa", fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(visit.cliente?.codigo, formatDateShort(visit.visitDate), visit.visitTime).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusPill(
                            when {
                                !visit.noVisitReason.isNullOrBlank() -> "Não realizada"
                                visit.completedAt != null || visit.completedVidas != null -> "Concluída"
                                else -> "Pendente"
                            },
                        )
                    }
                    visit.assignedToName?.let { Text("Responsável: $it", style = MaterialTheme.typography.bodySmall) }
                    visit.perfilVisita?.let { Text("Perfil: $it", style = MaterialTheme.typography.bodySmall) }
                    visit.instructions?.takeIf(String::isNotBlank)?.let { Text("Instruções: $it", style = MaterialTheme.typography.bodySmall) }
                    visit.noVisitReason?.let { Text("Motivo: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    visit.completedVidas?.let { Text("Vidas: $it", style = MaterialTheme.typography.bodySmall) }
                    val address = addressOf(visit.cliente?.endereco, visit.cliente?.complemento, visit.cliente?.bairro, visit.cliente?.cidade, visit.cliente?.uf)
                    if (address.isNotBlank()) {
                        TextButton(onClick = { openMap(context, address) }) {
                            Icon(Icons.Rounded.Place, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Abrir navegação")
                        }
                    }
                    val registered = visit.completedAt != null || visit.completedVidas != null || !visit.noVisitReason.isNullOrBlank()
                    if (!registered) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { complete = visit }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.CheckCircle, null)
                                Spacer(Modifier.width(5.dp))
                                Text("Concluir")
                            }
                            OutlinedButton(onClick = { noVisit = visit }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Block, null)
                                Spacer(Modifier.width(5.dp))
                                Text("Não realizada")
                            }
                        }
                    }
                }
            }
        }
    }

    complete?.let { visit ->
        var vidas by rememberSaveable(visit.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { complete = null },
            title = { Text("Concluir visita") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(visit.cliente?.empresa ?: "Empresa")
                    visit.perfilVisita?.let { Text("Perfil: $it", style = MaterialTheme.typography.bodySmall) }
                    OutlinedTextField(
                        value = vidas,
                        onValueChange = { if (it.all(Char::isDigit)) vidas = it },
                        label = { Text("Quantidade de vidas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
            },
            confirmButton = { Button(onClick = { onComplete(visit.id, vidas.toIntOrNull() ?: 0); complete = null }, enabled = vidas.isNotBlank()) { Text("Registrar") } },
            dismissButton = { TextButton(onClick = { complete = null }) { Text("Cancelar") } },
        )
    }
    noVisit?.let { visit ->
        NoVisitFaithfulDialog(
            company = visit.cliente?.empresa ?: "Empresa",
            onDismiss = { noVisit = null },
            onConfirm = { reason, obs -> onNoVisit(visit.id, reason, obs); noVisit = null },
        )
    }
}

@Composable
private fun NoVisitFaithfulDialog(company: String, onDismiss: () -> Unit, onConfirm: (String, String?) -> Unit) {
    val reasons = listOf("NAO AUTORIZADO", "NAO CHEGOU A TEMPO", "ENDERECO NAO LOCALIZADO", "AUSENTE NO DIA")
    var reason by rememberSaveable { mutableStateOf("") }
    var observation by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Visita não realizada") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(company)
                Text("Motivo", fontWeight = FontWeight.SemiBold)
                reasons.forEach { item -> FilterChip(selected = reason == item, onClick = { reason = item }, label = { Text(item) }) }
                OutlinedTextField(value = observation, onValueChange = { observation = it }, label = { Text("Observação") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onConfirm(reason, observation.trim().ifBlank { null }) }, enabled = reason.isNotBlank()) { Text("Registrar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
internal fun FaithfulCompaniesScreen(
    state: ParityUiState,
    session: UserSession,
    integrations: WebIntegrationApi,
    onLoad: (String, String, String?, Int) -> Unit,
    onCreate: (JSONObject) -> Unit,
    onUpdate: (String, JSONObject) -> Unit,
    onDelete: (String) -> Unit,
) {
    var search by rememberSaveable { mutableStateOf(state.clientSearch) }
    var mode by rememberSaveable { mutableStateOf(state.clientSearchMode) }
    var situacao by rememberSaveable { mutableStateOf(state.clientSituacao ?: "") }
    var selected by remember { mutableStateOf<ClienteListItem?>(null) }
    var editing by remember { mutableStateOf<ClienteListItem?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ClienteListItem?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FaithfulSectionHeader(
                "Empresas",
                "Cadastro e consulta usando as mesmas RPCs, filtros e dados do módulo web.",
                action = { IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, "Cadastrar") } },
            )
        }
        state.errorMessage?.let { item { FaithfulError(it) } }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar empresa") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
            )
        }
        item {
            Text("Buscar por", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("codigo" to "Código", "empresa" to "Empresa", "geral" to "Geral").forEach { (value, label) ->
                    FilterChip(selected = mode == value, onClick = { mode = value }, label = { Text(label) })
                }
            }
        }
        item {
            Text("Situação", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("" to "Todas", "Ativo" to "Ativo", "Suspenso/Inadimplente" to "Suspenso", "Cancelado" to "Cancelado").forEach { (value, label) ->
                    FilterChip(selected = situacao == value, onClick = { situacao = value }, label = { Text(label) })
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onLoad(search, mode, situacao.ifBlank { null }, 1) }) { Text("Aplicar filtros") }
                Spacer(Modifier.width(12.dp))
                Text("${state.clientsTotal} registros", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (state.clients.isEmpty() && !state.isLoading) item { FaithfulEmpty("Nenhuma empresa encontrada.") }
        items(state.clients, key = { it.id }) { company ->
            Card(onClick = { selected = company }, shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Business, null)
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(company.empresa ?: company.nomeFantasia ?: "Empresa", fontWeight = FontWeight.SemiBold)
                            Text(listOfNotNull(company.codigo, company.cnpj).joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        company.situacao?.let(::StatusPill)
                    }
                    val address = addressOf(company.endereco, company.complemento, company.bairro, company.cidade, company.uf)
                    if (address.isNotBlank()) Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        company.vidasQtde?.let { Text("Vidas: $it", style = MaterialTheme.typography.labelSmall) }
                        company.categoria?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
        item {
            PaginationBar(
                page = state.clientsPage,
                total = state.clientsTotal,
                pageSize = state.clientsPageSize,
                onPage = { onLoad(search, mode, situacao.ifBlank { null }, it) },
            )
        }
    }

    selected?.let { company ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(company.empresa ?: company.nomeFantasia ?: "Empresa") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Detail("Código", company.codigo)
                    Detail("CNPJ", company.cnpj)
                    Detail("Vidas", company.vidasQtde?.toString())
                    Detail("Corte", company.corte?.toString())
                    Detail("Vencimento", company.venc?.toString())
                    Detail("Valor", company.valor?.let(::currency))
                    Detail("Reajuste", company.reajustePct?.let { "$it%" })
                    Detail("Competência", company.competencia)
                    Detail("Última visita", company.dataUltimaVisita?.let(::formatDateShort))
                    Detail("Pessoa", company.pessoa)
                    Detail("Contato", company.contato)
                    Detail("Grupo", company.grupo)
                    Detail("Situação", company.situacao)
                    Detail("Categoria", company.categoria)
                    Detail("Perfil de visita", company.perfilVisita)
                    Detail("Regra de visita", company.regraVisitaObservacao)
                    Detail("Obs. comercial", company.obsComercial)
                    Detail("Observações", company.obs)
                    Detail("CEP", company.cep)
                    Detail("Endereço", addressOf(company.endereco, company.complemento, company.bairro, company.cidade, company.uf))
                }
            },
            confirmButton = { TextButton(onClick = { editing = company; selected = null }) { Icon(Icons.Rounded.Edit, null); Spacer(Modifier.width(5.dp)); Text("Editar") } },
            dismissButton = { TextButton(onClick = { deleting = company; selected = null }) { Icon(Icons.Rounded.DeleteOutline, null); Spacer(Modifier.width(5.dp)); Text("Excluir") } },
        )
    }
    if (creating) {
        FaithfulCompanyForm(
            title = "Cadastrar empresa",
            initial = null,
            session = session,
            integrations = integrations,
            onDismiss = { creating = false },
            onSave = { onCreate(it); creating = false },
        )
    }
    editing?.let { company ->
        FaithfulCompanyForm(
            title = "Editar empresa",
            initial = company,
            session = session,
            integrations = integrations,
            onDismiss = { editing = null },
            onSave = { onUpdate(company.id, it); editing = null },
        )
    }
    deleting?.let { company ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Excluir empresa") },
            text = { Text("Confirma a exclusão de ${company.empresa ?: company.codigo ?: "esta empresa"}?") },
            confirmButton = { Button(onClick = { onDelete(company.id); deleting = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FaithfulCompanyForm(
    title: String,
    initial: ClienteListItem?,
    session: UserSession,
    integrations: WebIntegrationApi,
    onDismiss: () -> Unit,
    onSave: (JSONObject) -> Unit,
) {
    var codigo by rememberSaveable(initial?.id) { mutableStateOf(initial?.codigo.orEmpty()) }
    var cnpj by rememberSaveable(initial?.id) { mutableStateOf(initial?.cnpj.orEmpty()) }
    var empresa by rememberSaveable(initial?.id) { mutableStateOf(initial?.empresa.orEmpty()) }
    var vidas by rememberSaveable(initial?.id) { mutableStateOf(initial?.vidasQtde?.toString().orEmpty()) }
    var corte by rememberSaveable(initial?.id) { mutableStateOf(initial?.corte?.toString().orEmpty()) }
    var venc by rememberSaveable(initial?.id) { mutableStateOf(initial?.venc?.toString().orEmpty()) }
    var valor by rememberSaveable(initial?.id) { mutableStateOf(initial?.valor?.toString().orEmpty()) }
    var reajuste by rememberSaveable(initial?.id) { mutableStateOf(initial?.reajustePct?.toString().orEmpty()) }
    var competencia by rememberSaveable(initial?.id) { mutableStateOf(initial?.competencia.orEmpty()) }
    var dataUltima by rememberSaveable(initial?.id) { mutableStateOf(initial?.dataUltimaVisita?.take(10).orEmpty()) }
    var pessoa by rememberSaveable(initial?.id) { mutableStateOf(initial?.pessoa.orEmpty()) }
    var contato by rememberSaveable(initial?.id) { mutableStateOf(initial?.contato.orEmpty()) }
    var grupo by rememberSaveable(initial?.id) { mutableStateOf(initial?.grupo.orEmpty()) }
    var situacao by rememberSaveable(initial?.id) { mutableStateOf(initial?.situacao ?: "Ativo") }
    var categoria by rememberSaveable(initial?.id) { mutableStateOf(initial?.categoria.orEmpty()) }
    var perfil by rememberSaveable(initial?.id) { mutableStateOf(initial?.perfilVisita.orEmpty()) }
    var regra by rememberSaveable(initial?.id) { mutableStateOf(initial?.regraVisitaObservacao.orEmpty()) }
    var obsComercial by rememberSaveable(initial?.id) { mutableStateOf(initial?.obsComercial.orEmpty()) }
    var obs by rememberSaveable(initial?.id) { mutableStateOf(initial?.obs.orEmpty()) }
    var cep by rememberSaveable(initial?.id) { mutableStateOf(initial?.cep.orEmpty()) }
    var endereco by rememberSaveable(initial?.id) { mutableStateOf(initial?.endereco.orEmpty()) }
    var complemento by rememberSaveable(initial?.id) { mutableStateOf(initial?.complemento.orEmpty()) }
    var bairro by rememberSaveable(initial?.id) { mutableStateOf(initial?.bairro.orEmpty()) }
    var cidade by rememberSaveable(initial?.id) { mutableStateOf(initial?.cidade.orEmpty()) }
    var uf by rememberSaveable(initial?.id) { mutableStateOf(initial?.uf.orEmpty()) }
    var lookupMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var lookupLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Identificação", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = codigo, onValueChange = { codigo = it }, label = { Text("Código") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                lookupLoading = true
                                lookupMessage = null
                                runCatching { integrations.fetchOdontoartEmpresa(session, codigo) }
                                    .onSuccess { row ->
                                        if (row == null) lookupMessage = "Empresa não encontrada no ERP."
                                        else {
                                            empresa = row.readString("RazaoSocial", "Empresa", "Nome") ?: empresa
                                            cnpj = row.readString("Cnpj", "CNPJ") ?: cnpj
                                            valor = row.readNumber("ValorTitular")?.toString() ?: valor
                                            lookupMessage = "Dados do ERP carregados."
                                        }
                                    }
                                    .onFailure { lookupMessage = it.message }
                                lookupLoading = false
                            }
                        },
                        enabled = codigo.isNotBlank() && !lookupLoading,
                    ) { Text("Consultar código") }
                }
                OutlinedTextField(value = cnpj, onValueChange = { cnpj = it }, label = { Text("CNPJ") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            lookupLoading = true
                            lookupMessage = null
                            runCatching { integrations.fetchCnpj(cnpj) }
                                .onSuccess { result ->
                                    empresa = result.razaoSocial ?: empresa
                                    cep = result.cep ?: cep
                                    endereco = listOfNotNull(result.logradouro, result.numero).joinToString(", ").ifBlank { endereco }
                                    bairro = result.bairro ?: bairro
                                    cidade = result.cidade ?: cidade
                                    uf = result.uf ?: uf
                                    lookupMessage = "Dados públicos do CNPJ carregados."
                                }
                                .onFailure { lookupMessage = it.message }
                            lookupLoading = false
                        }
                    },
                    enabled = cnpj.isNotBlank() && !lookupLoading,
                ) { Text("Consultar CNPJ") }
                OutlinedTextField(value = empresa, onValueChange = { empresa = it }, label = { Text("Empresa") }, modifier = Modifier.fillMaxWidth())

                Text("Contrato e comercial", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vidas, onValueChange = { vidas = it }, label = { Text("Vidas") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = valor, onValueChange = { valor = it }, label = { Text("Valor") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = corte, onValueChange = { corte = it }, label = { Text("Corte") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = venc, onValueChange = { venc = it }, label = { Text("Venc.") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = reajuste, onValueChange = { reajuste = it }, label = { Text("Reajuste %") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = competencia, onValueChange = { competencia = it }, label = { Text("Competência") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dataUltima, onValueChange = { dataUltima = it }, label = { Text("Data da última visita") }, placeholder = { Text("AAAA-MM-DD") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pessoa, onValueChange = { pessoa = it }, label = { Text("Pessoa de contato") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contato, onValueChange = { contato = it }, label = { Text("Contato") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = grupo, onValueChange = { grupo = it }, label = { Text("Grupo") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = situacao, onValueChange = { situacao = it }, label = { Text("Situação") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = categoria, onValueChange = { categoria = it }, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = perfil, onValueChange = { perfil = it }, label = { Text("Perfil de visita") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = regra, onValueChange = { regra = it }, label = { Text("Regra de visita") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = obsComercial, onValueChange = { obsComercial = it }, label = { Text("Obs. comercial") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = obs, onValueChange = { obs = it }, label = { Text("Observações") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                Text("Endereço", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = cep, onValueChange = { cep = it }, label = { Text("CEP") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            lookupLoading = true
                            lookupMessage = null
                            runCatching { integrations.fetchCep(cep) }
                                .onSuccess { result ->
                                    cep = result.cep ?: cep
                                    endereco = result.endereco ?: endereco
                                    if (!result.complemento.isNullOrBlank()) complemento = result.complemento
                                    bairro = result.bairro ?: bairro
                                    cidade = result.cidade ?: cidade
                                    uf = result.uf ?: uf
                                    lookupMessage = "CEP localizado."
                                }
                                .onFailure { lookupMessage = it.message }
                            lookupLoading = false
                        }
                    },
                    enabled = cep.isNotBlank() && !lookupLoading,
                ) { Text("Buscar CEP") }
                OutlinedTextField(value = endereco, onValueChange = { endereco = it }, label = { Text("Endereço") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = complemento, onValueChange = { complemento = it }, label = { Text("Complemento") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bairro, onValueChange = { bairro = it }, label = { Text("Bairro") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cidade, onValueChange = { cidade = it }, label = { Text("Cidade") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = uf, onValueChange = { uf = it.uppercase().take(2) }, label = { Text("UF") }, modifier = Modifier.width(86.dp))
                }
                lookupMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val payload = JSONObject()
                    payload.putNullable("codigo", codigo)
                    payload.putNullable("cnpj", cnpj)
                    payload.putNullable("empresa", empresa)
                    payload.putNumber("vidas_qtde", vidas)
                    payload.putNumber("corte", corte)
                    payload.putNumber("venc", venc)
                    payload.putNumber("valor", valor)
                    payload.putNumber("reajuste_pct", reajuste)
                    payload.putNullable("competencia", competencia)
                    payload.putNullable("data_da_ultima_visita", dataUltima)
                    payload.putNullable("pessoa", pessoa)
                    payload.putNullable("contato", contato)
                    payload.putNullable("grupo", grupo)
                    payload.putNullable("situacao", situacao.ifBlank { "Ativo" })
                    payload.putNullable("categoria", categoria)
                    payload.putNullable("perfil_visita", perfil)
                    payload.putNullable("regra_visita_observacao", regra)
                    payload.putNullable("obs_comercial", obsComercial)
                    payload.putNullable("obs", obs)
                    payload.putNullable("cep", cep)
                    payload.putNullable("endereco", endereco)
                    payload.putNullable("complemento", complemento)
                    payload.putNullable("bairro", bairro)
                    payload.putNullable("cidade", cidade)
                    payload.putNullable("uf", uf)
                    onSave(payload)
                },
                enabled = empresa.isNotBlank() || codigo.isNotBlank(),
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
internal fun FaithfulAcceptanceScreen(state: ParityUiState, onRegister: (String, Int) -> Unit) {
    var selectedDate by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FaithfulSectionHeader("Aceite digital", "Registre as vidas das datas pendentes do vendedor.") }
        state.errorMessage?.let { item { FaithfulError(it) } }
        if (state.pendingAcceptanceDates.isEmpty()) item { FaithfulEmpty("Nenhuma data pendente de aceite.") }
        items(state.pendingAcceptanceDates) { date ->
            Card(onClick = { selectedDate = date }, shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDateShort(date), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("Registrar", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (state.digitalSummary.isNotEmpty()) {
            item { Text("Registros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(state.digitalSummary, key = { it.id }) { row ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(formatDateShort(row.entryDate), modifier = Modifier.weight(1f))
                        Text("${row.vidas} vidas", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
    selectedDate?.let { date ->
        var vidas by rememberSaveable(date) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedDate = null },
            title = { Text("Aceite · ${formatDateShort(date)}") },
            text = { OutlinedTextField(value = vidas, onValueChange = { if (it.all(Char::isDigit)) vidas = it }, label = { Text("Vidas") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
            confirmButton = { Button(onClick = { onRegister(date, vidas.toIntOrNull() ?: 0); selectedDate = null }, enabled = vidas.isNotBlank()) { Text("Registrar") } },
            dismissButton = { TextButton(onClick = { selectedDate = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
internal fun FaithfulQueueScreen(
    state: ParityUiState,
    onRefresh: () -> Unit,
    onAction: (String, String, Int?, Int?, String?) -> Unit,
) {
    var selected by remember { mutableStateOf<com.odontoart.rotas.QueueControlItem?>(null) }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FaithfulSectionHeader("Fila", "Controle de liberação de empresas para roteirização.", action = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, "Atualizar") } }) }
        state.errorMessage?.let { item { FaithfulError(it) } }
        if (state.queue.isEmpty() && !state.isLoading) item { FaithfulEmpty("Nenhuma empresa na fila.") }
        items(state.queue, key = { it.empresaId }) { row ->
            Card(onClick = { selected = row }, shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(row.empresa ?: row.codigo ?: "Empresa", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        StatusPill(row.effectiveState)
                    }
                    Text(listOfNotNull(row.codigo, row.cnpj).joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                    row.daysRemaining?.let { Text("Dias restantes: $it", style = MaterialTheme.typography.bodySmall) }
                    row.manualReason?.let { Text("Motivo: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
    selected?.let { row ->
        var days by rememberSaveable(row.empresaId) { mutableStateOf("") }
        var reason by rememberSaveable(row.empresaId) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(row.empresa ?: row.codigo ?: "Fila") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Estado: ${row.effectiveState}")
                    OutlinedTextField(value = days, onValueChange = { if (it.all(Char::isDigit)) days = it }, label = { Text("Dias") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Motivo") })
                    Button(onClick = { onAction(row.empresaId, "RELEASE_NOW", null, null, reason.ifBlank { null }); selected = null }, modifier = Modifier.fillMaxWidth()) { Text("Liberar agora") }
                    OutlinedButton(onClick = { onAction(row.empresaId, "SET_WAITING_DAYS", days.toIntOrNull(), null, reason.ifBlank { null }); selected = null }, enabled = days.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Alterar prazo") }
                    OutlinedButton(onClick = { onAction(row.empresaId, "BLOCK_DAYS", null, days.toIntOrNull(), reason.ifBlank { null }); selected = null }, enabled = days.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Bloquear") }
                    TextButton(onClick = { onAction(row.empresaId, "UNBLOCK", null, null, reason.ifBlank { null }); selected = null }, modifier = Modifier.fillMaxWidth()) { Text("Desbloquear") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Fechar") } },
        )
    }
}

@Composable
internal fun FaithfulKpiScreen(state: ParityUiState, onPeriod: (Int) -> Unit) {
    var period by rememberSaveable { mutableStateOf(30) }
    val sales = state.kpiSnapshots.sumOf { it.vendasQtde }
    val cancellations = state.kpiSnapshots.sumOf { it.cancelamentosQtde }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FaithfulSectionHeader("KPI", "Indicadores de vidas por empresa e período.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 7, 15, 30).forEach { value ->
                    FilterChip(selected = period == value, onClick = { period = value; onPeriod(value) }, label = { Text("${value}d") })
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Vendas", sales.toString(), Modifier.weight(1f))
                MetricCard("Cancelamentos", cancellations.toString(), Modifier.weight(1f))
            }
        }
        state.errorMessage?.let { item { FaithfulError(it) } }
        items(state.kpiSnapshots, key = { it.id }) { row ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(row.empresa ?: row.codigo, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        row.status?.let(::StatusPill)
                    }
                    Text("Código ${row.codigo} · Vidas ${row.vidasQtde ?: 0}", style = MaterialTheme.typography.bodySmall)
                    Text("Δ ${row.delta} · vendas ${row.vendasQtde} · cancelamentos ${row.cancelamentosQtde}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
internal fun FaithfulNewsScreen(state: ParityUiState, onRefresh: () -> Unit, onRead: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FaithfulSectionHeader("Novidades", "Melhorias, atualizações e avisos do Sistema de Rotas.", action = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, "Atualizar") } }) }
        state.errorMessage?.let { item { FaithfulError(it) } }
        if (state.news.isEmpty() && !state.isLoading) item { FaithfulEmpty("Nenhuma novidade publicada.") }
        items(state.news, key = { it.id }) { row ->
            Card(onClick = { onRead(row.id) }, shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { StatusPill(row.tipo); StatusPill(row.modulo) }
                    Text(row.titulo, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text(stripHtml(row.descricao), maxLines = 5, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatDateShort(row.dataPublicacao), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
internal fun FaithfulLogsScreen(state: ParityUiState, onFilter: (String?, String?) -> Unit) {
    var action by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { FaithfulSectionHeader("Logs", "Registros de cadastro, alteração e exclusão realizados no sistema.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(null to "Todas", "INSERT" to "Cadastro", "UPDATE" to "Alteração", "DELETE" to "Exclusão").forEach { (value, label) ->
                    FilterChip(selected = action == value, onClick = { action = value; onFilter(value, null) }, label = { Text(label) })
                }
            }
        }
        state.errorMessage?.let { item { FaithfulError(it) } }
        items(state.logs, key = { it.id }) { row ->
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row { StatusPill(row.action); Spacer(Modifier.width(6.dp)); StatusPill(row.tableName) }
                    Text(row.userName ?: row.userId ?: "Sistema", fontWeight = FontWeight.SemiBold)
                    Text(formatDateShort(row.createdAt), style = MaterialTheme.typography.bodySmall)
                    row.recordId?.let { Text("ID: $it", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@Composable
internal fun FaithfulSettingsScreen(state: ParityUiState, onRefresh: () -> Unit, onInactive: (String, Boolean) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { FaithfulSectionHeader("Configurações", "Usuários, papéis e permissões do sistema.", action = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, "Atualizar") } }) }
        state.errorMessage?.let { item { FaithfulError(it) } }
        items(state.managedProfiles, key = { it.id }) { row ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(row.nome ?: row.displayName ?: "Usuário", fontWeight = FontWeight.SemiBold)
                    Text(row.role, style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Usuário inativo", modifier = Modifier.weight(1f))
                        Switch(checked = row.isInactive, onCheckedChange = { onInactive(row.id, it) })
                    }
                    Text(
                        "Pré-cadastro: ${if (row.canAccessPreCadastro) "sim" else "não"} · Próxima rota: ${if (row.canAccessNextRouteDashboard) "sim" else "não"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaginationBar(page: Int, total: Long, pageSize: Int, onPage: (Int) -> Unit) {
    val pages = maxOf(1, ((total + pageSize - 1) / pageSize).toInt())
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = { onPage((page - 1).coerceAtLeast(1)) }, enabled = page > 1) { Icon(Icons.Rounded.ChevronLeft, "Anterior") }
        Text("Página $page de $pages", modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = { onPage((page + 1).coerceAtMost(pages)) }, enabled = page < pages) { Icon(Icons.Rounded.ChevronRight, "Próxima") }
    }
}

@Composable
private fun Detail(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun addressOf(vararg parts: String?): String = parts.filterNotNull().map(String::trim).filter(String::isNotBlank).joinToString(", ")

private fun openMap(context: android.content.Context, address: String) {
    runCatching {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode("$address, Brasil")}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

private fun formatDateShort(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    val raw = value.take(10)
    val parts = raw.split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else raw
}

private fun currency(value: Double): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
private fun stripHtml(value: String): String = value.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()

private fun JSONObject.putNullable(key: String, value: String) {
    put(key, value.trim().takeIf(String::isNotBlank) ?: JSONObject.NULL)
}

private fun JSONObject.putNumber(key: String, value: String) {
    val parsed = value.trim().replace(",", ".").toDoubleOrNull()
    put(key, parsed ?: JSONObject.NULL)
}

private fun JSONObject.readString(vararg keys: String): String? {
    val names = keys.map(String::lowercase).toSet()
    val iterator = keys()
    while (iterator.hasNext()) {
        val key = iterator.next()
        if (key.lowercase() !in names || isNull(key)) continue
        val value = optString(key).trim()
        if (value.isNotBlank()) return value
    }
    return null
}

private fun JSONObject.readNumber(vararg keys: String): Double? {
    keys.forEach { target ->
        val iterator = keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (!key.equals(target, ignoreCase = true) || isNull(key)) continue
            return runCatching { getDouble(key) }.getOrNull() ?: optString(key).replace(",", ".").toDoubleOrNull()
        }
    }
    return null
}
