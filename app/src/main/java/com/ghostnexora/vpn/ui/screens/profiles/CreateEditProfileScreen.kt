@file:OptIn(ExperimentalMaterial3Api::class)

package com.ghostnexora.vpn.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ghostnexora.vpn.ui.theme.BackgroundDark
import com.ghostnexora.vpn.ui.theme.BorderSubtle
import com.ghostnexora.vpn.ui.theme.Dimens
import com.ghostnexora.vpn.ui.theme.GhostButton
import com.ghostnexora.vpn.ui.theme.GhostCard
import com.ghostnexora.vpn.ui.theme.NeonAmber
import com.ghostnexora.vpn.ui.theme.NeonGreen
import com.ghostnexora.vpn.ui.theme.TextOnAccent
import com.ghostnexora.vpn.ui.theme.TextPrimary
import com.ghostnexora.vpn.ui.theme.TextSecondary
import com.ghostnexora.vpn.ui.theme.TextTertiary
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun CreateEditProfileScreen(
    profileId: String? = null,
    onBack: () -> Unit,
    viewModel: CreateEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }, enabled = !state.isSaving) {
                        Text(if (state.isSaving) "Guardando" else "Guardar", color = NeonAmber)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXL)
        ) {
            GhostCard(borderColor = BorderSubtle, contentPadding = PaddingValues(Dimens.SpaceMD)) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Nombre del Perfil") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.nameError != null,
                        supportingText = { state.nameError?.let { Text(it, color = Color.Red) } }
                    )

                    OutlinedTextField(
                        value = state.host,
                        onValueChange = viewModel::onHostChange,
                        label = { Text("Servidor (Host)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.hostError != null,
                        supportingText = { state.hostError?.let { Text(it, color = Color.Red) } }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                        OutlinedTextField(
                            value = state.port,
                            onValueChange = viewModel::onPortChange,
                            label = { Text("Puerto") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = state.portError != null,
                            supportingText = { state.portError?.let { Text(it, color = Color.Red) } }
                        )

                        OutlinedTextField(
                            value = state.method,
                            onValueChange = viewModel::onMethodChange,
                            label = { Text("Método") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SwitchRow(
                        title = "Habilitar SSL/TLS",
                        checked = state.sslEnabled,
                        onCheckedChange = viewModel::onSslChange
                    )

                    OutlinedTextField(
                        value = state.username,
                        onValueChange = viewModel::onUsernameChange,
                        label = { Text("Usuario") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text("Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = viewModel::togglePasswordVisible) {
                                Icon(
                                    if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (state.passwordVisible) "Ocultar" else "Mostrar"
                                )
                            }
                        }
                    )
                }
            }

            GhostCard(borderColor = BorderSubtle, contentPadding = PaddingValues(Dimens.SpaceMD)) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                    Text("Opciones Avanzadas", style = MaterialTheme.typography.titleSmall, color = TextPrimary)

                    OutlinedTextField(
                        value = state.sni,
                        onValueChange = viewModel::onSniChange,
                        label = { Text("SNI (Server Name Indication)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proxyHost,
                        onValueChange = viewModel::onProxyHostChange,
                        label = { Text("Proxy Host") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proxyPort,
                        onValueChange = viewModel::onProxyPortChange,
                        label = { Text("Proxy Puerto") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = state.proxyType,
                        onValueChange = viewModel::onProxyTypeChange,
                        label = { Text("Tipo de Proxy") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SwitchRow(
                        title = "Perfil habilitado",
                        checked = state.enabled,
                        onCheckedChange = viewModel::onEnabledChange
                    )
                }
            }

            GhostCard(borderColor = BorderSubtle, contentPadding = PaddingValues(Dimens.SpaceMD)) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                    OutlinedTextField(
                        value = state.tags,
                        onValueChange = viewModel::onTagsChange,
                        label = { Text("Tags (separados por coma)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = viewModel::onNotesChange,
                        label = { Text("Notas") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }

            GhostButton(
                text = if (state.isSaving) "Guardando..." else if (state.isEditMode) "Guardar Cambios" else "Crear Perfil",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                containerColor = NeonAmber,
                contentColor = TextOnAccent
            )

            Spacer(Modifier.height(Dimens.Space3XL))
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = TextSecondary)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}