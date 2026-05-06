package com.umariana.lscbridge.ui.screens

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraRecognitionScreen(
    viewModel: CameraRecognitionViewModel = viewModel()
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
                FrameAnalyzer(onFrame = viewModel::onFrameAnalyzed)
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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    controller = cameraController
                }
            }
        )

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Texto detectado: ${uiState.recognizedText}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
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
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Girar")
                    }
                }
                Text(
                    text = "Confianza: ${(uiState.confidence * 100f).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Frames analizados: ${uiState.framesAnalyzed}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private class FrameAnalyzer(
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
