package com.odontoart.rotas.ui

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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import com.odontoart.rotas.ClienteListItem
import com.odontoart.rotas.ParityUiState
import com.odontoart.rotas.UserProfile
import com.odontoart.rotas.UserRole
import com.odontoart.rotas.VisitItem
import org.json.JSONObject

@Composable
internal fun VisitsParityScreen(
    state: ParityUiState,
    profile: UserProfile?,
    onRefresh: () -> Unit,
    onComplete: (String, Int) -> Unit,
    onNoVisit: (String, String, String?) -> Unit,
) {
    var dateFilter by rememberSaveable { mutableStateOf("") }
    var completeVisit by remember { mutableStateOf<VisitItem?>(null) }
    var noVisit by remember { mutableStateOf<VisitItem?>(null) }
    val filtered = remember(state.visits, dateFilter) {
        if (dateFilter.isBlank()) state.visits else state.visits.filter { it.visitDate.startsWith(dateFilter) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ParitySectionHeader(
                "Agenda",
                "Visitas, sequência da rota e registro de atendimento.",
                trailing = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar") } },
            )
        }
        state.errorMessage?.let { item { ParityErrorCard(it) } }
        item {
            OutlinedTextField(
                value = dateFilter,
                onValueChange = { dateFilter = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Filtrar data") },
                placeholder = { Text("AAAA-MM-DD") },
                singleLine = true,
            )
        }
        if (filtered.isEmpty() && !state.isLoading) {
            item { ParityInfoCard("Nenhuma visita encontrada para o filtro informado.") }
        }
        items(filtered, key = { it.id }) { visit ->
            VisitParityCard(
                visit = visit,
                canRegister = profile?.userRole != null,
                onComplete = { completeVisit = visit },
                onNoVisit = { noVisit = visit },
            )
        }
    }

    completeVisit?.let { visit ->
        CompleteVisitDialog(
            visit = visit,
            onDismiss = { completeVisit = null },
            onConfirm = { vidas -> onComplete(visit.id, vidas); completeVisit = null },
        )
    }
    noVisit?.let { visit ->
        NoVisitDialog(
            visit = visit,
            onDismiss = { noVisit = null },
            onConfirm = { reason, observation -> onNoVisit(visit.id, reason, observation); noVisit = null },
        )
    }
}

@Composable
private fun VisitParityCard(
    visit: VisitItem,
    canRegister: Boolean,
    onComplete: () -> Unit,
    onNoVisit: () -> Unit,
) {
    val registered = visit.completedAt != null || visit.completedVidas != null || !visit.noVisitReason.isNullOrBlank()
    val company = visit.cliente
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(company?.empresa ?: company?.nomeFantasia ?: "Empresa", fontWeight = FontWeight.SemiBold)
                    Text(
                        listOfNotNull(company?.codigo, formatDateBr(visit.visitDate), visit.visitTime).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ParityStatusPill(
                    when {
                        visit.completedAt != null || visit.completedVidas != null -> "Concluída"
                        !visit.noVisitReason.isNullOrBlank() -> "Não realizada"
                        else -> "Pendente"
                    },
                )
            }
            visit.assignedToName?.let { Text("Responsável: $it", style = MaterialTheme.typography.bodySmall) }
            visit.perfilVisita?.let { Text("Perfil visita: $it", style = MaterialTheme.typography.bodySmall) }
            visit.instructions?.takeIf { it.isNotBlank() }?.let { Text("Instruções: $it", style = MaterialTheme.typography.bodySmall) }
            visit.noVisitReason?.let { Text("Motivo: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (visit.completedVidas != null) Text("Vidas registradas: ${visit.completedVidas}", style = MaterialTheme.typography.bodySmall)
            if (canRegister && !registered) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onComplete, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Concluir")
                    }
                    OutlinedButton(onClick = onNoVisit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Block, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Não realizada")
                    }
                }
            }
        }
    }
}

@Composable
private fun CompleteVisitDialog(visit: VisitItem, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var vidas by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Concluir visita") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(visit.cliente?.empresa ?: visit.cliente?.nomeFantasia ?: "Empresa")
                OutlinedTextField(
                    value = vidas,
                    onValueChange = { if (it.all(Char::isDigit)) vidas = it },
                    label = { Text("Quantidade de vidas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(vidas.toIntOrNull() ?: 0) }, enabled = vidas.isNotBlank()) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun NoVisitDialog(visit: VisitItem, onDismiss: () -> Unit, onConfirm: (String, String?) -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    var observation by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Visita não realizada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(visit.cliente?.empresa ?: visit.cliente?.nomeFantasia ?: "Empresa")
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Motivo") }, singleLine = true)
                OutlinedTextField(value = observation, onValueChange = { observation = it }, label = { Text("Observação") }, minLines = 3)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(reason.trim(), observation.trim().ifBlank { null }) }, enabled = reason.isNotBlank()) { Text("Registrar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
internal fun ClientsParityScreen(
    state: ParityUiState,
    onSearch: (String) -> Unit,
    onCreate: (JSONObject, String) -> Unit,
    onUpdate: (String, JSONObject, String) -> Unit,
    onDelete: (String, String) -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
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
            ParitySectionHeader(
                "Empresas",
                "Cadastro, pesquisa e informações comerciais.",
                trailing = { IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, contentDescription = "Cadastrar empresa") } },
            )
        }
        state.errorMessage?.let { item { ParityErrorCard(it) } }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Código, empresa ou CNPJ") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSearch(search) }) { Text("Buscar") }
            }
        }
        if (state.clients.isEmpty() && !state.isLoading) item { ParityInfoCard("Nenhuma empresa encontrada.") }
        items(state.clients, key = { it.id }) { client ->
            Card(onClick = { selected = client }, shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(client.empresa ?: client.nomeFantasia ?: "Empresa", fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(client.codigo, client.cnpj).joinToString(" • ").ifBlank { "Sem identificação" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        client.situacao?.let { ParityStatusPill(it) }
                    }
                    val address = listOfNotNull(client.endereco, client.bairro, client.cidade, client.uf).filter { it.isNotBlank() }.joinToString(", ")
                    if (address.isNotBlank()) Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    selected?.let { client ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(client.empresa ?: client.nomeFantasia ?: "Empresa") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    ClientDetailLine("Código", client.codigo)
                    ClientDetailLine("CNPJ", client.cnpj)
                    ClientDetailLine("Vidas", client.vidasQtde?.toString())
                    ClientDetailLine("Contato", listOfNotNull(client.pessoa, client.contato).joinToString(" • "))
                    ClientDetailLine("Grupo", client.grupo)
                    ClientDetailLine("Situação", client.situacao)
                    ClientDetailLine("Categoria", client.categoria)
                    ClientDetailLine("Perfil visita", client.perfilVisita)
                    ClientDetailLine("Regra de visita", client.regraVisitaObservacao)
                    ClientDetailLine("Endereço", listOfNotNull(client.endereco, client.complemento, client.bairro, client.cidade, client.uf).joinToString(", "))
                }
            },
            confirmButton = {
                TextButton(onClick = { editing = client; selected = null }) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Editar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = client; selected = null }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Excluir")
                }
            },
        )
    }

    if (creating) {
        ClientFormDialog(
            title = "Cadastrar empresa",
            initial = null,
            onDismiss = { creating = false },
            onSave = { payload -> onCreate(payload, search); creating = false },
        )
    }
    editing?.let { client ->
        ClientFormDialog(
            title = "Editar empresa",
            initial = client,
            onDismiss = { editing = null },
            onSave = { payload -> onUpdate(client.id, payload, search); editing = null },
        )
    }
    deleting?.let { client ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Excluir empresa") },
            text = { Text("Confirma a exclusão de ${client.empresa ?: client.codigo ?: "esta empresa"}?") },
            confirmButton = { TextButton(onClick = { onDelete(client.id, search); deleting = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ClientDetailLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ClientFormDialog(
    title: String,
    initial: ClienteListItem?,
    onDismiss: () -> Unit,
    onSave: (JSONObject) -> Unit,
) {
    var codigo by rememberSaveable(initial?.id) { mutableStateOf(initial?.codigo.orEmpty()) }
    var cnpj by rememberSaveable(initial?.id) { mutableStateOf(initial?.cnpj.orEmpty()) }
    var empresa by rememberSaveable(initial?.id) { mutableStateOf(initial?.empresa.orEmpty()) }
    var vidas by rememberSaveable(initial?.id) { mutableStateOf(initial?.vidasQtde?.toString().orEmpty()) }
    var pessoa by rememberSaveable(initial?.id) { mutableStateOf(initial?.pessoa.orEmpty()) }
    var contato by rememberSaveable(initial?.id) { mutableStateOf(initial?.contato.orEmpty()) }
    var grupo by rememberSaveable(initial?.id) { mutableStateOf(initial?.grupo.orEmpty()) }
    var situacao by rememberSaveable(initial?.id) { mutableStateOf(initial?.situacao ?: "Ativo") }
    var categoria by rememberSaveable(initial?.id) { mutableStateOf(initial?.categoria.orEmpty()) }
    var perfil by rememberSaveable(initial?.id) { mutableStateOf(initial?.perfilVisita.orEmpty()) }
    var regra by rememberSaveable(initial?.id) { mutableStateOf(initial?.regraVisitaObservacao.orEmpty()) }
    var endereco by rememberSaveable(initial?.id) { mutableStateOf(initial?.endereco.orEmpty()) }
    var complemento by rememberSaveable(initial?.id) { mutableStateOf(initial?.complemento.orEmpty()) }
    var bairro by rememberSaveable(initial?.id) { mutableStateOf(initial?.bairro.orEmpty()) }
    var cidade by rememberSaveable(initial?.id) { mutableStateOf(initial?.cidade.orEmpty()) }
    var uf by rememberSaveable(initial?.id) { mutableStateOf(initial?.uf.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(codigo, { codigo = it }, label = { Text("Código") }, singleLine = true)
                OutlinedTextField(cnpj, { cnpj = it }, label = { Text("CNPJ") }, singleLine = true)
                OutlinedTextField(empresa, { empresa = it }, label = { Text("Empresa") }, singleLine = true)
                OutlinedTextField(vidas, { if (it.all(Char::isDigit)) vidas = it }, label = { Text("Vidas") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(pessoa, { pessoa = it }, label = { Text("Pessoa de contato") }, singleLine = true)
                OutlinedTextField(contato, { contato = it }, label = { Text("Contato") }, singleLine = true)
                OutlinedTextField(grupo, { grupo = it }, label = { Text("Grupo") }, singleLine = true)
                OutlinedTextField(situacao, { situacao = it }, label = { Text("Situação") }, singleLine = true)
                OutlinedTextField(categoria, { categoria = it }, label = { Text("Categoria") }, singleLine = true)
                OutlinedTextField(perfil, { perfil = it }, label = { Text("Perfil visita") })
                OutlinedTextField(regra, { regra = it }, label = { Text("Regra de visita") }, minLines = 2)
                OutlinedTextField(endereco, { endereco = it }, label = { Text("Endereço") })
                OutlinedTextField(complemento, { complemento = it }, label = { Text("Complemento") }, singleLine = true)
                OutlinedTextField(bairro, { bairro = it }, label = { Text("Bairro") }, singleLine = true)
                OutlinedTextField(cidade, { cidade = it }, label = { Text("Cidade") }, singleLine = true)
                OutlinedTextField(uf, { uf = it.uppercase().take(2) }, label = { Text("UF") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val payload = JSONObject()
                        .put("codigo", codigo.trim().ifBlank { JSONObject.NULL })
                        .put("cnpj", cnpj.trim().ifBlank { JSONObject.NULL })
                        .put("empresa", empresa.trim())
                        .put("vidas_qtde", vidas.toIntOrNull() ?: JSONObject.NULL)
                        .put("pessoa", pessoa.trim().ifBlank { JSONObject.NULL })
                        .put("contato", contato.trim().ifBlank { JSONObject.NULL })
                        .put("grupo", grupo.trim().ifBlank { JSONObject.NULL })
                        .put("situacao", situacao.trim().ifBlank { "Ativo" })
                        .put("categoria", categoria.trim().ifBlank { JSONObject.NULL })
                        .put("perfil_visita", perfil.trim().ifBlank { JSONObject.NULL })
                        .put("regra_visita_observacao", regra.trim().ifBlank { JSONObject.NULL })
                        .put("endereco", endereco.trim().ifBlank { JSONObject.NULL })
                        .put("complemento", complemento.trim().ifBlank { JSONObject.NULL })
                        .put("bairro", bairro.trim().ifBlank { JSONObject.NULL })
                        .put("cidade", cidade.trim().ifBlank { JSONObject.NULL })
                        .put("uf", uf.trim().ifBlank { JSONObject.NULL })
                    onSave(payload)
                },
                enabled = empresa.isNotBlank(),
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
internal fun AcceptanceParityScreen(
    state: ParityUiState,
    role: UserRole?,
    onRefresh: () -> Unit,
    onRegister: (String, Int) -> Unit,
) {
    var selectedDate by rememberSaveable { mutableStateOf("") }
    var vidas by rememberSaveable { mutableStateOf("") }
    val pendingDate = if (selectedDate in state.pendingAcceptanceDates) selectedDate else state.pendingAcceptanceDates.firstOrNull().orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ParitySectionHeader(
                "Aceite digital",
                "Registro diário de vidas aceitas.",
                trailing = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar") } },
            )
        }
        state.errorMessage?.let { item { ParityErrorCard(it) } }
        if (role == UserRole.VENDEDOR) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Pendências", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (state.pendingAcceptanceDates.isEmpty()) "Sem pendências no momento."
                            else "${state.pendingAcceptanceDates.size} data(s) pendente(s).",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.pendingAcceptanceDates.isNotEmpty()) {
                            OutlinedTextField(
                                value = pendingDate,
                                onValueChange = { selectedDate = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Data pendente") },
                                supportingText = { Text("Datas: ${state.pendingAcceptanceDates.joinToString { formatDateBr(it) }}") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = vidas,
                                onValueChange = { if (it.all(Char::isDigit)) vidas = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Quantidade de vidas") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                            )
                            Button(
                                onClick = { onRegister(pendingDate, vidas.toIntOrNull() ?: 0); vidas = "" },
                                enabled = pendingDate.isNotBlank() && vidas.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Registrar") }
                        }
                    }
                }
            }
        }
        item {
            Text(
                if (role == UserRole.VENDEDOR) "Registros realizados" else "Resumo do dia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (state.digitalSummary.isEmpty() && !state.isLoading) item { ParityInfoCard("Nenhum aceite encontrado.") }
        items(state.digitalSummary, key = { it.id }) { row ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.vendorName ?: "Vendedor", fontWeight = FontWeight.SemiBold)
                        Text(formatDateBr(row.entryDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${row.vidas} vidas", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
