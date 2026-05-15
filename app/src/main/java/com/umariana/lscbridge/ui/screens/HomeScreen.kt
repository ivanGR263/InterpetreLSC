package com.umariana.lscbridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onStartRecognition: () -> Unit,
    onOpenAdminTraining: () -> Unit
) {
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var adminUsername by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var adminLoginError by remember { mutableStateOf<String?>(null) }

    // Colores personalizados según el diseño solicitado
    val darkCharcoal = Color(0xFF121212)
    val softLavender = Color(0xFFE6E6FA)
    val purpleGlow = Color(0xFF2D1B4D)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(purpleGlow, darkCharcoal),
                    radius = 1500f
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                // Placeholder para el logo (dos manos entrelazadas formando un puente)
                Icon(
                    imageVector = Icons.Default.FrontHand,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Intérprete de ABC de LSC",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Main Action Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clickable { onStartRecognition() },
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = softLavender
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FrontHand, // Simulando "scanning hand"
                        contentDescription = null,
                        tint = darkCharcoal,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Iniciar reconocimiento en tiempo real",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = darkCharcoal
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Usa la cámara para interpretar LSC al instante",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = darkCharcoal.copy(alpha = 0.7f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Secondary Action
            OutlinedButton(
                onClick = {
                    showAdminLoginDialog = true
                    adminLoginError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Modo Administrador (Enseñar gestos)",
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = { showAdminLoginDialog = false },
            title = {
                Text(text = "Acceso administrador")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = adminUsername,
                        onValueChange = {
                            adminUsername = it
                            adminLoginError = null
                        },
                        label = { Text("Usuario") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = {
                            adminPassword = it
                            adminLoginError = null
                        },
                        label = { Text("Contrasena") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    adminLoginError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminUsername.trim().lowercase() == "cesmag" && adminPassword == "1234") {
                            showAdminLoginDialog = false
                            adminUsername = ""
                            adminPassword = ""
                            adminLoginError = null
                            onOpenAdminTraining()
                        } else {
                            adminLoginError = "Credenciales incorrectas"
                        }
                    }
                ) {
                    Text("Ingresar")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showAdminLoginDialog = false
                        adminLoginError = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
