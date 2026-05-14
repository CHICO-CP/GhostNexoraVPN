package com.ghostnexora.vpn.ui.screens.logs

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostnexora.vpn.ui.theme.*

@Composable
fun LogsScreen(
    onBack: () -> Unit,
    viewModel: LogsViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Registros") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Limpiar registros")
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
        ) {
            if (logs.isEmpty()) {
                EmptyLogsState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
                ) {
                    items(
                        items = logs,
                        key = { it.id }
                    ) { log ->
                        LogItem(log = log)
                    }

                    item {
                        Spacer(modifier = Modifier.height(Dimens.Space3XL))
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItem(log: com.ghostnexora.vpn.data.model.LogEntry) {
    GhostCard(
        backgroundColor = SurfaceVariant,
        borderColor = BorderSubtle,
        contentPadding = PaddingValues(Dimens.SpaceMD)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
            ) {
                // Level indicator
                val color = when (log.level.uppercase()) {
                    "ERROR" -> Color.Red
                    "WARN" -> NeonAmber
                    else -> NeonCyan
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(color)
                )

                Text(
                    text = log.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )

                Text(
                    text = log.level.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = color
                )

                Spacer(modifier = Modifier.weight(1f))

                if (log.tag.isNotEmpty()) {
                    Text(
                        text = log.tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = TextPrimary,
                overflow = TextOverflow.Visible
            )
        }
    }
}

@Composable
private fun EmptyLogsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXL)
        ) {
            Icon(
                Icons.Filled.Article,
                null,
                tint = TextTertiary,
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "No hay registros",
                style = MaterialTheme.typography.headlineSmall,
                color = TextSecondary
            )

            Text(
                text = "La actividad se mostrará aquí",
                style = MaterialTheme.typography.bodyLarge,
                color = TextTertiary
            )
        }
    }
}