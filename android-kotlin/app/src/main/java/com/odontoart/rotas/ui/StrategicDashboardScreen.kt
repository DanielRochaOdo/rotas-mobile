package com.odontoart.rotas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odontoart.rotas.UserRole
import com.odontoart.rotas.UserSession
import com.odontoart.rotas.dashboard.DashboardComputedModel
import com.odontoart.rotas.dashboard.DashboardMetricSet
import com.odontoart.rotas.dashboard.DashboardSellerSummary
import com.odontoart.rotas.dashboard.ExactDashboardUiState
import com.odontoart.rotas.dashboard.ExactDashboardViewModel
import com.odontoart.rotas.ui.theme.RotasSea
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private enum class DashboardTab(val label: String) {
    VISAO("Geral"), PERFORMANCE("Performance"), COMERCIAL("Comercial"), COBERTURA("Cobertura"), QUALIDADE("Qualidade")
}

private enum class DashboardKpi(val label: String) {
    VISITAS("Visitas totais"), CONCLUIDAS("Concluídas"), PENDENTES("Pendentes"), EMPRESAS("Empresas visitadas (ÚNICAS)"), COBERTURA("Cobertura"), VIDAS("Vidas totais"), EXECUCAO("Taxa de execução")
}

@Composable
internal fun StrategicDashboardScreen(
    session: UserSession,
    role: UserRole?,
    viewModel: ExactDashboardViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isVendor = role == UserRole.VENDEDOR
    val visibleTabs = if (isVendor) listOf(DashboardTab.VISAO) else DashboardTab.entries
    var tabName by rememberSaveable { mutableStateOf(DashboardTab.VISAO.name) }
    val tab = DashboardTab.valueOf(tabName)
    var draftFrom by rememberSaveable { mutableStateOf(state.from) }
    var draftTo by rememberSaveable { mutableStateOf(state.to) }
    var draftSupervisor by rememberSaveable { mutableStateOf(state.selectedSupervisorId) }
    var draftSeller by rememberSaveable { mutableStateOf(state.selectedSellerName) }
    var supervisorDialog by rememberSaveable { mutableStateOf(false) }
    var sellerDialog by rememberSaveable { mutableStateOf(false) }
    var selectedKpi by remember { mutableStateOf<DashboardKpi?>(null) }

    LaunchedEffect(session.userId, role) { viewModel.load(session, role) }
    LaunchedEffect(state.from, state.to, state.selectedSupervisorId, state.selectedSellerName) {
        draftFrom = state.from
        draftTo = state.to
        draftSupervisor = state.selectedSupervisorId
        draftSeller = state.selectedSellerName
    }
    LaunchedEffect(isVendor) { if (isVendor) tabName = DashboardTab.VISAO.name }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { DashboardHeader { viewModel.refresh(session, role) } }
        item {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                visibleTabs.forEach { item -> FilterChip(selected = item == tab, onClick = { tabName = item.name }, label = { Text(item.label) }) }
                Surface(shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = .20f))) {
                    Text("Dados combinados", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (!isVendor && tab != DashboardTab.VISAO) {
            item {
                DashboardFilters(
                    state = state,
                    from = draftFrom,
                    to = draftTo,
                    supervisor = draftSupervisor,
                    seller = draftSeller,
                    onFrom = { draftFrom = it },
                    onTo = { draftTo = it },
                    onSupervisor = { supervisorDialog = true },
                    onSeller = { sellerDialog = true },
                    onApply = { viewModel.applyFilters(session, role, draftFrom, draftTo, draftSupervisor, draftSeller) },
                    onClear = { viewModel.clearFilters(session, role) },
                )
            }
        }
        state.errorMessage?.let { item { DashboardError(it) } }
        when (tab) {
            DashboardTab.VISAO -> overviewItems(state) { selectedKpi = it }
            DashboardTab.PERFORMANCE -> performanceItems(state)
            DashboardTab.COMERCIAL -> commercialItems(state)
            DashboardTab.COBERTURA -> coverageItems(state)
            DashboardTab.QUALIDADE -> qualityItems(state)
        }
    }

    if (supervisorDialog) {
        OptionDialog(
            title = "Supervisor",
            options = listOf("all" to "Todos") + state.model.supervisors.map { it.id to it.name },
            selected = draftSupervisor,
            onSelect = { draftSupervisor = it; draftSeller = "all"; supervisorDialog = false },
            onDismiss = { supervisorDialog = false },
        )
    }
    if (sellerDialog) {
        val vendors = state.model.vendors.filter { draftSupervisor == "all" || it.supervisorId == draftSupervisor }
        OptionDialog(
            title = "Vendedor",
            options = listOf("all" to "Todos") + vendors.map { it.name to it.name },
            selected = draftSeller,
            onSelect = { draftSeller = it; sellerDialog = false },
            onDismiss = { sellerDialog = false },
        )
    }
    selectedKpi?.let { KpiDialog(it, state.model) { selectedKpi = null } }
}

@Composable
private fun DashboardHeader(onRefresh: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = .15f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                Text("Análise robusta com cruzamento de visitas, vidas registradas, aceite digital e cobertura comercial.", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar") }
        }
    }
}

@Composable
private fun DashboardFilters(
    state: ExactDashboardUiState,
    from: String,
    to: String,
    supervisor: String,
    seller: String,
    onFrom: (String) -> Unit,
    onTo: (String) -> Unit,
    onSupervisor: () -> Unit,
    onSeller: () -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
) {
    val supervisorLabel = state.model.supervisors.firstOrNull { it.id == supervisor }?.name ?: "Todos"
    val sellerLabel = if (seller == "all") "Todos" else seller
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = .15f))) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(from, onFrom, Modifier.weight(1f), label = { Text("De") }, singleLine = true)
                OutlinedTextField(to, onTo, Modifier.weight(1f), label = { Text("Até") }, singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onSupervisor, Modifier.weight(1f)) { Text("Supervisor: $supervisorLabel", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                OutlinedButton(onSeller, Modifier.weight(1f)) { Text("Vendedor: $sellerLabel", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onApply, Modifier.weight(1f)) { Text("Aplicar") }
                OutlinedButton(onClear, Modifier.weight(1f)) { Text("Limpar") }
            }
        }
    }
}

@Composable
private fun DashboardError(message: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

private fun LazyListScope.overviewItems(state: ExactDashboardUiState, onKpi: (DashboardKpi) -> Unit) {
    item { KpiGrid(state.model.metrics, onKpi) }
    item {
        SectionCard("Leitura rápida") {
            val m = state.model.metrics
            Text("${number(m.performedVisits)} realizadas, ${number(m.notPerformedVisits)} não realizadas e ${number(m.pendingVisits)} pendentes.")
            Text("Vidas: ${number(m.livesVisits)} em visitas + ${number(m.livesAcceptance)} em aceite digital = ${number(m.livesTotal)}.")
        }
    }
    if (state.model.daily.isNotEmpty()) item {
        SectionCard("Vidas por dia") {
            BarChart(state.model.daily.map { it.lives })
            DateLegend(state.model.daily.map { it.date })
        }
    }
    if (state.model.sellers.isNotEmpty()) item { SectionCard("Vidas por vendedor") { DonutChart(state.model.sellers) } }
}

private fun LazyListScope.performanceItems(state: ExactDashboardUiState) {
    val m = state.model.metrics
    val p = state.model.previousMetrics
    item {
        SectionCard("Tendência temporal") {
            BarChart(state.model.daily.map { it.totalForPerformance() })
            DateLegend(state.model.daily.map { it.date })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniMetric("Total atual", number(m.totalVisits), Modifier.weight(1f))
                MiniMetric("Anterior", number(p.totalVisits), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val average = if (state.model.daily.isEmpty()) 0.0 else m.totalVisits.toDouble() / state.model.daily.size
                MiniMetric("Média/dia", String.format(Locale("pt", "BR"), "%.1f", average), Modifier.weight(1f))
                MiniMetric("Variação", variation(m.totalVisits, p.totalVisits)?.let { String.format(Locale("pt", "BR"), "%+.1f%%", it) } ?: "-", Modifier.weight(1f))
            }
        }
    }
    item {
        SectionCard("Execução") {
            Progress("Concluídas", m.concludedVisits, m.totalVisits)
            Progress("Realizadas", m.performedVisits, m.totalVisits)
            Progress("Pendentes", m.pendingVisits, m.totalVisits)
            Text("Taxa de execução: ${percent(m.executionRate)}", fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun LazyListScope.commercialItems(state: ExactDashboardUiState) {
    item { SectionCard("Performance comercial") { Text("Vidas combinadas por vendedor (visitas + aceite digital).", color = MaterialTheme.colorScheme.onSurfaceVariant); DonutChart(state.model.sellers) } }
    items(state.model.sellers, key = { it.seller }) { SellerCard(it) }
}

private fun LazyListScope.coverageItems(state: ExactDashboardUiState) {
    val m = state.model.metrics
    item { SectionCard("Cobertura comercial") { Text("${number(m.visitedCompanies)} de ${number(m.totalClientsInScope)} empresas aparecem no recorte."); Text(percent(m.coverage), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium) } }
    item { RankingCard("Mais visitadas", state.model.mostVisitedCompanies.map { it.name to it.count.toString() }) }
    item { RankingCard("Menos visitadas", state.model.leastVisitedCompanies.map { it.name to it.count.toString() }) }
    item { RankingCard("Nunca visitadas", state.model.neverVisitedCompanies.map { it.name to "0" }) }
    if (state.model.cities.isNotEmpty()) item { RankingCard("Visitas por cidade", state.model.cities.map { it.city to it.count.toString() }) }
}

private fun LazyListScope.qualityItems(state: ExactDashboardUiState) {
    val m = state.model.metrics
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Quality("Sem empresa", m.missingClient, Modifier.weight(1f))
            Quality("Sem responsável", m.missingResponsible, Modifier.weight(1f))
            Quality("Sem data", m.missingDate, Modifier.weight(1f))
        }
    }
    if (state.model.reasons.isNotEmpty()) item { RankingCard("Motivos de não visita", state.model.reasons.map { it.reason to it.count.toString() }) }
    item {
        SectionCard("Qualidade do cadastro") {
            val issues = m.missingClient + m.missingResponsible + m.missingDate
            Icon(Icons.Rounded.WarningAmber, null, tint = if (issues > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(if (issues == 0) "Nenhuma inconsistência básica no período." else "$issues inconsistências básicas encontradas.", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun KpiGrid(m: DashboardMetricSet, onKpi: (DashboardKpi) -> Unit) {
    val values = listOf(
        DashboardKpi.VISITAS to number(m.totalVisits), DashboardKpi.CONCLUIDAS to number(m.concludedVisits), DashboardKpi.PENDENTES to number(m.pendingVisits), DashboardKpi.EMPRESAS to number(m.visitedCompanies), DashboardKpi.COBERTURA to percent(m.coverage), DashboardKpi.VIDAS to number(m.livesTotal), DashboardKpi.EXECUCAO to percent(m.executionRate),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (kpi, value) ->
                    Card(onClick = { onKpi(kpi) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = .15f))) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Text(kpi.label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall); Icon(Icons.Rounded.TrendingUp, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = .15f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable private fun MiniMetric(label: String, value: String, modifier: Modifier) { Surface(modifier, RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)) { Column(Modifier.padding(10.dp)) { Text(label, style = MaterialTheme.typography.labelSmall); Text(value, fontWeight = FontWeight.SemiBold) } } }
@Composable private fun Quality(label: String, value: Int, modifier: Modifier) { Card(modifier, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = .15f))) { Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value.toString(), style = MaterialTheme.typography.headlineSmall); Text(label, style = MaterialTheme.typography.labelSmall) } } }

@Composable
private fun Progress(label: String, value: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row { Text(label, Modifier.weight(1f)); Text("$value / ${total.coerceAtLeast(0)}", fontWeight = FontWeight.SemiBold) }
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            drawRoundRect(Color.LightGray.copy(alpha = .35f), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            val fraction = value.toFloat() / total.coerceAtLeast(1)
            drawRoundRect(RotasSea, size = Size(size.width * fraction.coerceIn(0f, 1f), size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        }
    }
}

@Composable
private fun BarChart(values: List<Int>) {
    if (values.isEmpty()) { Text("Sem dados no período."); return }
    val maxValue = max(1, values.maxOrNull() ?: 1)
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f)
    Canvas(Modifier.fillMaxWidth().height(190.dp)) {
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (values.size - 1)) / values.size.coerceAtLeast(1)).coerceAtLeast(2.dp.toPx())
        values.forEachIndexed { index, value ->
            val h = size.height * value.toFloat() / maxValue
            drawRoundRect(if (value > 0) RotasSea else Color.Gray.copy(alpha = .25f), Offset(index * (barWidth + gap), size.height - h), Size(barWidth, max(2.dp.toPx(), h)), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
        }
        val avg = values.average().toFloat()
        val y = size.height - size.height * (avg / maxValue)
        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
    }
}

@Composable
private fun DateLegend(dates: List<String>) {
    if (dates.isEmpty()) return
    val fmt = DateTimeFormatter.ofPattern("dd/MM")
    fun label(value: String) = runCatching { LocalDate.parse(value).format(fmt) }.getOrDefault(value)
    Row { Text(label(dates.first()), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall); Text(label(dates.last()), style = MaterialTheme.typography.labelSmall) }
}

@Composable
private fun DonutChart(sellers: List<DashboardSellerSummary>) {
    val data = sellers.filter { it.livesTotal > 0 }.take(10)
    if (data.isEmpty()) { Text("Sem vidas registradas no recorte."); return }
    val total = data.sumOf { it.livesTotal }.coerceAtLeast(1)
    val colors = listOf(RotasSea, Color(0xFF1F7A5A), Color(0xFF38BDF8), Color(0xFFF59E0B), Color(0xFF0F766E), Color(0xFF94A3B8), Color(0xFF22C55E))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(145.dp)) {
            var start = -90f
            data.forEachIndexed { index, seller ->
                val sweep = seller.livesTotal * 360f / total
                drawArc(colors[index % colors.size], start, sweep, false, style = Stroke(24.dp.toPx(), cap = StrokeCap.Butt))
                start += sweep
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            data.take(6).forEachIndexed { index, seller ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(9.dp)) { drawCircle(colors[index % colors.size]) }
                    Spacer(Modifier.width(6.dp))
                    Text(seller.seller, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                    Text(number(seller.livesTotal), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SellerCard(seller: DashboardSellerSummary) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = .15f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(seller.seller, fontWeight = FontWeight.SemiBold)
            Text("Visitas ${seller.visits} · Empresas ${seller.companies} · Vidas ${seller.livesTotal}", style = MaterialTheme.typography.bodySmall)
            Text("Visitas: ${seller.livesVisit} vidas · Aceite: ${seller.livesAcceptance} vidas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RankingCard(title: String, rows: List<Pair<String, String>>) {
    SectionCard(title) {
        if (rows.isEmpty()) Text("Sem dados.")
        rows.take(10).forEachIndexed { index, row -> Row { Text("${index + 1}. ${row.first}", Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall); Text(row.second, fontWeight = FontWeight.SemiBold) } }
    }
}

@Composable
private fun OptionDialog(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { LazyColumn(Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { items(options, key = { it.first }) { option -> FilterChip(option.first == selected, { onSelect(option.first) }, label = { Text(option.second) }) } } }, confirmButton = { TextButton(onDismiss) { Text("Fechar") } })
}

@Composable
private fun KpiDialog(kpi: DashboardKpi, model: DashboardComputedModel, onDismiss: () -> Unit) {
    val m = model.metrics
    val p = model.previousMetrics
    fun current(): Double = when (kpi) { DashboardKpi.VISITAS -> m.totalVisits.toDouble(); DashboardKpi.CONCLUIDAS -> m.concludedVisits.toDouble(); DashboardKpi.PENDENTES -> m.pendingVisits.toDouble(); DashboardKpi.EMPRESAS -> m.visitedCompanies.toDouble(); DashboardKpi.COBERTURA -> m.coverage; DashboardKpi.VIDAS -> m.livesTotal.toDouble(); DashboardKpi.EXECUCAO -> m.executionRate }
    fun previous(): Double = when (kpi) { DashboardKpi.VISITAS -> p.totalVisits.toDouble(); DashboardKpi.CONCLUIDAS -> p.concludedVisits.toDouble(); DashboardKpi.PENDENTES -> p.pendingVisits.toDouble(); DashboardKpi.EMPRESAS -> p.visitedCompanies.toDouble(); DashboardKpi.COBERTURA -> p.coverage; DashboardKpi.VIDAS -> p.livesTotal.toDouble(); DashboardKpi.EXECUCAO -> p.executionRate }
    val displayed = if (kpi == DashboardKpi.COBERTURA || kpi == DashboardKpi.EXECUCAO) percent(current()) else number(current().toInt())
    AlertDialog(onDismissRequest = onDismiss, title = { Text(kpi.label) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(displayed, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary); Text("Período anterior: ${if (kpi == DashboardKpi.COBERTURA || kpi == DashboardKpi.EXECUCAO) percent(previous()) else number(previous().toInt())}"); if (previous() != 0.0) Text("Variação: ${String.format(Locale("pt", "BR"), "%+.1f%%", (current() - previous()) * 100.0 / abs(previous()))}"); if (kpi == DashboardKpi.VIDAS) Text("${m.livesVisits} vidas em visitas + ${m.livesAcceptance} em aceite digital.") } }, confirmButton = { TextButton(onDismiss) { Text("Fechar") } })
}

private fun com.odontoart.rotas.dashboard.DashboardDailyPoint.totalForPerformance(): Int = visits
private fun number(value: Int): String = NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(value)
private fun percent(value: Double): String = String.format(Locale("pt", "BR"), "%.1f%%", value)
private fun variation(current: Int, previous: Int): Double? = if (previous == 0) if (current == 0) 0.0 else null else (current - previous) * 100.0 / abs(previous)
