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

// ══════════════════════════════════════════════════════════════════════════
// CREATE / EDIT PROFILE SCREEN
// ══════════════════════════════════════════════════════════════════════════

@Composable
fun CreateEditProfileScreen(
    profileId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Cargar perfil si es edición
    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }

    // Navegar al guardar
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onSaved()
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize().background(BackgroundDark), Alignment.Center) {
            CircularProgressIndicator(color = NeonCyan)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)
    ) {
        Spacer(Modifier.height(Dimens.SpaceXS))

        // ── 1. Sección básica ──────────────────────────────────────────────
        FormSection(
            title = "INFORMACIÓN BÁSICA",
            icon  = Icons.Filled.Info
        ) {
            // Nombre
            GhostTextField(
                value         = uiState.name,
                onValueChange = viewModel::onNameChange,
                label         = "Nombre del perfil *",
                placeholder   = "Ej: Servidor Principal",
                isError       = uiState.nameError != null,
                errorMessage  = uiState.nameError ?: ""
            )

            // Host
            GhostTextField(
                value         = uiState.host,
                onValueChange = viewModel::onHostChange,
                label         = "Host / Servidor *",
                placeholder   = "Ej: vpn.example.com o 1.2.3.4",
                isError       = uiState.hostError != null,
                errorMessage  = uiState.hostError ?: "",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            // Puerto
            GhostTextField(
                value           = uiState.port,
                onValueChange   = viewModel::onPortChange,
                label           = "Puerto *",
                placeholder     = "443",
                isError         = uiState.portError != null,
                errorMessage    = uiState.portError ?: "",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Método VPN
            MethodDropdown(
                selected  = uiState.method,
                onSelect  = viewModel::onMethodChange
            )

            // Switch SSL
            SslSwitch(
                enabled   = uiState.sslEnabled,
                onChange  = viewModel::onSslChange
            )
        }

        // ── 2. Sección de credenciales ─────────────────────────────────────
        FormSection(
            title = "CREDENCIALES",
            icon  = Icons.Filled.Key
        ) {
            GhostTextField(
                value         = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                label         = "Usuario",
                placeholder   = "Opcional"
            )

            GhostTextField(
                value               = uiState.password,
                onValueChange       = viewModel::onPasswordChange,
                label               = "Contraseña",
                placeholder         = "Opcional",
                visualTransformation = if (uiState.passwordVisible)
                    VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisible) {
                        Icon(
                            imageVector = if (uiState.passwordVisible)
                                Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = "Mostrar/ocultar",
                            tint = TextTertiary
                        )
                    }
                }
            )
        }

        // ── 3. Sección avanzada (expandible) ──────────────────────────────
        AdvancedSection(
            expanded      = uiState.showAdvanced,
            onToggle      = viewModel::toggleAdvancedSection,
            sni           = uiState.sni,
            proxyHost     = uiState.proxyHost,
            proxyPort     = uiState.proxyPort,
            proxyType     = uiState.proxyType,
            onSniChange   = viewModel::onSniChange,
            onProxyHostChange = viewModel::onProxyHostChange,
            onProxyPortChange = viewModel::onProxyPortChange,
            onProxyTypeChange = viewModel::onProxyTypeChange
        )

        // ── 4. Sección de metadata ─────────────────────────────────────────
        FormSection(
            title = "METADATA",
            icon  = Icons.Filled.Label
        ) {
            GhostTextField(
                value         = uiState.tags,
                onValueChange = viewModel::onTagsChange,
                label         = "Etiquetas",
                placeholder   = "fast, premium, us (separadas por coma)",
                singleLine    = false
            )

            GhostTextField(
                value         = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label         = "Notas",
                placeholder   = "Notas opcionales sobre este perfil",
                singleLine    = false
            )

            // Switch habilitado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text  = "Perfil habilitado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text  = "Aparece en la lista de activos",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
                Switch(
                    checked         = uiState.enabled,
                    onCheckedChange = viewModel::onEnabledChange,
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor    = TextOnAccent,
                        checkedTrackColor    = NeonCyan,
                        uncheckedThumbColor  = TextTertiary,
                        uncheckedTrackColor  = SurfaceElevated
                    )
                )
            }
        }

        // ── 5. Error general ───────────────────────────────────────────────
        AnimatedVisibility(visible = uiState.error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(NeonRed.copy(0.08f))
                    .border(Dimens.BorderNormal, NeonRed.copy(0.4f), MaterialTheme.shapes.medium)
                    .padding(Dimens.SpaceMD),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
            ) {
                Icon(Icons.Filled.Error, null, tint = NeonRed, modifier = Modifier.size(Dimens.IconMD))
                Text(uiState.error ?: "", style = MaterialTheme.typography.bodySmall, color = NeonRed)
            }
        }

        // ── 6. Botones de acción ───────────────────────────────────────────
        ActionButtons(
            isSaving   = uiState.isSaving,
            isEditMode = uiState.isEditMode,
            onSave     = viewModel::save,
            onCancel   = onBack
        )

        Spacer(Modifier.height(Dimens.Space3XL))
    }
}

// ══════════════════════════════════════════════════════════════════════════
// COMPONENTES REUTILIZABLES DEL FORMULARIO
// ══════════════════════════════════════════════════════════════════════════

// ── Sección con título ────────────────────────────────────────────────────

@Composable
private fun FormSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
        // Encabezado de sección
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(Dimens.IconSM)
            )
            Text(
                text  = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NeonCyanDim,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.weight(1f))
            HorizontalDivider(
                modifier   = Modifier.weight(2f),
                color      = BorderSubtle,
                thickness  = Dimens.BorderThin
            )
        }

        GhostCard {
            content()
        }
    }
}

// ── Dropdown de método VPN ────────────────────────────────────────────────

@Composable
private fun MethodDropdown(
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text  = "Método de conexión",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = Dimens.SpaceXS)
        )
        Box {
            OutlinedButton(
                onClick  = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = InputFieldShape,
                colors   = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceVariant,
                    contentColor   = TextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    Dimens.BorderNormal, BorderNormal
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = selected.uppercase(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = TextTertiary
                    )
                }
            }

            DropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false },
                modifier         = Modifier.background(SurfaceVariant)
            ) {
                VPN_METHODS.forEach { method ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text  = method.uppercase(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (method == selected) NeonCyan else TextPrimary,
                                    fontWeight = if (method == selected) FontWeight.Bold
                                                 else FontWeight.Normal
                                )
                            )
                        },
                        onClick = { onSelect(method); expanded = false },
                        leadingIcon = if (method == selected) {
                            { Icon(Icons.Filled.Check, null, tint = NeonCyan, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

// ── Switch SSL ────────────────────────────────────────────────────────────

@Composable
private fun SslSwitch(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
        ) {
            Icon(
                imageVector = if (enabled) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = null,
                tint = if (enabled) NeonGreen else TextTertiary,
                modifier = Modifier.size(Dimens.IconSM)
            )
            Column {
                Text(
                    text  = "SSL Habilitado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text  = if (enabled) "Conexión cifrada (recomendado)" else "Sin cifrado SSL",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) NeonGreen else NeonAmber
                )
            }
        }
        Switch(
            checked         = enabled,
            onCheckedChange = onChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = TextOnAccent,
                checkedTrackColor   = NeonGreen,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = SurfaceElevated
            )
        )
    }
}

// ── Sección avanzada expandible ───────────────────────────────────────────

@Composable
private fun AdvancedSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    sni: String,
    proxyHost: String,
    proxyPort: String,
    proxyType: String,
    onSniChange: (String) -> Unit,
    onProxyHostChange: (String) -> Unit,
    onProxyPortChange: (String) -> Unit,
    onProxyTypeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
        // Header expandible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(SurfaceVariant)
                .border(Dimens.BorderNormal, BorderSubtle, MaterialTheme.shapes.medium)
                .clickable(onClick = onToggle)
                .padding(Dimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = NeonPurple,
                    modifier = Modifier.size(Dimens.IconSM)
                )
                Text(
                    text  = "OPCIONES AVANZADAS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonPurple,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = TextTertiary
            )
        }

        // Contenido expandible
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            GhostCard(borderColor = NeonPurple.copy(0.2f)) {
                // SNI
                GhostTextField(
                    value         = sni,
                    onValueChange = onSniChange,
                    label         = "SNI (Server Name Indication)",
                    placeholder   = "Ej: cloudflare.com"
                )

                Spacer(Modifier.height(Dimens.SpaceSM))

                // Subsección proxy
                Text(
                    text  = "Configuración de Proxy",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(vertical = Dimens.SpaceXS)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                    GhostTextField(
                        value         = proxyHost,
                        onValueChange = onProxyHostChange,
                        label         = "Proxy Host",
                        placeholder   = "127.0.0.1",
                        modifier      = Modifier.weight(2f)
                    )
                    GhostTextField(
                        value           = proxyPort,
                        onValueChange   = onProxyPortChange,
                        label           = "Puerto",
                        placeholder     = "8080",
                        modifier        = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(Modifier.height(Dimens.SpaceSM))

                // Tipo de proxy
                ProxyTypeSelector(
                    selected = proxyType,
                    onSelect = onProxyTypeChange
                )
            }
        }
    }
}

// ── Selector tipo de proxy ────────────────────────────────────────────────

@Composable
private fun ProxyTypeSelector(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(
            text  = "Tipo de Proxy",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = Dimens.SpaceXS)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            PROXY_TYPES.forEach { type ->
                val label = type.ifEmpty { "Ninguno" }
                val isSelected = type == selected
                FilterChip(
                    selected  = isSelected,
                    onClick   = { onSelect(type) },
                    label     = {
                        Text(
                            text  = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) TextOnAccent else TextSecondary
                        )
                    },
                    colors    = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPurple,
                        selectedLabelColor     = TextOnAccent,
                        containerColor         = SurfaceElevated,
                        labelColor             = TextSecondary
                    ),
                    border    = FilterChipDefaults.filterChipBorder(
                        enabled             = true,
                        selected            = isSelected,
                        borderColor         = BorderNormal,
                        selectedBorderColor = NeonPurple,
                        borderWidth         = Dimens.BorderNormal,
                        selectedBorderWidth = Dimens.BorderNormal
                    )
                )
            }
        }
    }
}

// ── Botones de acción ─────────────────────────────────────────────────────

@Composable
private fun ActionButtons(
    isSaving: Boolean,
    isEditMode: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        if (isSaving) {
            LinearProgressIndicator(
                modifier   = Modifier.fillMaxWidth(),
                color      = NeonCyan,
                trackColor = SurfaceElevated
            )
        }

        GhostButton(
            text     = if (isEditMode) "Guardar Cambios" else "Crear Perfil",
            onClick  = onSave,
            enabled  = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        GhostOutlineButton(
            text         = "Cancelar",
            onClick      = onCancel,
            borderColor  = BorderNormal,
            contentColor = TextSecondary,
            modifier     = Modifier.fillMaxWidth()
        )
    }
}
