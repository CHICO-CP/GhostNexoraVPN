package com.ghostnexora.vpn.ui.screens.profiles

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostnexora.vpn.data.model.VpnProfile
import com.ghostnexora.vpn.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════
// PROFILE LIST SCREEN
// ══════════════════════════════════════════════════════════════════════════

@Composable
fun ProfileListScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: ProfileListViewModel = hiltViewModel()
) {
    val profiles        by viewModel.profiles.collectAsStateWithLifecycle()
    val uiState         by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery     by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter    by viewModel.activeFilter.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Diálogo de confirmación de borrado
    if (uiState.profileToDelete != null) {
        DeleteProfileDialog(
            profileName = uiState.profileToDelete!!.name,
            onConfirm   = { viewModel.confirmDelete() },
            onDismiss   = { viewModel.dismissDelete() }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick          = onNavigateToCreate,
                containerColor   = NeonCyan,
                contentColor     = TextOnAccent,
                shape            = CircleShape,
                modifier         = Modifier.neonGlow(NeonCyan, radius = 16.dp, alpha = 0.35f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Crear perfil",
                    modifier = Modifier.size(Dimens.IconLG)
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = SurfaceElevated,
                    contentColor   = TextPrimary,
                    shape          = SnackbarShape
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(padding)
        ) {
            // ── Barra de búsqueda ──────────────────────────────────────────
            SearchBar(
                query    = searchQuery,
                onChange = viewModel::onSearchQueryChange,
                onClear  = viewModel::clearSearch
            )

            // ── Chips de filtro ────────────────────────────────────────────
            FilterChipRow(
                activeFilter = activeFilter,
                onSelect     = viewModel::setFilter
            )

            // ── Lista de perfiles ──────────────────────────────────────────
            if (profiles.isEmpty()) {
                ProfileEmptyState(
                    isFiltered = searchQuery.isNotBlank() || activeFilter != ProfileFilter.ALL,
                    onCreate   = onNavigateToCreate
                )
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(
                        start  = Dimens.ScreenPadding,
                        end    = Dimens.ScreenPadding,
                        top    = Dimens.SpaceSM,
                        bottom = Dimens.Space4XL
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ProfileCardSpacing)
                ) {
                    // Contador
                    item {
                        Text(
                            text  = "${profiles.size} perfil(es)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextTertiary,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(vertical = Dimens.SpaceXS)
                        )
                    }

                    items(profiles, key = { it.id }) { profile ->
                        ProfileCard(
                            profile         = profile,
                            isActive        = profile.id == activeProfileId,
                            onSelect        = { viewModel.selectActiveProfile(profile.id) },
                            onEdit          = { onNavigateToEdit(profile.id) },
                            onDelete        = { viewModel.requestDelete(profile) },
                            onToggleFavorite = { viewModel.toggleFavorite(profile) }
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// BARRA DE BÚSQUEDA
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun SearchBar(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value         = query,
            onValueChange = onChange,
            placeholder   = { Text("Buscar perfiles…", color = TextTertiary) },
            leadingIcon   = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = if (query.isNotBlank()) NeonCyan else TextTertiary
                )
            },
            trailingIcon  = if (query.isNotBlank()) {
                {
                    IconButton(onClick = onClear) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Limpiar",
                            tint = TextTertiary
                        )
                    }
                }
            } else null,
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = InputFieldShape,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor    = NeonCyan,
                unfocusedBorderColor  = BorderNormal,
                focusedTextColor      = TextPrimary,
                unfocusedTextColor    = TextPrimary,
                cursorColor           = NeonCyan,
                focusedContainerColor = SurfaceVariant,
                unfocusedContainerColor = SurfaceVariant
            )
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════
// CHIPS DE FILTRO
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun FilterChipRow(
    activeFilter: ProfileFilter,
    onSelect: (ProfileFilter) -> Unit
) {
    LazyRow(
        contentPadding      = PaddingValues(horizontal = Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        modifier            = Modifier.padding(bottom = Dimens.SpaceSM)
    ) {
        items(ProfileFilter.entries) { filter ->
            val isSelected = filter == activeFilter
            FilterChip(
                selected  = isSelected,
                onClick   = { onSelect(filter) },
                label     = {
                    Text(
                        text  = filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) TextOnAccent else TextSecondary
                    )
                },
                leadingIcon = when (filter) {
                    ProfileFilter.ALL       -> null
                    ProfileFilter.FAVORITES -> {
                        { Icon(Icons.Filled.Star, null, Modifier.size(14.dp)) }
                    }
                    ProfileFilter.ENABLED   -> {
                        { Icon(Icons.Filled.CheckCircle, null, Modifier.size(14.dp)) }
                    }
                },
                colors    = FilterChipDefaults.filterChipColors(
                    selectedContainerColor      = NeonCyan,
                    selectedLabelColor          = TextOnAccent,
                    selectedLeadingIconColor     = TextOnAccent,
                    containerColor              = SurfaceVariant,
                    labelColor                  = TextSecondary
                ),
                border    = FilterChipDefaults.filterChipBorder(
                    enabled         = true,
                    selected        = isSelected,
                    borderColor     = BorderNormal,
                    selectedBorderColor = NeonCyan,
                    borderWidth     = Dimens.BorderNormal,
                    selectedBorderWidth = Dimens.BorderNormal
                )
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// TARJETA DE PERFIL
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileCard(
    profile: VpnProfile,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val borderColor = when {
        isActive -> NeonCyan.copy(alpha = 0.6f)
        else     -> BorderSubtle
    }
    val bgColor = when {
        isActive -> NeonCyan.copy(alpha = 0.06f)
        else     -> SurfaceVariant
    }

    var expanded by remember { mutableStateOf(false) }

    GhostCard(
        borderColor     = borderColor,
        backgroundColor = bgColor,
        glowColor       = if (isActive) NeonCyan else null,
        contentPadding  = PaddingValues(0.dp),
        modifier        = Modifier.fillMaxWidth()
    ) {
        Column {
            // ── Fila principal ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(Dimens.SpaceMD),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
            ) {
                // Ícono con badge activo
                Box {
                    Box(
                        modifier = Modifier
                            .size(Dimens.ProfileIconSize)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (isActive) NeonCyan.copy(0.15f)
                                else SurfaceElevated
                            )
                            .border(
                                Dimens.BorderNormal,
                                if (isActive) NeonCyan.copy(0.5f) else BorderNormal,
                                MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VpnKey,
                            contentDescription = null,
                            tint = if (isActive) NeonCyan else TextTertiary,
                            modifier = Modifier.size(Dimens.IconMD)
                        )
                    }
                    // Dot activo
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                                .border(2.dp, SurfaceVariant, CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }
                }

                // Info del perfil
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)
                    ) {
                        Text(
                            text     = profile.name.ifEmpty { "Sin nombre" },
                            style    = MaterialTheme.typography.titleSmall.copy(
                                color = if (isActive) TextPrimary else TextSecondary,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Badge activo
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .clip(TagShape)
                                    .background(NeonCyan.copy(0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text  = "ACTIVO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonCyan,
                                        fontSize = 9.sp,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text  = "${profile.host}:${profile.port}",
                        style = MonoStyle.copy(fontSize = 11.sp, color = TextTertiary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Método + SSL
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = profile.method.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonBlue,
                                letterSpacing = 1.sp
                            )
                        )
                        if (profile.sslEnabled) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "SSL",
                                tint = NeonGreen,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text  = "SSL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NeonGreen,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Favorito
                IconButton(
                    onClick  = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (profile.isFavorite) Icons.Filled.Star
                                      else Icons.Filled.StarBorder,
                        contentDescription = "Favorito",
                        tint = if (profile.isFavorite) NeonAmber else TextTertiary,
                        modifier = Modifier.size(Dimens.IconSM)
                    )
                }

                // Expandir / colapsar
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess
                                  else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(Dimens.IconSM)
                )
            }

            // ── Tags (si hay) ──────────────────────────────────────────────
            if (profile.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Dimens.SpaceMD + Dimens.ProfileIconSize + Dimens.SpaceMD,
                                 end   = Dimens.SpaceMD,
                                 bottom = Dimens.SpaceSM),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)
                ) {
                    profile.tags.take(4).forEach { tag ->
                        ProfileTagChip(tag)
                    }
                }
            }

            // ── Acciones expandidas ────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Column {
                    NeonDivider(
                        modifier = Modifier.padding(horizontal = Dimens.SpaceMD),
                        color    = BorderSubtle
                    )
                    // Notas
                    if (profile.notes.isNotBlank()) {
                        Text(
                            text     = profile.notes,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = TextTertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(
                                horizontal = Dimens.SpaceMD,
                                vertical   = Dimens.SpaceSM
                            )
                        )
                    }
                    // Botones de acción
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = Dimens.SpaceMD,
                                vertical   = Dimens.SpaceSM
                            ),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
                    ) {
                        // Seleccionar como activo
                        if (!isActive) {
                            GhostButton(
                                text     = "Usar",
                                onClick  = onSelect,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Editar
                        GhostOutlineButton(
                            text    = "Editar",
                            onClick = onEdit,
                            modifier = Modifier.weight(1f)
                        )
                        // Eliminar
                        GhostOutlineButton(
                            text         = "Eliminar",
                            onClick      = onDelete,
                            borderColor  = NeonRed.copy(0.5f),
                            contentColor = NeonRed,
                            modifier     = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// ESTADO VACÍO
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileEmptyState(
    isFiltered: Boolean,
    onCreate: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
            modifier = Modifier.padding(Dimens.Space2XL)
        ) {
            Icon(
                imageVector = if (isFiltered) Icons.Filled.SearchOff
                              else Icons.Filled.VpnKeyOff,
                contentDescription = null,
                tint     = TextTertiary,
                modifier = Modifier.size(Dimens.Space4XL)
            )
            Text(
                text  = if (isFiltered) "Sin resultados" else "Sin perfiles",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Text(
                text  = if (isFiltered) "Prueba con otra búsqueda o filtro"
                        else "Crea tu primer perfil o importa uno",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
            if (!isFiltered) {
                Spacer(Modifier.height(Dimens.SpaceSM))
                GhostButton(
                    text    = "Crear Perfil",
                    onClick = onCreate,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// DIÁLOGO DE CONFIRMACIÓN DE BORRADO
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun DeleteProfileDialog(
    profileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = SurfaceVariant,
        titleContentColor = TextPrimary,
        textContentColor  = TextSecondary,
        icon = {
            Icon(
                imageVector = Icons.Filled.DeleteForever,
                contentDescription = null,
                tint = NeonRed,
                modifier = Modifier.size(Dimens.IconXL)
            )
        },
        title = {
            Text(
                text  = "Eliminar perfil",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Text(
                text  = "¿Eliminar \"$profileName\"? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text  = "Eliminar",
                    color = NeonRed,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
