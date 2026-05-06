package com.umariana.lscbridge.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.umariana.lscbridge.ui.screens.AdminTrainingScreen
import com.umariana.lscbridge.ui.screens.CameraRecognitionScreen
import com.umariana.lscbridge.ui.screens.HomeScreen
import com.umariana.lscbridge.ui.screens.PermissionRequiredScreen

@Composable
fun LSCBridgeNavGraph(
    navController: NavHostController,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.Home.route,
        modifier = modifier
    ) {
        composable(Destinations.Home.route) {
            HomeScreen(
                onStartRecognition = {
                    navController.navigate(Destinations.Camera.route)
                },
                onOpenAdminTraining = {
                    navController.navigate(Destinations.AdminTraining.route)
                }
            )
        }
        composable(Destinations.Camera.route) {
            if (hasCameraPermission) {
                CameraRecognitionScreen()
            } else {
                PermissionRequiredScreen(
                    onRequestPermission = onRequestCameraPermission
                )
            }
        }
        composable(Destinations.AdminTraining.route) {
            if (hasCameraPermission) {
                AdminTrainingScreen()
            } else {
                PermissionRequiredScreen(
                    onRequestPermission = onRequestCameraPermission
                )
            }
        }
    }
}
