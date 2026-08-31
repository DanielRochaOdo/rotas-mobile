package com.odontoart.rotas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
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
    VISAO("Geral"),
    PERFORMANCE("Performance"),
    COMERCIAL("Comercial"),
    COBERTURA("Cobertura"),
    QUALIDADE("Qualidade"),
}

private enum class DashboardKpi(val label: String) {
    VISITAS("Visitas totais"),
    CONCLUIDAS("Concluídas"),
    PENDENTES("Pendentes"),
    EMPRESAS("Empresas visitadas (ÚNICAS)"),
    COBERTURA("Cobertura"),
    VIDAS("Vidas totais"),
    EXECUCAO("Taxa de execução"),
}

@Composable
internal fun StrategicDashboardScreen(
    session: UserSession,
    role: UserRole?,
    viewModel: ExactDashboardViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isVendor = role == UserRole.VENDEDOR
    val tabs = if (isVendor) listOf(DashboardTab.VISAO) else DashboardTab.entries
    var tabName by rememberSaveable { mutableStateOf(DashboardTab.VISAO.name) }
    val tab = DashboardTab.valueOf(tabName)
    var draftFrom by rememberSaveable { mutableStateOf(state.from) }
    var draftTo by rememberSaveable { mutableStateOf(state.to) }
    var draftSupervisor by rememberSaveable { mutableStateOf(state.selectedSupervisorId) }
    var draftSeller by rememberSaveable { mutableStateOf(state.selectedSellerName) }
    var supervisorDialog by rememberSaveable { mutableStateOf(false) }
    var sellerDialog by rememberSaveable { mutableStateOf(false) }
    var selectedKpi by remember { mutableStateOf<DashboardKpi?>(null) }

    LaunchedEffect(session.userId, role) {
        viewModel.load(session, role)
    }
    LaunchedEffect(state.from, state.to, state.selectedSupervisorId, state.selectedSellerName) {
        draftFrom = state.from
        draftTo = state.to
        draftSupervisor = state.selectedSupervisorId
        draftSeller = state.selectedSellerName
    }
    LaunchedEffect(isVendor) {
        if (isVendor) tabName = DashboardTab.VISAO.name
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            DashboardHeaderCard(
                onRefresh = { viewModel.refresh(session, role) },
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                tabs.forEach { item ->
                    FilterChip(
                        selected = item == tab,
                        onClick = { tabName = item.name },
                        label = { Text(item.label) },
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.20f)),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Text("Dados combinados", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (!isVendor && tab != DashboardTab.VISAO) {
            item {
                DashboardFilterCard(
                    state = state,
                    draftFrom = draftFrom,
                    draftTo = draftTo,
                    draftSupervisor = draftSupervisor,
                    draftSeller = draftSeller,
                    onFrom = { draftFrom = it },
                    onTo = { draftTo = it },
                    onSupervisor = { supervisorDialog = true },
                    onSeller = { sellerDialog = true },
                    onApply = {
                        viewModel.applyFilters(session, role, draftFrom, draftTo, draftSupervisor, draftSeller)
                    },
                    onClear = { viewModel.clearFilters(session, role) },
                )
            }
        }

        state.errorMessage?.let { message ->
            item {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        when (tab) {
            DashboardTab.VISAO -> dashboardOverviewItems(state, selectedKpiSetter = { selectedKpi = it })
            DashboardTab.PERFORMANCE -> dashboardPerformanceItems(state)
            DashboardTab.COMERCIAL -> dashboardCommercialItems(state)
            DashboardTab.COBERTURA -> dashboardCoverageItems(state)
            DashboardTab.QUALIDADE -> dashboardQualityItems(state)
        }
    }

    if (supervisorDialog) {
        DashboardOptionDialog(
            title = "Supervisor",
            options = listOf("all" to "Todos") + state.model.supervisors.map { it.id to it.name },
            selected = draftSupervisor,
            onSelect = {
                draftSupervisor = it
                if (it != state.selectedSupervisorId) draftSeller = "all"
                supervisorDialog = false
            },
            onDismiss = { supervisorDialog = false },
        )
    }
    if (sellerDialog) {
        val sellers = state.model.vendors
            .filter { draftSupervisor == "all" || it.supervisorId == draftSupervisor }
            .map { it.name to it.name }
        DashboardOptionDialog(
            title = "Vendedor",
            options = listOf("all" to "Todos") + sellers,
            selected = draftSeller,
            onSelect = {
                draftSeller = it
                sellerDialog = false
            },
            onDismiss = { sellerDialog = false },
        )
    }
    selectedKpi?.let { kpi ->
        DashboardKpiDialog(kpi = kpi, model = state.model, onDismiss = { selectedKpi = null })
    }
}

@Composable
private fun DashboardHeaderCard(onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.15f)),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                Text(
                    "Análise robusta com cruzamento de visitas, vidas registradas, aceite digital e cobertura comercial.",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar") }
        }
    }
}

@Composable
private fun DashboardFilterCard(
    state: ExactDashboardUiState,
    draftFrom: String,
    draftTo: String,
    draftSupervisor: String,
    draftSeller: String,
    onFrom: (String) -> Unit,
    onTo: (String) -> Unit,
    onSupervisor: () -> Unit,
    onSeller: () -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
) {
    val supervisorLabel = state.model.supervisors.firstOrNull { it.id == draftSupervisor }?.name ?: "Todos"
    val sellerLabel = if (draftSeller == "all") "Todos" else draftSeller
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.15f))) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = draftFrom, onValueChange = onFrom, label = { Text("De") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = draftTo, onValueChange = onTo, label = { Text("Até") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSupervisor, modifier = Modifier.weight(1f)) {
                    Text("Supervisor: $supervisorLabel", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(onClick = onSeller, modifier = Modifier.weight(1f)) {
                    Text("Vendedor: $sellerLabel", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApply, modifier = Modifier.weight(1f)) { Text("Aplicar") }
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Limpar") }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.dashboardOverviewItems(
    state: ExactDashboardUiState,
    selectedKpiSetter: (DashboardKpi) -> Unit,
) {
    val m = state.model.metrics
    item {
        DashboardKpiGrid(metrics = m, onClick = selectedKpiSetter)
    }
    item {
        DashboardSectionCard("Leitura rápida") {
            Text("${formatNumber(m.performedVisits)} visitas realizadas, ${formatNumber(m.notPerformedVisits)} não realizadas e ${formatNumber(m.pendingVisits)} pendentes no período.")
            Text("Vidas: ${formatNumber(m.livesVisits)} em visitas + ${formatNumber(m.livesAcceptance)} em aceite digital = ${formatNumber(m.livesTotal)}.")
        }
    }
    if (state.model.daily.isNotEmpty()) {
        item {
            DashboardSectionCard("Vidas por dia") {
                DashboardDailyBarChart(state.model.daily.map { it.lives })
                DashboardDateLegend(state.model.daily.map { it.date })
            }
        }
    }
    if (state.model.sellers.isNotEmpty()) {
        item {
            DashboardSectionCard("Vidas por vendedor") {
                SellerDonutChart(state.model.sellers)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.dashboardPerformanceItems(state: ExactDashboardUiState) {
    val m = state.model.metrics
    val p = state.model.previousMetrics
    val variation = percentVariation(m.totalVisits, p.totalVisits)
    item {
        DashboardSectionCard("Tendência temporal") {
            DashboardDailyBarChart(state.model.daily.map { it.visits })
            DashboardDateLegend(state.model.daily.map { it.date })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallMetric("Total atual", formatNumber(m.totalVisits), Modifier.weight(1f))
                SmallMetric("Período anterior", formatNumber(p.totalVisits), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallMetric("Média diária", if (state.model.daily.isEmpty()) "0" else String.format(Locale("pt", "BR"), "%.1f", m.totalVisits.toDouble() / state.model.daily.size), Modifier.weight(1f))
                SmallMetric("Variação", variation?.let { String.format(Locale("pt", "BR"), "%+.1f%%", it) } ?: "-", Modifier.weight(1f))
            }
        }
    }
    item {
        DashboardSectionCard("Execução") {
            ProgressLine("Concluídas", m.concludedVisits, max(1, m.totalVisits))
            ProgressLine("Realizadas", m.performedVisits, max(1, m.totalVisits))
            ProgressLine("Pendentes", m.pendingVisits, max(1, m.totalVisits))
            Text("Taxa de execução: ${formatPercent(m.executionRate)}", fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.dashboardCommercialItems(state: ExactDashboardUiState) {
    val sellers = state.model.sellers
    item {
        DashboardSectionCard("Performance comercial") {
            Text("Vidas combinadas por vendedor (visitas + aceite digital).", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SellerDonutChart(sellers)
        }
    }
    items(sellers, key = { it.seller }) { seller ->
        DashboardSellerCard(seller)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.dashboardCoverageItems(state: ExactDashboardUiState) {
    val m = state.model.metrics
    item {
        DashboardSectionCard("Cobertura comercial") {
            Text("${formatNumber(m.visitedCompanies)} de ${formatNumber(m.totalClientsInScope)} empresas aparecem no recorte.")
            Text("Cobertura: ${formatPercent(m.coverage)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }
    }
    item { CompanyRankingCard("Mais visitadas", state.model.mostVisitedCompanies.map { it.name to it.count.toString() }) }
    item { CompanyRankingCard("Menos visitadas", state.model.leastVisitedCompanies.map { it.name to it.count.toString() }) }
    item { CompanyRankingCard("Nunca visitadas", state.model.neverVisitedCompanies.map { it.name to "0" }) }
    if (state.model.cities.isNotEmpty()) {
        item { CompanyRankingCard("Visitas por cidade", state.model.cities.map { it.city to it.count.toString() }) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.dashboardQualityItems(state: ExactDashboardUiState) {
    val m = state.model.metrics
    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QualityMetric("Sem empresa", m.missingClient, Modifier.weight(1f))
            QualityMetric("Sem responsável", m.missingResponsible, Modifier.weight(1f))
            QualityMetric("Sem data", m.missingDate, Modifier.weight(1f))
        }
    }
    if (state.model.reasons.isNotEmpty()) {
        item { CompanyRankingCard("Motivos de não visita", state.model.reasons.map { it.reason to it.count.toString() }) }
    }
    item {
        DashboardSectionCard("Qualidade do cadastro") {
            val totalIssues = m.missingClient + m.missingResponsible + m.missingDate
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = if (totalIssues > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(if (totalIssues == 0) "Nenhuma inconsistência básica no período." else "$totalIssues inconsistências básicas encontradas.", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DashboardKpiGrid(metrics: DashboardMetricSet, onClick: (DashboardKpi) -> Unit) {
    val cards = listOf(
        DashboardKpi.VISITAS to formatNumber(metrics.totalVisits),
        DashboardKpi.CONCLUIDAS to formatNumber(metrics.concludedVisits),
        DashboardKpi.PENDENTES to formatNumber(metrics.pendingVisits),
        DashboardKpi.EMPRESAS to formatNumber(metrics.visitedCompanies),
        DashboardKpi.COBERTURA to formatPercent(metrics.coverage),
        DashboardKpi.VIDAS to formatNumber(metrics.livesTotal),
        DashboardKpi.EXECUCAO to formatPercent(metrics.executionRate),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cards.chunked(2).forEach { rowCards ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCards.forEach { (kpi, value) ->
                    Card(
                        onClick = { onClick(kpi) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.15f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(kpi.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                if (rowCards.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DashboardSectionCard(title: String, content: @Composable Column.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.15f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun SmallMetric(label: String, value: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QualityMetric(label: String, value: Int, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.15f))) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProgressLine(label: String, value: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row { Text(label, modifier = Modifier.weight(1f)); Text("$value / $total", fontWeight = FontWeight.SemiBold) }
        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            drawRoundRect(color = Color.LightGray.copy(alpha = 0.35f), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            val fraction = (value.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
            drawRoundRect(color = RotasSea, size = Size(size.width * fraction, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        }
    }
}

@Composable
private fun DashboardDailyBarChart(values: List<Int>) {
    if (values.isEmpty()) {
        Text("Sem dados no período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val maximum = max(1, values.maxOrNull() ?: 1)
    Canvas(modifier = Modifier.fillMaxWidth().height(190.dp)) {
        val count = values.size.coerceAtLeast(1)
        val gap = 4.dp.toPx()
        val available = size.width - gap * (count - 1)
        val barWidth = (available / count).coerceAtLeast(2.dp.toPx())
        values.forEachIndexed { index, value ->
            val ratio = value.toFloat() / maximum
            val height = size.height * ratio
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = if (value > 0) RotasSea else Color.Gray.copy(alpha = 0.25f),
                topLeft = Offset(x, size.height - height),
                size = Size(barWidth, max(2.dp.toPx(), height)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
            )
        }
        val average = values.average().toFloat()
        val averageY = size.height - size.height * (average / maximum)
        drawLine(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), start = Offset(0f, averageY), end = Offset(size.width, averageY), strokeWidth = 1.dp.toPx())
    }
}

@Composable
private fun DashboardDateLegend(dates: List<String>) {
    if (dates.isEmpty()) return
    val formatter = DateTimeFormatter.ofPattern("dd/MM")
    val first = runCatching { LocalDate.parse(dates.first()).format(formatter) }.getOrDefault(dates.first())
    val last = runCatching { LocalDate.parse(dates.last()).format(formatter) }.getOrDefault(dates.last())
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(first, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(last, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SellerDonutChart(sellers: List<DashboardSellerSummary>) {
    val data = sellers.filter { it.livesTotal > 0 }.take(10)
    if (data.isEmpty()) {
        Text("Sem vidas registradas no recorte.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val total = data.sumOf { it.livesTotal }.coerceAtLeast(1)
    val palette = listOf(
        RotasSea,
        Color(0xFF1F7A5A),
        Color(0xFF38BDF8),
        Color(0xFFF59E0B),
        Color(0xFF0F766E),
        Color(0xFF94A3B8),
        Color(0xFF22C55E),
        Color(0xFF7DD3FC),
    )
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(150.dp)) {
            var start = -90f
            data.forEachIndexed { index, seller ->
                val sweep = seller.livesTotal * 360f / total
                drawArc(
                    color = palette[index % palette.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            data.take(6).forEachIndexed { index, seller ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(9.dp)) { drawCircle(palette[index % palette.size]) }
                    Spacer(Modifier.width(7.dp))
                    Text(seller.seller, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                    Text(formatNumber(seller.livesTotal), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun DashboardSellerCard(seller: DashboardSellerSummary) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.15f))) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(seller.seller, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Visitas ${seller.visits}", style = MaterialTheme.typography.bodySmall)
                Text("Empresas ${seller.companies}", style = MaterialTheme.typography.bodySmall)
                Text("Vidas ${seller.livesTotal}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Visitas: ${seller.livesVisit} vidas · Aceite: ${seller.livesAcceptance} vidas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompanyRankingCard(title: String, rows: List<Pair<String, String>>) {
    DashboardSectionCard(title) {
        if (rows.isEmpty()) Text("Sem dados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        rows.take(10).forEachIndexed { index, row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("${index + 1}. ${row.first}", modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                Text(row.second, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DashboardOptionDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(options, key = { it.first }) { option ->
                    FilterChip(selected = option.first == selected, onClick = { onSelect(option.first) }, label = { Text(option.second) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
private fun DashboardKpiDialog(kpi: DashboardKpi, model: DashboardComputedModel, onDismiss: () -> Unit) {
    val m = model.metrics
    val previous = model.previousMetrics
    val value = when (kpi) {
        DashboardKpi.VISITAS -> formatNumber(m.totalVisits)
        DashboardKpi.CONCLUIDAS -> formatNumber(m.concludedVisits)
        DashboardKpi.PENDENTES -> formatNumber(m.pendingVisits)
        DashboardKpi.EMPRESAS -> formatNumber(m.visitedCompanies)
        DashboardKpi.COBERTURA -> formatPercent(m.coverage)
        DashboardKpi.VIDAS -> formatNumber(m.livesTotal)
        DashboardKpi.EXECUCAO -> formatPercent(m.executionRate)
    }
    val previousValue = when (kpi) {
        DashboardKpi.VISITAS -> previous.totalVisits
        DashboardKpi.CONCLUIDAS -> previous.concludedVisits
        DashboardKpi.PENDENTES -> previous.pendingVisits
        DashboardKpi.EMPRESAS -> previous.visitedCompanies
        DashboardKpi.COBERTURA -> previous.coverage.toInt()
        DashboardKpi.VIDAS -> previous.livesTotal
        DashboardKpi.EXECUCAO -> previous.executionRate.toInt()
    }
    val currentNumeric = when (kpi) {
        DashboardKpi.COBERTURA -> m.coverage.toInt()
        DashboardKpi.EXECUCAO -> m.executionRate.toInt()
        DashboardKpi.VISITAS -> m.totalVisits
        DashboardKpi.CONCLUIDAS -> m.concludedVisits
        DashboardKpi.PENDENTES -> m.pendingVisits
        DashboardKpi.EMPRESAS -> m.visitedCompanies
        DashboardKpi.VIDAS -> m.livesTotal
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(kpi.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                Text("Período anterior: ${formatNumber(previousValue)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                percentVariation(currentNumeric, previousValue)?.let { variation ->
                    Text("Variação: ${String.format(Locale("pt", "BR"), "%+.1f%%", variation)}", fontWeight = FontWeight.SemiBold)
                }
                when (kpi) {
                    DashboardKpi.VIDAS -> Text("${m.livesVisits} vidas vieram de visitas e ${m.livesAcceptance} do aceite digital.")
                    DashboardKpi.COBERTURA -> Text("${m.visitedCompanies} empresas aparecem no recorte de uma carteira com ${m.totalClientsInScope} empresas.")
                    DashboardKpi.EXECUCAO -> Text("A taxa considera visitas realizadas sem motivo de não visita em relação ao total do período.")
                    else -> Text("Toque fora do modal para retornar ao Dashboard.")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

private fun formatNumber(value: Int): String = NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(value)
private fun formatPercent(value: Double): String = String.format(Locale("pt", "BR"), "%.1f%%", value)
private fun percentVariation(current: Int, previous: Int): Double? {
    if (previous == 0) return if (current == 0) 0.0 else null
    return (current - previous) * 100.0 / abs(previous)
}
