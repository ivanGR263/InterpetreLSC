package com.cesmag.nexing.ui.navigation

sealed class Destinations(val route: String) {
    data object Home : Destinations("home")
    data object Camera : Destinations("camera")
    data object AdminTraining : Destinations("admin_training")
}
