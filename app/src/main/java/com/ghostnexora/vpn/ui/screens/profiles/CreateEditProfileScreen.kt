package com.ghostnexora.vpn.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostnexora.vpn.data.model.VpnProfile
import com.ghostnexora.vpn.ui.theme.*

@Composable
fun CreateEditProfileScreen(
    profileId: Long? = null, // null = crear nuevo
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val isEditMode = profileId != null
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Cargar perfil si es modo edición
    LaunchedEffect(profileId) {
        if (profileId != null) {
            viewModel.loadProfileForEdit(profileId)
        }
    }

    // Snackbar
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Éxito al guardar
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onBack()
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Editar Perfil" else "Nuevo Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
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
            GhostCard {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                    // Nombre
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { viewModel.updateName(it) },
                        label = { Text("Nombre del Perfil") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.nameError != null,
                        supportingText = { state.nameError?.let { Text(it, color = Color.Red) } }
                    )

                    // Host
                    OutlinedTextField(
                        value = state.host,
                        onValueChange = { viewModel.updateHost(it) },
                        label = { Text("Servidor (Host)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.hostError != null,
                        supportingText = { state.hostError?.let { Text(it, color = Color.Red) } }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                        // Puerto
                        OutlinedTextField(
                            value = state.port,
                            onValueChange = { viewModel.updatePort(it) },
                            label = { Text("Puerto") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = state.portError != null,
                            supportingText = { state.portError?.let { Text(it, color = Color.Red) } }
                        )

                        // Método
                        OutlinedTextField(
                            value = state.method,
                            onValueChange = { viewModel.updateMethod(it) },
                            label = { Text("Método") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // SSL / TLS
                    SwitchSetting(
                        title = "Habilitar SSL/TLS",
                        checked = state.sslEnabled,
                        onCheckedChange = { viewModel.toggleSslEnabled() }
                    )

                    // Username y Password
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = { viewModel.updateUsername(it) },
                        label = { Text("Usuario (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    var passwordVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("Contraseña (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                                )
                            }
                        }
                    )
                }
            }

            // Advanced Options (collapsible)
            var showAdvanced by remember { mutableStateOf(false) }

            GhostCard {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvanced = !showAdvanced }
                            .padding(Dimens.SpaceMD),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Opciones Avanzadas", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            null
                        )
                    }

                    if (showAdvanced) {
                        Column(
                            modifier = Modifier.padding(horizontal = Dimens.SpaceMD),
                            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
                        ) {
                            OutlinedTextField(
                                value = state.sni ?: "",
                                onValueChange = { viewModel.updateSni(it) },
                                label = { Text("SNI (Server Name Indication)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = state.proxyHost ?: "",
                                onValueChange = { viewModel.updateProxyHost(it) },
                                label = { Text("Proxy Host") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = state.proxyPort ?: "",
                                onValueChange = { viewModel.updateProxyPort(it) },
                                label = { Text("Proxy Puerto") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            // Proxy Type (puedes expandir con Dropdown si quieres)
                            OutlinedTextField(
                                value = state.proxyType ?: "",
                                onValueChange = { viewModel.updateProxyType(it) },
                                label = { Text("Tipo de Proxy") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Tags y Notes
            GhostCard {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                    OutlinedTextField(
                        value = state.tags.joinToString(", "),
                        onValueChange = { viewModel.updateTags(it) },
                        label = { Text("Tags (separados por coma)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.notes ?: "",
                        onValueChange = { viewModel.updateNotes(it) },
                        label = { Text("Notas") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }

            // Save Button
            GhostButton(
                text = if (state.isSaving) "Guardando..." else if (isEditMode) "Guardar Cambios" else "Crear Perfil",
                onClick = { viewModel.saveProfile() },
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
private fun SwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpaceMD, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}