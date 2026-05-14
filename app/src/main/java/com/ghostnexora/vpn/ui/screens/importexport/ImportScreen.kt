package com.ghostnexora.vpn.ui.screens.importexport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostnexora.vpn.ui.theme.*

@Composable
fun ImportScreen(
    onBack: () -> Unit,
    viewModel: ImportExportViewModel = hiltViewModel()
) {
    val state = viewModel.importState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }

    // Mensajes de éxito
    LaunchedEffect(state.importSuccess) {
        if (state.importSuccess) {
            snackbarHostState.showSnackbar(
                message = "${state.importedCount} perfil(es) importado(s) correctamente"
            )
            viewModel.clearImportMessage()
        }
    }

    // Errores
    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearImportMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Importar Perfiles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(padding)
                .padding(Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)
        ) {
            // Header
            GhostCard(
                backgroundColor = NeonCyan.copy(0.25f),
                borderColor = NeonCyan,
                contentColor = NeonCyan.copy(0.05f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(NeonCyan.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.FileDownload,
                            null,
                            tint = NeonCyan,
                            modifier = Modifier.size(Dimens.IconXXL)
                        )
                    }

                    Text(
                        "Importar Perfiles",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        "Selecciona un archivo JSON\nexportado previamente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Botón principal de importar
            GhostButton(
                text = if (state.isLoading) "Importando..." else "Seleccionar Archivo JSON",
                onClick = { viewModel.importFromFile() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                containerColor = NeonCyan,
                contentColor = TextOnAccent,
                icon = Icons.Filled.UploadFile
            )

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonAmber
                )
            }

            // Estado después de importar
            if (state.importedCount > 0 || state.error != null) {
                GhostCard(
                    backgroundColor = SurfaceVariant,
                    borderColor = BorderNormal
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.SpaceXL),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.importedCount > 0) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                null,
                                tint = NeonGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "${state.importedCount} perfiles importados",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonGreen
                            )
                        }

                        if (state.error != null) {
                            Icon(
                                Icons.Filled.Error,
                                null,
                                tint = Color.Red,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                state.error,
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Información
            Text(
                text = "Solo se aceptan archivos JSON generados por GhostNexoraVPN",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Dimens.SpaceXL)
            )

            Spacer(modifier = Modifier.height(Dimens.Space3XL))
        }
    }
}