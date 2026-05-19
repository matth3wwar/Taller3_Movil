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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taller3.R
import com.example.taller3.screens.*
import com.google.firebase.auth.FirebaseAuth

enum class AppScreens{
    Home,
    Login,
    Register,
    Account,
    AvailableUsers
}

@Composable
fun Navigation(auth: FirebaseAuth, startDestination: AppScreens){
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route?.split("?")?.get(0)

            val hideBottomBar = currentRoute == AppScreens.Login.name || 
                               currentRoute == AppScreens.Register.name ||
                               currentRoute == AppScreens.AvailableUsers.name

            if (!hideBottomBar) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination

                    AppScreens.entries.filter { 
                        it != AppScreens.Login && 
                        it != AppScreens.Register && 
                        it != AppScreens.AvailableUsers 
                    }.forEach { screen ->
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
                            selected = currentDestination?.hierarchy?.any { it.route?.startsWith(screen.name) == true } == true,
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
            composable(
                route = AppScreens.Home.name + "?targetUserId={targetUserId}",
                arguments = listOf(navArgument("targetUserId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val targetUserId = backStackEntry.arguments?.getString("targetUserId")
                Maps(navController, targetUserId)
            }
            composable(route= AppScreens.Login.name){
                Login(mAuth = auth, controller = navController)
            }
            composable(route= AppScreens.Register.name){
                Register(mAuth = auth, controller = navController)
            }
            composable(route= AppScreens.AvailableUsers.name){
                AvailableUsers(navController)
            }
        }
    }
}