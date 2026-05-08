package com.umariana.lscbridge.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun AdminTrainingScreen(
    viewModel: AdminTrainingViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val cameraExecutor: ExecutorService = remember {
        Executors.newSingleThreadExecutor()
    }
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            imageAnalysisBackpressureStrategy = STRATEGY_KEEP_ONLY_LATEST
            setImageAnalysisAnalyzer(
                cameraExecutor,
                AdminFrameAnalyzer(onFrame = viewModel::onFrame)
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    LaunchedEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraController.unbind()
            cameraExecutor.shutdown()
        }
    }

    val displayStatus = remember(uiState.statusMessage) {
        when {
            uiState.statusMessage.contains("hand_landmarker.task", ignoreCase = true) ->
                "No se pudo iniciar el detector de manos. Verifica los recursos del modelo."
            uiState.statusMessage.contains("dataset", ignoreCase = true) ->
                "Datos listos para captura."
            else -> uiState.statusMessage
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Modo administradora",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Captura ejemplos de gestos para mejorar el reconocimiento.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configuracion",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.currentLabel,
                    onValueChange = viewModel::onLabelChange,
                    label = { Text("Nombre del gesto (ej: hola)") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = if (uiState.isCapturing) "Capturando" else "En pausa"
                            )
                        }
                    )
                    Text(
                        text = "Muestras: ${uiState.samplesCaptured}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = viewModel::toggleCapture,
                enabled = uiState.detectorReady
            ) {
                Text(
                    text = if (uiState.isCapturing) {
                        "Detener captura"
                    } else {
                        "Iniciar captura"
                    }
                )
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    cameraController.cameraSelector =
                        if (cameraController.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Cambiar cámara"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Girar")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { saveCsvLocally(context) }
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Guardar CSV"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = { shareCsvToWhatsApp(context) }
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Enviar a WhatsApp"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = { deleteDataset(context) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Borrar CSV"
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Estado",
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider()
                Text(
                    text = displayStatus,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text(
            text = "Vista de camara",
            style = MaterialTheme.typography.titleSmall
        )
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    controller = cameraController
                }
            }
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Consejo: muestra la mano completa y mantén buena iluminación.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

private fun saveCsvLocally(context: Context) {
    val csvFile = File(context.filesDir, "training/gestures_dataset.csv")
    if (!csvFile.exists()) {
        Toast.makeText(context, "Aun no hay datos capturados", Toast.LENGTH_SHORT).show()
        return
    }
    Toast.makeText(context, "CSV guardado en: ${csvFile.absolutePath}", Toast.LENGTH_LONG).show()
}

private fun shareCsvToWhatsApp(context: Context) {
    val csvFile = File(context.filesDir, "training/gestures_dataset.csv")
    if (!csvFile.exists()) {
        Toast.makeText(context, "Aun no hay datos capturados", Toast.LENGTH_SHORT).show()
        return
    }

    val csvUri = FileProvider.getUriForFile(
        context,
        "com.umariana.lscbridge.fileprovider",
        csvFile
    )

    val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, csvUri)
        putExtra(Intent.EXTRA_SUBJECT, "Dataset CSV LSC")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setPackage("com.whatsapp")
    }

    if (whatsappIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(whatsappIntent)
    } else {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, csvUri)
            putExtra(Intent.EXTRA_SUBJECT, "Dataset CSV LSC")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir CSV"))
    }
}

private fun deleteDataset(context: Context) {
    val csvFile = File(context.filesDir, "training/gestures_dataset.csv")
    if (csvFile.exists()) {
        if (csvFile.delete()) {
            Toast.makeText(context, "Dataset borrado correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No se pudo borrar el archivo", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "El archivo no existe", Toast.LENGTH_SHORT).show()
    }
}

private class AdminFrameAnalyzer(
    private val onFrame: (ImageProxy) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        try {
            onFrame(image)
        } finally {
            image.close()
        }
    }
}
