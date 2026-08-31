package com.odontoart.rotas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odontoart.rotas.MainUiState
import com.odontoart.rotas.MainViewModel
import com.odontoart.rotas.ui.theme.RotasCanvas
import com.odontoart.rotas.ui.theme.RotasCanvasDeep
import com.odontoart.rotas.ui.theme.RotasSea
import com.odontoart.rotas.ui.theme.RotasSeaLight

@Composable
fun NativeAppRoot(mainViewModel: MainViewModel = viewModel()) {
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    if (state.session == null) {
        ReferenceLoginScreen(
            state = state,
            onLogin = mainViewModel::signIn,
            onClearError = mainViewModel::clearError,
        )
    } else {
        NativeParityApp(mainViewModel = mainViewModel)
    }
}

@Composable
private fun ReferenceLoginScreen(
    state: MainUiState,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFF171717), Color(0xFF121212), Color(0xFF0F0F0F)))
    } else {
        Brush.linearGradient(listOf(RotasCanvasDeep, RotasCanvas, Color.White))
    }
    val canSubmit = state.isConfigured && !state.isAuthenticating && email.isNotBlank() && password.isNotBlank()

    fun submit() {
        if (canSubmit) onLogin(email.trim(), password)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White,
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                    ) {
                        Icon(
                            Icons.Rounded.Map,
                            contentDescription = null,
                            tint = RotasSea,
                            modifier = Modifier.padding(10.dp).size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "ODONTOART",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                    )
                }
                Text(
                    text = "Agenda+ Rotas",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Plataforma interna de gestão de visitas e roteirização comercial. O acesso é restrito e controlado pela Odontoart.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                )
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                ),
                border = CardDefaults.outlinedCardBorder(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Acesso exclusivo", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Caso precise de credenciais, fale com a supervisão comercial.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.97f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Entrar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Use seu e-mail corporativo Odontoart.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    if (!state.isConfigured) {
                        ParityErrorCard("A configuração de acesso ao servidor não foi carregada neste APK.")
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (state.errorMessage != null || state.accessDeniedMessage != null) onClearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("E-mail") },
                        placeholder = { Text("nome@odontoart.com.br") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(13.dp),
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (state.errorMessage != null || state.accessDeniedMessage != null) onClearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Senha") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha",
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        shape = RoundedCornerShape(13.dp),
                    )

                    (state.accessDeniedMessage ?: state.errorMessage)?.let { ParityErrorCard(it) }

                    Button(
                        onClick = ::submit,
                        enabled = canSubmit,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        if (state.isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(19.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(9.dp))
                        }
                        Text(if (state.isAuthenticating) "Entrando..." else "Entrar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Text(
                text = "Sistema interno Odontoart",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
            )
        }
    }
}
