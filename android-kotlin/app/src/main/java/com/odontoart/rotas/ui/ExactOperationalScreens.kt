package com.odontoart.rotas.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.odontoart.rotas.RoutesWebRepository
import com.odontoart.rotas.UserProfile
import com.odontoart.rotas.UserSession
import com.odontoart.rotas.VisitItem
import com.odontoart.rotas.WebIntegrationApi
import com.odontoart.rotas.ui.theme.RotasSea
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

@Composable
private fun WebPageHeader(title: String, description: String, action: (@Composable () -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.15f)),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            action?.invoke()
        }
    }
}

@Composable
private fun WebCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.15f)),
    ) { content() }
}

@Composable
private fun WebStatus(label: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = RotasSea.copy(alpha = 0.10f), border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.20f))) {
        Text(label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun WebError(message: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Text(message, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
internal fun ExactDashboardScreen(state: ParityUiState, onRefresh: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf("visao") }
    val tabs = listOf("visao" to "Geral", "performance" to "Performance", "comercial" to "Comercial", "cobertura" to "Cobertura", "qualidade" to "Qualidade")
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            WebPageHeader(
                title = "Dashboard",
                description = "Análise robusta com cruzamento de visitas, vidas registradas, aceite digital e cobertura comercial.",
                action = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar") } },
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                tabs.forEach { (key, label) -> FilterChip(selected = tab == key, onClick = { tab = key }, label = { Text(label) }) }
            }
        }
        state.errorMessage?.let { item { WebError(it) } }
        if (tab == "visao") {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExactMetricCard("Visitas totais", state.dashboard.visits.toString(), Modifier.weight(1f))
                    ExactMetricCard("Concluídas", state.dashboard.completedVisits.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExactMetricCard("Pendentes", state.dashboard.pendingVisits.toString(), Modifier.weight(1f))
                    ExactMetricCard("Vidas totais", state.dashboard.completedLives.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExactMetricCard("Empresas", state.dashboard.companies.toString(), Modifier.weight(1f))
                    ExactMetricCard("Rotas", state.dashboard.routes.toString(), Modifier.weight(1f))
                }
            }
        } else {
            item {
                WebCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(tabs.first { it.first == tab }.second, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Esta aba agora faz parte do mesmo fluxo visual do Dashboard Estratégico web. Os cálculos avançados e filtros estão sendo alimentados pelo Supabase de Dashboard separado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ExactMetricCard("Visitas", state.dashboard.visits.toString(), Modifier.weight(1f))
                            ExactMetricCard("Vidas", state.dashboard.completedLives.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExactMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.15f))) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.width(16.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
internal fun ExactRoutesScreen(
    state: ParityUiState,
    mainState: MainUiState,
    onSearch: (String, Int) -> Unit,
    onReloadSaved: () -> Unit,
    onSelectSaved: (String) -> Unit,
    onDeleteSaved: (String) -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf("planejamento") }
    var search by rememberSaveable { mutableStateOf(state.agendaSearch) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var routeError by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val routeRepository = remember { RoutesWebRepository() }
    val context = LocalContext.current

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            WebPageHeader(
                title = "Rotas",
                description = "Planejamento da Agenda elegível, seleção de empresas e sequência de visitas.",
                action = { IconButton(onClick = { onSearch(search, state.agendaPage); onReloadSaved() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar") } },
            )
        }
        state.errorMessage?.let { item { WebError(it) } }
        routeError?.let { item { WebError(it) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == "planejamento", onClick = { mode = "planejamento" }, label = { Text("Planejamento") })
                FilterChip(selected = mode == "salvas", onClick = { mode = "salvas" }, label = { Text("Rotas salvas") })
            }
        }
        if (mode == "planejamento") {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.weight(1f), label = { Text("Empresa") }, leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }, singleLine = true)
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSearch(search, 1) }) { Text("Buscar") }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${state.agendaTotal} empresas elegíveis", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    if (selectedIds.isNotEmpty()) {
                        Button(onClick = { createOpen = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("Criar rota (${selectedIds.size})")
                        }
                    }
                }
            }
            items(state.agendaCompanies, key = { it.id }) { company ->
                ExactAgendaCompanyCard(
                    company = company,
                    selected = company.id in selectedIds,
                    onSelected = { checked -> selectedIds = if (checked) selectedIds + company.id else selectedIds - company.id },
                    onMap = { addressOf(company.endereco, company.complemento, company.bairro, company.cidade, company.uf).takeIf(String::isNotBlank)?.let { openMap(context, it) } },
                )
            }
            item { ExactPagination(page = state.agendaPage, total = state.agendaTotal, pageSize = state.agendaPageSize, onPage = { onSearch(search, it) }) }
        } else {
            items(mainState.routes, key = { it.id }) { route ->
                WebCard {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            TextButton(onClick = { onSelectSaved(route.id) }) { Text(route.name, fontWeight = FontWeight.SemiBold) }
                            Text(formatDateBrExact(route.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (mainState.canEditRoutes) IconButton(onClick = { onDeleteSaved(route.id) }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Excluir") }
                    }
                }
            }
            if (mainState.selectedRouteId != null) {
                item { Text("Paradas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                items(mainState.stops, key = { it.id }) { stop ->
                    WebCard {
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
        var name by rememberSaveable { mutableStateOf("") }
        var date by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { createOpen = false },
            title = { Text("Criar rota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${selectedIds.size} empresas selecionadas")
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome da rota") }, singleLine = true)
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data (AAAA-MM-DD)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val session = mainState.session
                    if (session == null) {
                        routeError = "Sessão inválida. Faça login novamente."
                        return@Button
                    }
                    scope.launch {
                        runCatching { routeRepository.createRouteWithStops(session, name, date.ifBlank { null }, selectedIds.toList()) }
                            .onSuccess {
                                routeError = null
                                selectedIds = emptySet()
                                createOpen = false
                                mode = "salvas"
                                onReloadSaved()
                            }
                            .onFailure { routeError = it.message ?: "Falha ao criar rota." }
                    }
                }, enabled = name.isNotBlank()) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { createOpen = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ExactAgendaCompanyCard(company: AgendaCompanyItem, selected: Boolean, onSelected: (Boolean) -> Unit, onMap: () -> Unit) {
    WebCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = selected, onCheckedChange = onSelected)
                Column(modifier = Modifier.weight(1f)) {
                    Text(company.empresa ?: company.nomeFantasia ?: "Empresa", fontWeight = FontWeight.SemiBold)
                    Text(listOfNotNull(company.codigo, company.categoria, company.perfilVisita).joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                company.situacao?.let { WebStatus(it) }
            }
            val address = addressOf(company.endereco, company.complemento, company.bairro, company.cidade, company.uf)
            if (address.isNotBlank()) Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                company.vendedor?.let { Text("Vendedor: $it", style = MaterialTheme.typography.labelSmall) }
                company.dataUltimaVisita?.let { Text("Última visita: ${formatDateBrExact(it)}", style = MaterialTheme.typography.labelSmall) }
            }
            if (address.isNotBlank()) TextButton(onClick = onMap) { Icon(Icons.Rounded.Place, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Abrir mapa") }
        }
    }
}

@Composable
internal fun ExactVisitsScreen(
    state: ParityUiState,
    profile: UserProfile?,
    onRefresh: () -> Unit,
    onComplete: (String, Int) -> Unit,
    onNoVisit: (String, String, String?) -> Unit,
) {
    var dateFilter by rememberSaveable { mutableStateOf("") }
    var complete by remember { mutableStateOf<VisitItem?>(null) }
    var noVisit by remember { mutableStateOf<VisitItem?>(null) }
    val context = LocalContext.current
    val filtered = if (dateFilter.isBlank()) state.visits else state.visits.filter { it.visitDate.startsWith(dateFilter) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { WebPageHeader("Agenda", "Visitas, sequência da rota e registro de atendimento.", action = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar") } }) }
        state.errorMessage?.let { item { WebError(it) } }
        item { OutlinedTextField(value = dateFilter, onValueChange = { dateFilter = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Filtrar data") }, placeholder = { Text("AAAA-MM-DD") }, leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) }, singleLine = true) }
        items(filtered, key = { it.id }) { visit ->
            val registered = visit.completedAt != null || visit.completedVidas != null || !visit.noVisitReason.isNullOrBlank()
            WebCard {
                Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(visit.cliente?.empresa ?: visit.cliente?.nomeFantasia ?: "Empresa", fontWeight = FontWeight.SemiBold)
                            Text(listOfNotNull(visit.cliente?.codigo, formatDateBrExact(visit.visitDate), visit.visitTime).joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        WebStatus(if (!visit.noVisitReason.isNullOrBlank()) "Não realizada" else if (registered) "Concluída" else "Pendente")
                    }
                    visit.assignedToName?.let { Text("Responsável: $it", style = MaterialTheme.typography.bodySmall) }
                    visit.perfilVisita?.let { Text("Perfil: $it", style = MaterialTheme.typography.bodySmall) }
                    visit.instructions?.takeIf(String::isNotBlank)?.let { Text("Instruções: $it", style = MaterialTheme.typography.bodySmall) }
                    visit.noVisitReason?.let { Text("Motivo: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    visit.completedVidas?.let { Text("Vidas: $it", style = MaterialTheme.typography.bodySmall) }
                    val address = addressOf(visit.cliente?.endereco, visit.cliente?.complemento, visit.cliente?.bairro, visit.cliente?.cidade, visit.cliente?.uf)
                    if (address.isNotBlank()) TextButton(onClick = { openMap(context, address) }) { Icon(Icons.Rounded.Place, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Abrir navegação") }
                    if (!registered && profile?.userRole != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { complete = visit }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.CheckCircle, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Concluir") }
                            OutlinedButton(onClick = { noVisit = visit }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Block, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Não realizada") }
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
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(visit.cliente?.empresa ?: "Empresa"); visit.perfilVisita?.let { Text("Perfil: $it", style = MaterialTheme.typography.bodySmall) }; OutlinedTextField(value = vidas, onValueChange = { if (it.all(Char::isDigit)) vidas = it }, label = { Text("Quantidade de vidas") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) } },
            confirmButton = { Button(onClick = { onComplete(visit.id, vidas.toIntOrNull() ?: 0); complete = null }, enabled = vidas.isNotBlank()) { Text("Registrar") } },
            dismissButton = { TextButton(onClick = { complete = null }) { Text("Cancelar") } },
        )
    }
    noVisit?.let { visit ->
        val reasons = listOf("NAO AUTORIZADO", "NAO CHEGOU A TEMPO", "ENDERECO NAO LOCALIZADO", "AUSENTE NO DIA")
        var reason by rememberSaveable(visit.id) { mutableStateOf("") }
        var observation by rememberSaveable(visit.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { noVisit = null },
            title = { Text("Visita não realizada") },
            text = { Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(visit.cliente?.empresa ?: "Empresa"); reasons.forEach { item -> FilterChip(selected = reason == item, onClick = { reason = item }, label = { Text(item) }) }; OutlinedTextField(value = observation, onValueChange = { observation = it }, label = { Text("Observação") }, minLines = 3, modifier = Modifier.fillMaxWidth()) } },
            confirmButton = { Button(onClick = { onNoVisit(visit.id, reason, observation.trim().ifBlank { null }); noVisit = null }, enabled = reason.isNotBlank()) { Text("Registrar") } },
            dismissButton = { TextButton(onClick = { noVisit = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
internal fun ExactClientsScreen(
    state: ParityUiState,
    session: UserSession,
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
    val integrations = remember { WebIntegrationApi() }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { WebPageHeader("Empresas", "Cadastro, pesquisa e informações comerciais.", action = { IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, contentDescription = "Cadastrar") } }) }
        state.errorMessage?.let { item { WebError(it) } }
        item { OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Buscar empresa") }, leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }, singleLine = true) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("codigo" to "Código", "empresa" to "Empresa", "geral" to "Geral").forEach { (value, label) -> FilterChip(selected = mode == value, onClick = { mode = value }, label = { Text(label) }) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("" to "Todas", "Ativo" to "Ativo", "Suspenso/Inadimplente" to "Suspenso", "Cancelado" to "Cancelado").forEach { (value, label) -> FilterChip(selected = situacao == value, onClick = { situacao = value }, label = { Text(label) }) }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onLoad(search, mode, situacao.ifBlank { null }, 1) }) { Text("Aplicar filtros") }
                Spacer(Modifier.width(12.dp))
                Text("${state.clientsTotal} registros", style = MaterialTheme.typography.labelLarge)
            }
        }
        items(state.clients, key = { it.id }) { company ->
            Card(onClick = { selected = company }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.15f))) {
                Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(company.empresa ?: company.nomeFantasia ?: "Empresa", fontWeight = FontWeight.SemiBold)
                            Text(listOfNotNull(company.codigo, company.cnpj).joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        company.situacao?.let { WebStatus(it) }
                    }
                    val address = addressOf(company.endereco, company.complemento, company.bairro, company.cidade, company.uf)
                    if (address.isNotBlank()) Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { company.vidasQtde?.let { Text("Vidas: $it", style = MaterialTheme.typography.labelSmall) }; company.categoria?.let { Text(it, style = MaterialTheme.typography.labelSmall) } }
                }
            }
        }
        item { ExactPagination(page = state.clientsPage, total = state.clientsTotal, pageSize = state.clientsPageSize, onPage = { onLoad(search, mode, situacao.ifBlank { null }, it) }) }
    }

    selected?.let { company ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(company.empresa ?: company.nomeFantasia ?: "Empresa") },
            text = { Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) { ExactDetail("Código", company.codigo); ExactDetail("CNPJ", company.cnpj); ExactDetail("Vidas", company.vidasQtde?.toString()); ExactDetail("Corte", company.corte?.toString()); ExactDetail("Vencimento", company.venc?.toString()); ExactDetail("Valor", company.valor?.let(::currency)); ExactDetail("Reajuste", company.reajustePct?.let { "$it%" }); ExactDetail("Competência", company.competencia); ExactDetail("Última visita", company.dataUltimaVisita?.let(::formatDateBrExact)); ExactDetail("Pessoa", company.pessoa); ExactDetail("Contato", company.contato); ExactDetail("Grupo", company.grupo); ExactDetail("Situação", company.situacao); ExactDetail("Categoria", company.categoria); ExactDetail("Perfil de visita", company.perfilVisita); ExactDetail("Regra de visita", company.regraVisitaObservacao); ExactDetail("Obs. comercial", company.obsComercial); ExactDetail("Observações", company.obs); ExactDetail("CEP", company.cep); ExactDetail("Endereço", addressOf(company.endereco, company.complemento, company.bairro, company.cidade, company.uf)) } },
            confirmButton = { TextButton(onClick = { editing = company; selected = null }) { Icon(Icons.Rounded.Edit, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Editar") } },
            dismissButton = { TextButton(onClick = { deleting = company; selected = null }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Excluir") } },
        )
    }
    if (creating) ExactCompanyForm(title = "Cadastrar empresa", initial = null, session = session, integrations = integrations, onDismiss = { creating = false }, onSave = { onCreate(it); creating = false })
    editing?.let { company -> ExactCompanyForm(title = "Editar empresa", initial = company, session = session, integrations = integrations, onDismiss = { editing = null }, onSave = { onUpdate(company.id, it); editing = null }) }
    deleting?.let { company -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Excluir empresa") }, text = { Text("Confirma a exclusão de ${company.empresa ?: company.codigo ?: "esta empresa"}?") }, confirmButton = { Button(onClick = { onDelete(company.id); deleting = null }) { Text("Excluir") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }) }
}

@Composable
private fun ExactCompanyForm(title: String, initial: ClienteListItem?, session: UserSession, integrations: WebIntegrationApi, onDismiss: () -> Unit, onSave: (JSONObject) -> Unit) {
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
                OutlinedButton(onClick = { scope.launch { lookupLoading = true; lookupMessage = null; runCatching { integrations.fetchOdontoartEmpresa(session, codigo) }.onSuccess { row -> if (row == null) lookupMessage = "Empresa não encontrada no ERP." else { empresa = row.readStringExact("RazaoSocial", "Empresa", "Nome") ?: empresa; cnpj = row.readStringExact("Cnpj", "CNPJ") ?: cnpj; valor = row.readNumberExact("ValorTitular")?.toString() ?: valor; lookupMessage = "Dados do ERP carregados." } }.onFailure { lookupMessage = it.message }; lookupLoading = false } }, enabled = codigo.isNotBlank() && !lookupLoading) { Text("Consultar código") }
                OutlinedTextField(value = cnpj, onValueChange = { cnpj = it }, label = { Text("CNPJ") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedButton(onClick = { scope.launch { lookupLoading = true; lookupMessage = null; runCatching { integrations.fetchCnpj(cnpj) }.onSuccess { result -> empresa = result.razaoSocial ?: empresa; cep = result.cep ?: cep; endereco = listOfNotNull(result.logradouro, result.numero).joinToString(", ").ifBlank { endereco }; bairro = result.bairro ?: bairro; cidade = result.cidade ?: cidade; uf = result.uf ?: uf; lookupMessage = "Dados públicos do CNPJ carregados." }.onFailure { lookupMessage = it.message }; lookupLoading = false } }, enabled = cnpj.isNotBlank() && !lookupLoading) { Text("Consultar CNPJ") }
                OutlinedTextField(value = empresa, onValueChange = { empresa = it }, label = { Text("Empresa") }, modifier = Modifier.fillMaxWidth())
                Text("Contrato e comercial", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = vidas, onValueChange = { vidas = it }, label = { Text("Vidas") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)); OutlinedTextField(value = valor, onValueChange = { valor = it }, label = { Text("Valor") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = corte, onValueChange = { corte = it }, label = { Text("Corte") }, modifier = Modifier.weight(1f)); OutlinedTextField(value = venc, onValueChange = { venc = it }, label = { Text("Venc.") }, modifier = Modifier.weight(1f)) }
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
                OutlinedButton(onClick = { scope.launch { lookupLoading = true; lookupMessage = null; runCatching { integrations.fetchCep(cep) }.onSuccess { result -> cep = result.cep ?: cep; endereco = result.endereco ?: endereco; if (!result.complemento.isNullOrBlank()) complemento = result.complemento; bairro = result.bairro ?: bairro; cidade = result.cidade ?: cidade; uf = result.uf ?: uf; lookupMessage = "CEP localizado." }.onFailure { lookupMessage = it.message }; lookupLoading = false } }, enabled = cep.isNotBlank() && !lookupLoading) { Text("Buscar CEP") }
                OutlinedTextField(value = endereco, onValueChange = { endereco = it }, label = { Text("Endereço") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = complemento, onValueChange = { complemento = it }, label = { Text("Complemento") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bairro, onValueChange = { bairro = it }, label = { Text("Bairro") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = cidade, onValueChange = { cidade = it }, label = { Text("Cidade") }, modifier = Modifier.weight(1f)); OutlinedTextField(value = uf, onValueChange = { uf = it.uppercase().take(2) }, label = { Text("UF") }, modifier = Modifier.width(86.dp)) }
                lookupMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = { Button(onClick = { val payload = JSONObject(); payload.putNullableExact("codigo", codigo); payload.putNullableExact("cnpj", cnpj); payload.putNullableExact("empresa", empresa); payload.putNumberExact("vidas_qtde", vidas); payload.putNumberExact("corte", corte); payload.putNumberExact("venc", venc); payload.putNumberExact("valor", valor); payload.putNumberExact("reajuste_pct", reajuste); payload.putNullableExact("competencia", competencia); payload.putNullableExact("data_da_ultima_visita", dataUltima); payload.putNullableExact("pessoa", pessoa); payload.putNullableExact("contato", contato); payload.putNullableExact("grupo", grupo); payload.putNullableExact("situacao", situacao.ifBlank { "Ativo" }); payload.putNullableExact("categoria", categoria); payload.putNullableExact("perfil_visita", perfil); payload.putNullableExact("regra_visita_observacao", regra); payload.putNullableExact("obs_comercial", obsComercial); payload.putNullableExact("obs", obs); payload.putNullableExact("cep", cep); payload.putNullableExact("endereco", endereco); payload.putNullableExact("complemento", complemento); payload.putNullableExact("bairro", bairro); payload.putNullableExact("cidade", cidade); payload.putNullableExact("uf", uf); onSave(payload) }, enabled = empresa.isNotBlank() || codigo.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ExactPagination(page: Int, total: Long, pageSize: Int, onPage: (Int) -> Unit) {
    val pages = maxOf(1, ((total + pageSize - 1) / pageSize).toInt())
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = { onPage((page - 1).coerceAtLeast(1)) }, enabled = page > 1) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Anterior") }
        Text("Página $page de $pages", modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = { onPage((page + 1).coerceAtMost(pages)) }, enabled = page < pages) { Icon(Icons.Rounded.ChevronRight, contentDescription = "Próxima") }
    }
}

@Composable
private fun ExactDetail(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyMedium) }
}

private fun addressOf(vararg parts: String?): String = parts.filterNotNull().map(String::trim).filter(String::isNotBlank).joinToString(", ")
private fun openMap(context: android.content.Context, address: String) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode("$address, Brasil")}"))) } }
private fun formatDateBrExact(value: String?): String { if (value.isNullOrBlank()) return "-"; val raw = value.take(10); val p = raw.split("-"); return if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else raw }
private fun currency(value: Double): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
private fun JSONObject.putNullableExact(key: String, value: String) { put(key, value.trim().takeIf(String::isNotBlank) ?: JSONObject.NULL) }
private fun JSONObject.putNumberExact(key: String, value: String) { put(key, value.trim().replace(",", ".").toDoubleOrNull() ?: JSONObject.NULL) }
private fun JSONObject.readStringExact(vararg targets: String): String? { targets.forEach { target -> val iterator = keys(); while (iterator.hasNext()) { val key = iterator.next(); if (key.equals(target, ignoreCase = true) && !isNull(key)) { val v = optString(key).trim(); if (v.isNotBlank()) return v } } }; return null }
private fun JSONObject.readNumberExact(vararg targets: String): Double? { targets.forEach { target -> val iterator = keys(); while (iterator.hasNext()) { val key = iterator.next(); if (key.equals(target, ignoreCase = true) && !isNull(key)) return runCatching { getDouble(key) }.getOrNull() ?: optString(key).replace(",", ".").toDoubleOrNull() } }; return null }
