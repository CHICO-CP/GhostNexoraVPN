package com.ghostnexora.vpn.ui.screens.profiles

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostnexora.vpn.ui.theme.*

@Composable
fun CreateEditProfileScreen(profileId: String?, onSaved: () -> Unit, onBack: () -> Unit, viewModel: CreateEditViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(profileId) { viewModel.loadProfile(profileId) }
    LaunchedEffect(uiState.savedSuccessfully) { if (uiState.savedSuccessfully) onSaved() }
    if (uiState.isLoading) { Box(Modifier.fillMaxSize().background(BackgroundDark), Alignment.Center) { CircularProgressIndicator(color = NeonCyan) }; return }
    Column(Modifier.fillMaxSize().background(BackgroundDark).verticalScroll(rememberScrollState()).padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)) {
        Spacer(Modifier.height(Dimens.SpaceXS))
        FormSection("INFORMACION BASICA", Icons.Filled.Info) {
            GhostTextField(uiState.name, viewModel::onNameChange, "Nombre del perfil *", placeholder = "Ej: Servidor Principal", isError = uiState.nameError != null, errorMessage = uiState.nameError ?: "")
            GhostTextField(uiState.host, viewModel::onHostChange, "Host / Servidor *", placeholder = "Ej: vpn.example.com", isError = uiState.hostError != null, errorMessage = uiState.hostError ?: "", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
            GhostTextField(uiState.port, viewModel::onPortChange, "Puerto *", placeholder = "443", isError = uiState.portError != null, errorMessage = uiState.portError ?: "", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            MethodDropdown(uiState.method, viewModel::onMethodChange)
            SslSwitch(uiState.sslEnabled, viewModel::onSslChange)
        }
        FormSection("CREDENCIALES", Icons.Filled.Key) {
            GhostTextField(uiState.username, viewModel::onUsernameChange, "Usuario", placeholder = "Opcional")
            GhostTextField(uiState.password, viewModel::onPasswordChange, "Contrasena", placeholder = "Opcional",
                visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(viewModel::togglePasswordVisible) { Icon(if (uiState.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = TextTertiary) } })
        }
        AdvancedSection(uiState.showAdvanced, viewModel::toggleAdvancedSection, uiState.sni, uiState.proxyHost, uiState.proxyPort, uiState.proxyType, viewModel::onSniChange, viewModel::onProxyHostChange, viewModel::onProxyPortChange, viewModel::onProxyTypeChange)
        FormSection("METADATA", Icons.Filled.Label) {
            GhostTextField(uiState.tags, viewModel::onTagsChange, "Etiquetas", placeholder = "fast, premium (separadas por coma)", singleLine = false)
            GhostTextField(uiState.notes, viewModel::onNotesChange, "Notas", placeholder = "Notas opcionales", singleLine = false)
            Row(Modifier.fillMaxWidth(), Alignment.CenterVertically, Arrangement.SpaceBetween) {
                Column { Text("Perfil habilitado", style = MaterialTheme.typography.bodyMedium, color = TextPrimary); Text("Aparece en la lista de activos", style = MaterialTheme.typography.bodySmall, color = TextTertiary) }
                Switch(uiState.enabled, viewModel::onEnabledChange, colors = SwitchDefaults.colors(checkedThumbColor = TextOnAccent, checkedTrackColor = NeonCyan, uncheckedThumbColor = TextTertiary, uncheckedTrackColor = SurfaceElevated))
            }
        }
        AnimatedVisibility(uiState.error != null) {
            Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(NeonRed.copy(0.08f)).border(Dimens.BorderNormal, NeonRed.copy(0.4f), MaterialTheme.shapes.medium).padding(Dimens.SpaceMD), Alignment.CenterVertically, Arrangement.spacedBy(Dimens.SpaceSM)) {
                Icon(Icons.Filled.Error, null, tint = NeonRed, modifier = Modifier.size(Dimens.IconMD))
                Text(uiState.error ?: "", style = MaterialTheme.typography.bodySmall, color = NeonRed)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            if (uiState.isSaving) LinearProgressIndicator(Modifier.fillMaxWidth(), color = NeonCyan, trackColor = SurfaceElevated)
            GhostButton(if (uiState.isEditMode) "Guardar Cambios" else "Crear Perfil", viewModel::save, Modifier.fillMaxWidth(), enabled = !uiState.isSaving)
            GhostOutlineButton("Cancelar", onBack, Modifier.fillMaxWidth(), borderColor = BorderNormal, contentColor = TextSecondary)
        }
        Spacer(Modifier.height(Dimens.Space3XL))
    }
}

@Composable
private fun FormSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(Dimens.IconSM))
            Text(title, style = MaterialTheme.typography.labelSmall.copy(color = NeonCyanDim, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.weight(1f))
            HorizontalDivider(Modifier.weight(2f), color = BorderSubtle, thickness = Dimens.BorderThin)
        }
        GhostCard { content() }
    }
}

@Composable
private fun MethodDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("Metodo de conexion", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(bottom = Dimens.SpaceXS))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = InputFieldShape, colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceVariant, contentColor = TextPrimary), border = androidx.compose.foundation.BorderStroke(Dimens.BorderNormal, BorderNormal)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(selected.uppercase(), style = MaterialTheme.typography.bodyMedium.copy(color = NeonCyan, fontWeight = FontWeight.SemiBold))
                    Icon(Icons.Filled.ArrowDropDown, null, tint = TextTertiary)
                }
            }
            DropdownMenu(expanded, { expanded = false }, Modifier.background(SurfaceVariant)) {
                VPN_METHODS.forEach { method ->
                    DropdownMenuItem(text = { Text(method.uppercase(), style = MaterialTheme.typography.bodyMedium.copy(color = if (method == selected) NeonCyan else TextPrimary, fontWeight = if (method == selected) FontWeight.Bold else FontWeight.Normal)) },
                        onClick = { onSelect(method); expanded = false },
                        leadingIcon = if (method == selected) { { Icon(Icons.Filled.Check, null, tint = NeonCyan, modifier = Modifier.size(16.dp)) } } else null)
                }
            }
        }
    }
}

@Composable
private fun SslSwitch(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Alignment.CenterVertically, Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            Icon(if (enabled) Icons.Filled.Lock else Icons.Filled.LockOpen, null, tint = if (enabled) NeonGreen else TextTertiary, modifier = Modifier.size(Dimens.IconSM))
            Column {
                Text("SSL Habilitado", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(if (enabled) "Conexion cifrada (recomendado)" else "Sin cifrado SSL", style = MaterialTheme.typography.bodySmall, color = if (enabled) NeonGreen else NeonAmber)
            }
        }
        Switch(enabled, onChange, colors = SwitchDefaults.colors(checkedThumbColor = TextOnAccent, checkedTrackColor = NeonGreen, uncheckedThumbColor = TextTertiary, uncheckedTrackColor = SurfaceElevated))
    }
}

@Composable
private fun AdvancedSection(expanded: Boolean, onToggle: () -> Unit, sni: String, proxyHost: String, proxyPort: String, proxyType: String, onSniChange: (String) -> Unit, onProxyHostChange: (String) -> Unit, onProxyPortChange: (String) -> Unit, onProxyTypeChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(SurfaceVariant).border(Dimens.BorderNormal, BorderSubtle, MaterialTheme.shapes.medium).clickable(onClick = onToggle).padding(Dimens.SpaceMD), Alignment.CenterVertically, Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                Icon(Icons.Filled.Tune, null, tint = NeonPurple, modifier = Modifier.size(Dimens.IconSM))
                Text("OPCIONES AVANZADAS", style = MaterialTheme.typography.labelSmall.copy(color = NeonPurple, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold))
            }
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = TextTertiary)
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            GhostCard(borderColor = NeonPurple.copy(0.2f)) {
                GhostTextField(sni, onSniChange, "SNI (Server Name Indication)", placeholder = "Ej: cloudflare.com")
                Spacer(Modifier.height(Dimens.SpaceSM))
                Text("Configuracion de Proxy", style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, letterSpacing = 1.sp), modifier = Modifier.padding(vertical = Dimens.SpaceXS))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                    GhostTextField(proxyHost, onProxyHostChange, "Proxy Host", placeholder = "127.0.0.1", modifier = Modifier.weight(2f))
                    GhostTextField(proxyPort, onProxyPortChange, "Puerto", placeholder = "8080", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Spacer(Modifier.height(Dimens.SpaceSM))
                Column {
                    Text("Tipo de Proxy", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(bottom = Dimens.SpaceXS))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                        PROXY_TYPES.forEach { type ->
                            val label = type.ifEmpty { "Ninguno" }
                            val isSelected = type == proxyType
                            FilterChip(selected = isSelected, onClick = { onProxyTypeChange(type) }, label = { Text(label, style = MaterialTheme.typography.labelMedium, color = if (isSelected) TextOnAccent else TextSecondary) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonPurple, selectedLabelColor = TextOnAccent, containerColor = SurfaceElevated, labelColor = TextSecondary),
                                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = BorderNormal, selectedBorderColor = NeonPurple, borderWidth = Dimens.BorderNormal, selectedBorderWidth = Dimens.BorderNormal))
                        }
                    }
                }
            }
        }
    }
}
