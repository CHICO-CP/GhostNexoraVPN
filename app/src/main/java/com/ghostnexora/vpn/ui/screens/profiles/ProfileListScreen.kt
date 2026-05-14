package com.ghostnexora.vpn.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostnexora.vpn.data.model.VpnProfile
import com.ghostnexora.vpn.ui.theme.*

@Composable
fun ProfileListScreen(
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    onEditProfile: (VpnProfile) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    // Delete confirmation dialog
    var profileToDelete by remember { mutableStateOf<VpnProfile?>(null) }

    if (profileToDelete != null) {
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Eliminar Perfil") },
            text = {
                Text("¿Estás seguro de que deseas eliminar '${profileToDelete?.name}'?\nEsta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileToDelete?.let { viewModel.deleteProfile(it) }
                        profileToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mis Perfiles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateNew) {
                        Icon(Icons.Filled.Add, contentDescription = "Nuevo Perfil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNew,
                containerColor = NeonCyan,
                contentColor = TextOnAccent
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo")
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(padding)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
        ) {
            if (profiles.isEmpty()) {
                item {
                    EmptyProfilesState(onCreateNew = onCreateNew)
                }
            } else {
                items(
                    items = profiles,
                    key = { it.id }
                ) { profile ->
                    ProfileItem(
                        profile = profile,
                        onEdit = { onEditProfile(profile) },
                        onDelete = { profileToDelete = profile }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(Dimens.Space3XL)) }
        }
    }
}

@Composable
private fun ProfileItem(
    profile: VpnProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GhostCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = BorderSubtle,
        borderColor = null,
        padding = PaddingValues(Dimens.SpaceMD)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
        ) {
            // VPN Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(NeonCyan.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.VpnKey,
                    null,
                    tint = NeonCyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Profile Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name.ifEmpty { "Perfil sin nombre" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "\( {profile.host}: \){profile.port}",
                    style = MonoStyle.copy(color = TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (profile.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = Dimens.SpaceXS),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)
                    ) {
                        profile.tags.take(3).forEach { tag ->
                            ProfileTagChip(tag)
                        }
                    }
                }
            }

            // Action Buttons
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Editar",
                        tint = NeonCyan
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Eliminar",
                        tint = Color.Red.copy(0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyProfilesState(onCreateNew: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.Space4XL),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXL)
        ) {
            Icon(
                Icons.Filled.VpnKey,
                null,
                tint = TextTertiary,
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = "No tienes perfiles aún",
                style = MaterialTheme.typography.headlineSmall,
                color = TextSecondary
            )

            Text(
                text = "Crea tu primer perfil para comenzar\na usar la VPN",
                style = MaterialTheme.typography.bodyLarge,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )

            GhostButton(
                text = "Crear Primer Perfil",
                onClick = onCreateNew,
                containerColor = NeonCyan,
                contentColor = TextOnAccent,
                icon = Icons.Filled.Add
            )
        }
    }
}