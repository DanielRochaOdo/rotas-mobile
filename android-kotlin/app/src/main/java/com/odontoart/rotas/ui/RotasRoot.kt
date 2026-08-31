package com.odontoart.rotas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
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
import com.odontoart.rotas.ui.theme.RotasBorder
import com.odontoart.rotas.ui.theme.RotasCanvas
import com.odontoart.rotas.ui.theme.RotasCanvasDeep
import com.odontoart.rotas.ui.theme.RotasInk
import com.odontoart.rotas.ui.theme.RotasMist
import com.odontoart.rotas.ui.theme.RotasMuted
import com.odontoart.rotas.ui.theme.RotasSea
import com.odontoart.rotas.ui.theme.RotasSeaLight

@Composable
fun RotasRoot(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.session == null) {
        NativeLoginScreen(
            state = state,
            onLogin = viewModel::signIn,
            onClearError = viewModel::clearError,
        )
    } else {
        NativeParityApp(mainViewModel = viewModel)
    }
}

@Composable
private fun NativeLoginScreen(
    state: MainUiState,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()
    val canSubmit = state.isConfigured && !state.isAuthenticating && email.isNotBlank() && password.isNotBlank()

    fun submit() {
        if (canSubmit) onLogin(email.trim(), password)
    }

    val heroModifier = Modifier.drawWithCache {
        val base = if (dark) {
            Brush.linearGradient(listOf(Color(0xFF171717), Color(0xFF121212), Color(0xFF0F0F0F)))
        } else {
            Brush.linearGradient(listOf(RotasCanvas, Color(0xFFEEF5F1), Color(0xFFF9FCFA)))
        }
        val leftGlow = Brush.radialGradient(
            colors = listOf(
                (if (dark) RotasSeaLight.copy(alpha = 0.12f) else RotasSeaLight.copy(alpha = 0.16f)),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.12f, size.height * 0.08f),
            radius = size.maxDimension * 0.62f,
        )
        val rightGlow = Brush.radialGradient(
            colors = listOf(
                (if (dark) RotasSea.copy(alpha = 0.08f) else RotasSea.copy(alpha = 0.12f)),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.88f, 0f),
            radius = size.maxDimension * 0.55f,
        )
        onDrawBehind {
            drawRect(base)
            drawRect(leftGlow)
            drawRect(rightGlow)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(heroModifier)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            val wide = maxWidth >= 820.dp
            val contentModifier = Modifier.fillMaxWidth().widthIn(max = 1120.dp)

            if (wide) {
                Row(
                    modifier = contentModifier,
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LoginIntro(dark = dark, modifier = Modifier.weight(1f))
                    LoginCard(
                        state = state,
                        email = email,
                        password = password,
                        showPassword = showPassword,
                        canSubmit = canSubmit,
                        dark = dark,
                        onEmailChange = {
                            email = it
                            if (state.errorMessage != null) onClearError()
                        },
                        onPasswordChange = {
                            password = it
                            if (state.errorMessage != null) onClearError()
                        },
                        onTogglePassword = { showPassword = !showPassword },
                        onSubmit = ::submit,
                        modifier = Modifier.weight(1f).widthIn(max = 448.dp),
                    )
                }
            } else {
                Column(
                    modifier = contentModifier.widthIn(max = 448.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    LoginIntro(dark = dark)
                    LoginCard(
                        state = state,
                        email = email,
                        password = password,
                        showPassword = showPassword,
                        canSubmit = canSubmit,
                        dark = dark,
                        onEmailChange = {
                            email = it
                            if (state.errorMessage != null) onClearError()
                        },
                        onPasswordChange = {
                            password = it
                            if (state.errorMessage != null) onClearError()
                        },
                        onTogglePassword = { showPassword = !showPassword },
                        onSubmit = ::submit,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginIntro(dark: Boolean, modifier: Modifier = Modifier) {
    val primaryText = if (dark) Color.White else RotasInk
    val secondaryText = if (dark) Color.White.copy(alpha = 0.72f) else RotasInk.copy(alpha = 0.70f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = "ODONTOART",
            style = MaterialTheme.typography.labelSmall,
            color = if (dark) Color.White.copy(alpha = 0.55f) else RotasMuted,
            letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.8f,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "AGENDA+ ROTAS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = primaryText,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "PLATAFORMA INTERNA DE GESTÃO DE VISITAS E ROTEIRIZAÇÃO COMERCIAL. O ACESSO É RESTRITO E CONTROLADO PELA ODONTOART.",
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryText,
        )
        Spacer(Modifier.height(22.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (dark) Color.White.copy(alpha = 0.10f) else Color(0x66F1F6F3),
            ),
            border = BorderStroke(1.dp, if (dark) RotasSea.copy(alpha = 0.25f) else RotasSea.copy(alpha = 0.20f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ACESSO EXCLUSIVO",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText,
                )
                Text(
                    text = "CASO PRECISE DE CREDENCIAIS, FALE COM A SUPERVISÃO COMERCIAL.",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText,
                )
            }
        }
    }
}

@Composable
private fun LoginCard(
    state: MainUiState,
    email: String,
    password: String,
    showPassword: Boolean,
    canSubmit: Boolean,
    dark: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryText = if (dark) Color.White else RotasInk
    val secondaryText = if (dark) Color.White.copy(alpha = 0.70f) else RotasInk.copy(alpha = 0.70f)
    val inputBackground = if (dark) Color.White.copy(alpha = 0.10f) else Color.White
    val inputBorder = if (dark) Color.White.copy(alpha = 0.10f) else RotasMist

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.95f),
        ),
        border = BorderStroke(1.dp, if (dark) RotasSea.copy(alpha = 0.25f) else RotasSea.copy(alpha = 0.20f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ENTRAR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, color = primaryText)
                Text("USE SEU E-MAIL CORPORATIVO ODONTOART.", style = MaterialTheme.typography.bodySmall, color = secondaryText)
            }

            if (!state.isConfigured) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = "A CONFIGURAÇÃO DE ACESSO AO SERVIDOR NÃO FOI CARREGADA NESTE APK.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("E-MAIL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = primaryText)
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("NOME@ODONTOART.COM.BR") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RotasSea,
                        unfocusedBorderColor = inputBorder,
                        focusedContainerColor = inputBackground,
                        unfocusedContainerColor = inputBackground,
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText,
                        focusedPlaceholderColor = secondaryText.copy(alpha = 0.65f),
                        unfocusedPlaceholderColor = secondaryText.copy(alpha = 0.65f),
                    ),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SENHA", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = primaryText)
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("********") },
                    trailingIcon = {
                        IconButton(onClick = onTogglePassword) {
                            Icon(
                                imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha",
                                tint = secondaryText,
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RotasSea,
                        unfocusedBorderColor = inputBorder,
                        focusedContainerColor = inputBackground,
                        unfocusedContainerColor = inputBackground,
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText,
                        focusedPlaceholderColor = secondaryText.copy(alpha = 0.65f),
                        unfocusedPlaceholderColor = secondaryText.copy(alpha = 0.65f),
                    ),
                )
            }

            state.errorMessage?.let { error ->
                Text(
                    text = error.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dark) Color(0xFFFCA5A5) else Color(0xFFEF4444),
                )
            }

            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, RotasSea.copy(alpha = 0.40f)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RotasSeaLight,
                    contentColor = if (dark) Color.White else RotasInk,
                    disabledContainerColor = RotasSeaLight.copy(alpha = 0.55f),
                    disabledContentColor = if (dark) Color.White.copy(alpha = 0.70f) else RotasInk.copy(alpha = 0.70f),
                ),
            ) {
                if (state.isAuthenticating) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp).height(18.dp),
                        strokeWidth = 2.dp,
                        color = if (dark) Color.White else RotasInk,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (state.isAuthenticating) "ENTRANDO..." else "ENTRAR", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
