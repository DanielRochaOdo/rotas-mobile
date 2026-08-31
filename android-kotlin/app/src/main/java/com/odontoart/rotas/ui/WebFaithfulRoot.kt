package com.odontoart.rotas.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odontoart.rotas.MainViewModel

/**
 * Raiz da segunda etapa nativa: preserva o login existente e, apos autenticar,
 * usa o shell cuja fonte de verdade e o Odontoart-rotas web.
 */
@Composable
fun WebFaithfulRoot(mainViewModel: MainViewModel = viewModel()) {
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    if (state.session == null) {
        RotasRoot(viewModel = mainViewModel)
    } else {
        WebFaithfulApp(mainViewModel = mainViewModel)
    }
}
