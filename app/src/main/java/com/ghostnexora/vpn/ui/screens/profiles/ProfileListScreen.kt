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

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.profileToDelete != null) {
        DeleteProfileDialog(
            profileName = uiState.profileToDelete!!.name,
            onConfirm   = { viewModel.confirmDelete() },
            onDismiss   = { viewModel.dismissDelete() }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate, containerColor = NeonCyan, contentColor = TextOnAccent, shape = CircleShape) {
                Icon(Icons.Filled.Add, "Crear perfil", modifier = Modifier.size(Dimens.IconLG))
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = SurfaceElevated, contentColor = TextPrimary, shape = SnackbarShape)
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(Modifier.fillMaxSize().background(BackgroundDark).padding(padding)) {
            // Search
            Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSM), Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Buscar perfiles...", color = TextTertiary) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = if (searchQuery.isNotBlank()) NeonCyan else TextTertiary) },
                    trailingIcon = if (searchQuery.isNotBlank()) { { IconButton(onClick = viewModel::clearSearch) { Icon(Icons.Filled.Close, null, tint = TextTertiary) } } } else null,
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = InputFieldShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = BorderNormal, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = NeonCyan, focusedContainerColor = SurfaceVariant, unfocusedContainerColor = SurfaceVariant)
                )
            }
            // Filters
            LazyRow(contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM), modifier = Modifier.padding(bottom = Dimens.SpaceSM)) {
                items(ProfileFilter.entries) { filter ->
                    val isSelected = filter == activeFilter
                    FilterChip(
                        selected = isSelected, onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.label, style = MaterialTheme.typography.labelMedium, color = if (isSelected) TextOnAccent else TextSecondary) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonCyan, selectedLabelColor = TextOnAccent, containerColor = SurfaceVariant, labelColor = TextSecondary),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = BorderNormal, selectedBorderColor = NeonCyan, borderWidth = Dimens.BorderNormal, selectedBorderWidth = Dimens.BorderNormal)
                    )
                }
            }
            // List
            if (profiles.isEmpty()) {
                ProfileEmptyState(isFiltered = searchQuery.isNotBlank() || activeFilter != ProfileFilter.ALL, onCreate = onNavigateToCreate)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = Dimens.SpaceSM, bottom = Dimens.Space4XL),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ProfileCardSpacing)
                ) {
                    item {
                        Text("${profiles.size} perfil(es)", style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, letterSpacing = 1.sp), modifier = Modifier.padding(vertical = Dimens.SpaceXS))
                    }
                    items(items = profiles, key = { profile -> profile.id }) { profile ->
                        ProfileCard(
                            profile          = profile,
                            isActive         = profile.id == activeProfileId,
                            onSelect         = { viewModel.selectActiveProfile(profile.id) },
                            onEdit           = { onNavigateToEdit(profile.id) },
                            onDelete         = { viewModel.requestDelete(profile) },
                            onToggleFavorite = { viewModel.toggleFavorite(profile) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: VpnProfile, isActive: Boolean, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onToggleFavorite: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val borderColor = if (isActive) NeonCyan.copy(0.6f) else BorderSubtle
    val bgColor = if (isActive) NeonCyan.copy(0.06f) else SurfaceVariant
    GhostCard(borderColor = borderColor, backgroundColor = bgColor, glowColor = if (isActive) NeonCyan else null, contentPadding = PaddingValues(0.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(Dimens.SpaceMD), Alignment.CenterVertically, Arrangement.spacedBy(Dimens.SpaceMD)) {
                Box {
                    Box(Modifier.size(Dimens.ProfileIconSize).clip(MaterialTheme.shapes.medium).background(if (isActive) NeonCyan.copy(0.15f) else SurfaceElevated).border(Dimens.BorderNormal, if (isActive) NeonCyan.copy(0.5f) else BorderNormal, MaterialTheme.shapes.medium), Alignment.Center) {
                        Icon(Icons.Filled.VpnKey, null, tint = if (isActive) NeonCyan else TextTertiary, modifier = Modifier.size(Dimens.IconMD))
                    }
                    if (isActive) Box(Modifier.size(10.dp).clip(CircleShape).background(NeonGreen).border(2.dp, SurfaceVariant, CircleShape).align(Alignment.TopEnd))
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)) {
                        Text(profile.name.ifEmpty { "Sin nombre" }, style = MaterialTheme.typography.titleSmall.copy(color = if (isActive) TextPrimary else TextSecondary, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (isActive) Box(Modifier.clip(TagShape).background(NeonCyan.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("ACTIVO", style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontSize = 9.sp, letterSpacing = 1.sp)) }
                    }
                    Text("${profile.host}:${profile.port}", style = MonoStyle.copy(11.sp, color = TextTertiary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS), verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.method.uppercase(), style = MaterialTheme.typography.labelSmall.copy(color = NeonBlue, letterSpacing = 1.sp))
                        if (profile.sslEnabled) { Icon(Icons.Filled.Lock, "SSL", tint = NeonGreen, modifier = Modifier.size(10.dp)); Text("SSL", style = MaterialTheme.typography.labelSmall.copy(color = NeonGreen, fontSize = 10.sp)) }
                    }
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(if (profile.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder, "Favorito", tint = if (profile.isFavorite) NeonAmber else TextTertiary, modifier = Modifier.size(Dimens.IconSM))
                }
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = TextTertiary, modifier = Modifier.size(Dimens.IconSM))
            }
            if (profile.tags.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(start = Dimens.SpaceMD + Dimens.ProfileIconSize + Dimens.SpaceMD, end = Dimens.SpaceMD, bottom = Dimens.SpaceSM), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)) {
                    profile.tags.take(4).forEach { ProfileTagChip(it) }
                }
            }
            AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    NeonDivider(Modifier.padding(horizontal = Dimens.SpaceMD))
                    if (profile.notes.isNotBlank()) Text(profile.notes, style = MaterialTheme.typography.bodySmall, color = TextTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM))
                    Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                        if (!isActive) GhostButton("Usar", onSelect, Modifier.weight(1f))
                        GhostOutlineButton("Editar", onEdit, Modifier.weight(1f))
                        GhostOutlineButton("Eliminar", onDelete, Modifier.weight(1f), borderColor = NeonRed.copy(0.5f), contentColor = NeonRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEmptyState(isFiltered: Boolean, onCreate: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(Alignment.CenterHorizontally, Arrangement.spacedBy(Dimens.SpaceMD), Modifier.padding(Dimens.Space2XL)) {
            Icon(if (isFiltered) Icons.Filled.SearchOff else Icons.Filled.VpnKeyOff, null, tint = TextTertiary, modifier = Modifier.size(Dimens.Space4XL))
            Text(if (isFiltered) "Sin resultados" else "Sin perfiles", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Text(if (isFiltered) "Prueba con otra busqueda o filtro" else "Crea tu primer perfil o importa uno", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            if (!isFiltered) { Spacer(Modifier.height(Dimens.SpaceSM)); GhostButton("Crear Perfil", onCreate, Modifier.fillMaxWidth(0.6f)) }
        }
    }
}

@Composable
private fun DeleteProfileDialog(profileName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = SurfaceVariant, titleContentColor = TextPrimary, textContentColor = TextSecondary,
        icon = { Icon(Icons.Filled.DeleteForever, null, tint = NeonRed, modifier = Modifier.size(Dimens.IconXL)) },
        title = { Text("Eliminar perfil", fontWeight = FontWeight.Bold) },
        text = { Text("Eliminar \"$profileName\"? Esta accion no se puede deshacer.", style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onConfirm) { Text("Eliminar", color = NeonRed, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar", color = TextSecondary) } }
    )
}
