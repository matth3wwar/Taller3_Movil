package com.example.taller3.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.taller3.R
import com.example.taller3.navigation.AppScreens
/*
@Composable
fun Home(controller: NavHostController){
    LocationPermission()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(
            modifier = Modifier.padding(10.dp).weight(33f),
            onClick = { controller.navigate(AppScreens.Contacts.name)}
        ) {
            Image(painter = painterResource(R.drawable.contact), contentDescription = "icono de contactos",modifier = Modifier.fillMaxSize())
        }
        ElevatedCard(
            modifier = Modifier.padding(10.dp).weight(33f),
            onClick = { controller.navigate(AppScreens.Login.name) }
        ) {
            Image(painter = painterResource(R.drawable.camera), contentDescription = "icono de camara",modifier = Modifier.fillMaxSize())
        }
        ElevatedCard(
            modifier = Modifier.padding(10.dp).weight(33f),
            onClick = { controller.navigate(AppScreens.Maps.name) }
        ) {
            Image(painter = painterResource(R.drawable.map), contentDescription = "icono de mapas",modifier = Modifier.fillMaxSize())
        }
    }
}
*/