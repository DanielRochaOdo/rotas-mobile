package com.odontoart.rotas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odontoart.rotas.AuditLogItem
import com.odontoart.rotas.ParityUiState
import com.odontoart.rotas.QueueControlItem

@Composable
internal fun QueueParityScreen(
    state: ParityUiState,
    onRefresh: () -> Unit,
    onAction: (String, String, Int?, Int?, String?) -> Unit,
) {
    var stateFilter by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<QueueControlItem?>(null) }
    val filtered = remember(state.queue, stateFilter) {
        if (stateFilter.isBlank()) state.queue else state.queue.filter { it.effectiveState == stateFilter }
    }
    val pending = state.queue.count { it.effectiveState == "PENDING_WAIT" }
    val releasePending = state.queue.count { it.effectiveState == "RELEASE_PENDING" || it.effectiveState == "READY_AUTO" }
    val manual = state.queue.count { it.effectiveState == "RELEASED_MANUAL" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ParitySectionHeader(
                "Fila",
                "Controle de carência e liberação de novas empresas.",
                trailing = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar") } },
            )
        }
        state.errorMessage?.let { item { ParityErrorCard(it) } }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QueueMetric("Aguardando", pending, Modifier.weight(1f))
                QueueMetric("Liberação", releasePending, Modifier.weight(1f))
                QueueMetric("Manual", manual, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = stateFilter.isBlank(), onClick = { stateFilter = "" }, label = { Text("Todos") })
                FilterChip(selected = stateFilter == "PENDING_WAIT", onClick = { stateFilter = "PENDING_WAIT" }, label = { Text("Aguardando") })
                FilterChip(selected = stateFilter == "BLOCKED_MANUAL", onClick = { stateFilter = "BLOCKED_MANUAL" }, label = { Text("Bloqueados") })
            }
        }
        if (filtered.isEmpty() && !state.isLoading) item { ParityInfoCard("Nenhuma empresa encontrada na fila.") }
        items(filtered, key = { it.empresaId }) { row ->
            Card(onClick = { selected = row }, shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.empresa ?: "Empresa", fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(row.codigo, row.cnpj).joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ParityStatusPill(queueStateLabel(row.effectiveState))
                    }
                    Text("1º pagamento: ${formatDateBr(row.dataContrato)}", style = MaterialTheme.typography.bodySmall)
                    Text("Liberação prevista: ${formatDateTimeBr(row.eligibleAt)}", style = MaterialTheme.typography.bodySmall)
                    row.daysRemaining?.let { Text("Dias restantes: $it", style = MaterialTheme.typography.bodySmall) }
                    row.manualReason?.let { Text("Motivo manual: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }

    selected?.let { row ->
        QueueActionDialog(
            row = row,
            onDismiss = { selected = null },
            onAction = { action, waiting, block, reason ->
                onAction(row.empresaId, action, waiting, block, reason)
                selected = null
            },
        )
    }
}

@Composable
private fun QueueMetric(label: String, value: Int, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun queueStateLabel(state: String): String = when (state) {
    "PENDING_WAIT" -> "Aguardando prazo"
    "RELEASE_PENDING" -> "Liberação pendente"
    "READY_AUTO" -> "Liberada automática"
    "RELEASED_MANUAL" -> "Liberada manual"
    "BLOCKED_MANUAL" -> "Bloqueada manual"
    else -> state
}

@Composable
private fun QueueActionDialog(
    row: QueueControlItem,
    onDismiss: () -> Unit,
    onAction: (String, Int?, Int?, String?) -> Unit,
) {
    var waitingDays by rememberSaveable { mutableStateOf(row.waitingDaysSnapshot?.toString().orEmpty()) }
    var blockDays by rememberSaveable { mutableStateOf("7") }
    var reason by rememberSaveable { mutableStateOf(row.manualReason.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.empresa ?: "Empresa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Estado: ${queueStateLabel(row.effectiveState)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    waitingDays,
                    { if (it.all(Char::isDigit)) waitingDays = it },
                    label = { Text("Prazo em dias") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    blockDays,
                    { if (it.all(Char::isDigit)) blockDays = it },
                    label = { Text("Bloquear por dias") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(reason, { reason = it }, label = { Text("Motivo") }, minLines = 2)
                Button(onClick = { onAction("RELEASE_NOW", null, null, reason.trim().ifBlank { null }) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Liberar agora")
                }
                OutlinedButton(
                    onClick = { onAction("SET_WAITING_DAYS", waitingDays.toIntOrNull(), null, reason.trim().ifBlank { null }) },
                    enabled = waitingDays.toIntOrNull() != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Alterar prazo") }
                OutlinedButton(
                    onClick = { onAction("BLOCK_DAYS", null, blockDays.toIntOrNull(), reason.trim().ifBlank { null }) },
                    enabled = blockDays.toIntOrNull() != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Bloquear")
                }
                if (row.effectiveState == "BLOCKED_MANUAL") {
                    OutlinedButton(onClick = { onAction("UNBLOCK", null, null, reason.trim().ifBlank { null }) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.LockOpen, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Desbloquear")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
internal fun KpiParityScreen(state: ParityUiState, onPeriod: (Int) -> Unit) {
    var period by rememberSaveable { mutableStateOf(30) }
    val sales = state.kpiSnapshots.sumOf { it.vendasQtde }
    val cancellations = state.kpiSnapshots.sumOf { it.cancelamentosQtde }
    val delta = state.kpiSnapshots.sumOf { it.delta }
    val lives = state.kpiSnapshots.sumOf { it.vidasQtde ?: 0 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ParitySectionHeader("KPI", "Acompanhamento de vidas, vendas e cancelamentos por período.") }
        state.errorMessage?.let { item { ParityErrorCard(it) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 7, 15, 30).forEach { days ->
                    FilterChip(
                        selected = period == days,
                        onClick = { period = days; onPeriod(days) },
                        label = { Text("$days d") },
                    )
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMetric("Vendas", sales, Modifier.weight(1f))
                KpiMetric("Cancelamentos", cancellations, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMetric("Saldo", delta, Modifier.weight(1f))
                KpiMetric("Vidas", lives, Modifier.weight(1f))
            }
        }
        if (state.kpiSnapshots.isEmpty() && !state.isLoading) item { ParityInfoCard("Nenhum snapshot de KPI encontrado para este período.") }
        items(state.kpiSnapshots, key = { it.id }) { row ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.empresa ?: row.codigo, fontWeight = FontWeight.SemiBold)
                            Text("Código ${row.codigo} • ${row.categoria ?: "Sem categoria"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        row.status?.let { ParityStatusPill(it) }
                    }
                    Text("Vidas: ${row.vidasQtde ?: 0} | Vendas: ${row.vendasQtde} | Cancelamentos: ${row.cancelamentosQtde} | Saldo: ${row.delta}", style = MaterialTheme.typography.bodySmall)
                    Text("Snapshot: ${formatDateTimeBr(row.snapshotAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun KpiMetric(label: String, value: Int, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun NewsParityScreen(
    state: ParityUiState,
    onRefresh: () -> Unit,
    onMarkRead: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ParitySectionHeader(
                "Novidades",
                "Melhorias, atualizações e avisos do Sistema de Rotas.",
                trailing = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar") } },
            )
        }
        state.errorMessage?.let { item { ParityErrorCard(it) } }
        if (state.news.isEmpty() && !state.isLoading) item { ParityInfoCard("Nenhuma novidade publicada até o momento.") }
        items(state.news, key = { it.id }) { news ->
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ParityStatusPill(news.tipo)
                        ParityStatusPill(news.modulo)
                    }
                    Text(news.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stripBasicHtml(news.descricao), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatDateBr(news.dataPublicacao), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { onMarkRead(news.id) }, modifier = Modifier.fillMaxWidth()) { Text("Marcar como lida") }
                }
            }
        }
    }
}

@Composable
internal fun SettingsParityScreen(
    state: ParityUiState,
    onRefresh: () -> Unit,
    onSetInactive: (String, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ParitySectionHeader(
                "Configurações",
                "Usuários, perfis, vínculos e permissões do sistema.",
                trailing = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar") } },
            )
        }
        state.errorMessage?.let { item { ParityErrorCard(it) } }
        if (state.managedProfiles.isEmpty() && !state.isLoading) item { ParityInfoCard("Nenhum perfil encontrado.") }
        items(state.managedProfiles, key = { it.id }) { profile ->
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.nome ?: profile.displayName ?: "Usuário", fontWeight = FontWeight.SemiBold)
                            Text(profile.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ParityStatusPill(if (profile.isInactive) "Inativo" else "Ativo")
                    }
                    profile.supervisorId?.let { Text("Supervisor ID: $it", style = MaterialTheme.typography.bodySmall) }
                    profile.vendedorId?.let { Text("Vendedor ID: $it", style = MaterialTheme.typography.bodySmall) }
                    Text("Pré-cadastro: ${if (profile.canAccessPreCadastro) "Sim" else "Não"} • Próxima rota: ${if (profile.canAccessNextRouteDashboard) "Sim" else "Não"}", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = { onSetInactive(profile.id, !profile.isInactive) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (profile.isInactive) "Reativar usuário" else "Inativar usuário") }
                }
            }
        }
    }
}

@Composable
internal fun LogsParityScreen(
    state: ParityUiState,
    onFilter: (String?, String?) -> Unit,
) {
    var action by rememberSaveable { mutableStateOf("all") }
    var table by rememberSaveable { mutableStateOf("all") }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val availableTables = remember(state.logs) { state.logs.map { it.tableName }.distinct().sorted() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ParitySectionHeader("Logs", "Cadastros, alterações e exclusões realizados no sistema.") }
        state.errorMessage?.let { item { ParityErrorCard(it) } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("all" to "Todas", "INSERT" to "Cadastro", "UPDATE" to "Alteração", "DELETE" to "Exclusão").forEach { option ->
                        FilterChip(
                            selected = action == option.first,
                            onClick = { action = option.first; onFilter(action, table) },
                            label = { Text(option.second) },
                        )
                    }
                }
                if (availableTables.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(selected = table == "all", onClick = { table = "all"; onFilter(action, table) }, label = { Text("Todos módulos") })
                        availableTables.take(3).forEach { value ->
                            FilterChip(selected = table == value, onClick = { table = value; onFilter(action, table) }, label = { Text(logTableLabel(value)) })
                        }
                    }
                }
            }
        }
        if (state.logs.isEmpty() && !state.isLoading) item { ParityInfoCard("Nenhum registro encontrado.") }
        items(state.logs, key = { it.id }) { log ->
            AuditLogCard(log, expanded == log.id) { expanded = if (expanded == log.id) null else log.id }
        }
    }
}

@Composable
private fun AuditLogCard(log: AuditLogItem, expanded: Boolean, onToggle: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${logActionLabel(log.action)} · ${logTableLabel(log.tableName)}", fontWeight = FontWeight.SemiBold)
                    Text("${formatDateTimeBr(log.createdAt)} • ${log.userName ?: log.userId ?: "Sistema"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onToggle) {
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = "Detalhes")
                }
            }
            log.recordId?.let { Text("ID: $it", style = MaterialTheme.typography.labelSmall) }
            if (expanded) {
                log.oldData?.let { Text("Antes\n$it", style = MaterialTheme.typography.bodySmall) }
                log.newData?.let { Text("Depois\n$it", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun logActionLabel(value: String): String = when (value) {
    "INSERT" -> "Cadastro"
    "UPDATE" -> "Alteração"
    "DELETE" -> "Exclusão"
    else -> value
}

private fun logTableLabel(value: String): String = when (value) {
    "agenda" -> "Agenda"
    "visits" -> "Visitas"
    "routes" -> "Rotas"
    "route_stops" -> "Paradas"
    "clientes" -> "Empresas"
    "profiles" -> "Usuários"
    "aceite_digital" -> "Aceite digital"
    "agenda_headers_map" -> "Agenda (Headers)"
    else -> value
}
