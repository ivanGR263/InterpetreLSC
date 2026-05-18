package com.cesmag.nexing.ui.components

import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun PermissionStatusCard(hasPermission: Boolean) {
    Card {
        val status = if (hasPermission) "Permiso de cámara concedido" else "Permiso de cámara pendiente"
        Text(text = status)
    }
}
