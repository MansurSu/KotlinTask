package com.example.mycity

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mycity.pages.HomePage
import com.example.mycity.pages.LoginPage
import com.example.mycity.pages.SignupPage
import com.example.mycity.pages.WelcomePage

@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "welcome", modifier = modifier) {
        composable("welcome") { WelcomePage(modifier, navController) }
        composable("login")   { LoginPage(modifier, navController, authViewModel) }
        composable("signup")  { SignupPage(modifier, navController, authViewModel) }
        composable("home")    { HomePage(modifier, navController, authViewModel) }

        composable("addCity") {
            com.example.mycity.ui.screens.AddCityScreen(
                onCityAdded = {
                    navController.navigate("home") {
                        popUpTo("addCity") { inclusive = true }
                    }
                }
            )
        }
    }
}
