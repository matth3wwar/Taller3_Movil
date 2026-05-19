package com.example.taller3.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.taller3.R
import com.example.taller3.screens.Account
import com.example.taller3.screens.Camera
import com.example.taller3.screens.Contacts
import com.example.taller3.screens.Login
import com.example.taller3.screens.Maps
import com.example.taller3.screens.Register
import com.google.firebase.auth.FirebaseAuth

enum class AppScreens{
    Home,
    Login,
    Register,
    Account
}

@Composable
fun Navigation(auth: FirebaseAuth, startDestination: AppScreens){
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Define which screens should NOT show the bar
            val hideBottomBar = currentRoute == AppScreens.Login.name || currentRoute == AppScreens.Register.name

            if (!hideBottomBar) {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    // Iterate through your screens to create items
                    AppScreens.entries.filter { it != AppScreens.Login && it != AppScreens.Register }
                        .forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                when (screen) {
                                    AppScreens.Home -> Icon(
                                        Icons.Default.Home,
                                        contentDescription = null
                                    )

                                    AppScreens.Account -> Icon(
                                        Icons.Default.Person,
                                        contentDescription = null
                                    )

                                    else -> Icon(Icons.Default.Home, contentDescription = null)
                                }
                            },
                            label = { Text(screen.name) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.name } == true,
                            onClick = {
                                navController.navigate(screen.name) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController, 
            startDestination = startDestination.name,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(route= AppScreens.Account.name){
                Account(mAuth = auth, controller = navController)
            }
            composable(route= AppScreens.Home.name){
                Maps(navController)
            }
            composable(route= AppScreens.Login.name){
                Login(mAuth = auth, controller = navController)
            }
            composable(route= AppScreens.Register.name){
                Register(mAuth = auth, controller = navController)
            }
        }
    }
}