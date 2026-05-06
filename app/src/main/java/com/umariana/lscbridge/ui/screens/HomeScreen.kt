package com.umariana.lscbridge.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onStartRecognition: () -> Unit,
    onOpenAdminTraining: () -> Unit
) {
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var adminUsername by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var adminLoginError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "LSC Bridge — Módulo 2",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onStartRecognition) {
                Text(text = "Iniciar reconocimiento en tiempo real")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    showAdminLoginDialog = true
                    adminLoginError = null
                }
            ) {
                Text(text = "Modo administradora (enseñar gestos)")
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
