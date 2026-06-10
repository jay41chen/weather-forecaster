package com.weather.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.weather.feature.citylist.CityListScreen
import com.weather.feature.weather.WeatherScreen

@Composable
fun WeatherNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "weather") {
        composable("weather") {
            WeatherScreen(
                onNavigateToCityList = { navController.navigate("city_list") }
            )
        }
        composable("city_list") {
            CityListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
