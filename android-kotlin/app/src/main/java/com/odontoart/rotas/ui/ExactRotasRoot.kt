package com.odontoart.rotas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odontoart.rotas.MainUiState
import com.odontoart.rotas.MainViewModel
import com.odontoart.rotas.ui.theme.RotasCanvas
import com.odontoart.rotas.ui.theme.RotasSea

/**
 * Ponto de entrada nativo definitivo. O estado autenticado nunca chama NativeParityApp.
 * Login reproduz src/pages/Login.tsx; apos login a navegacao entra em ExactParityApp.
 */
@Composable
fun ExactRotasRoot(mainViewModel: MainViewModel = viewModel()) {
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    if (state.session == null) {
        ExactLoginScreen(
            state = state,
            onLogin = mainViewModel::signIn,
            onClearError = mainViewModel::clearError,
        )
    } else {
        ExactParityApp(mainViewModel = mainViewModel)
    }
}

@Composable
private fun ExactLoginScreen(
    state: MainUiState,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val canSubmit = state.isConfigured && !state.isAuthenticating && email.isNotBlank() && password.isNotBlank()

    fun submit() {
        if (canSubmit) onLogin(email.trim(), password)
    }

    val background = Brush.linearGradient(
        listOf(
            Color(0xFFF6FAF7),
            Color(0xFFEEF6F1),
            Color(0xFFF9FCFA),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp)) {
                Text(
                    text = "ODONTOART",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Agenda+ Rotas",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Plataforma interna de gestão de visitas e roteirização comercial. O acesso é restrito e controlado pela Odontoart.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(20.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RotasSea.copy(alpha = 0.20f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Acesso exclusivo", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Caso precise de credenciais, fale com a supervisão comercial.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RotasSea.copy(alpha = 0.20f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Entrar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                        Text(
                            "Use seu e-mail corporativo Odontoart.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (!state.isConfigured) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    "A configuração de acesso ao servidor não foi carregada neste APK.",
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                if (state.errorMessage != null) onClearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("E-mail") },
                            placeholder = { Text("nome@odontoart.com.br") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (state.errorMessage != null) onClearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Senha") },
                            placeholder = { Text("********") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha",
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                        )

                        state.errorMessage?.let { error ->
                            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }

                        Button(
                            onClick = { submit() },
                            enabled = canSubmit,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (state.isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 8.dp).height(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                            Text(if (state.isAuthenticating) "Entrando..." else "Entrar", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
