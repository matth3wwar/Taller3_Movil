package com.example.taller3.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.taller3.R
import com.example.taller3.navigation.AppScreens
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val photoUrl: String? = null,
    val available: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableUsers(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    var users by remember { mutableStateOf<List<User>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("available", true)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val allAvailable = snapshot.toObjects(User::class.java)
                    // No mostrar al usuario actual
                    users = allAvailable.filter { it.uid != currentUid }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Users") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No other users available")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(users) { user ->
                    UserCard(user) {
                        // Al hacer clic, volvemos al mapa pasando el ID del usuario objetivo
                        navController.navigate(AppScreens.Home.name + "?targetUserId=${user.uid}") {
                            popUpTo(AppScreens.Home.name) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(user: User, onViewLocation: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val painter = if (!user.photoUrl.isNullOrEmpty()) {
                rememberAsyncImagePainter(user.photoUrl)
            } else {
                painterResource(id = R.drawable.contacticon)
            }

            Image(
                painter = painter,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(50.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${user.firstName} ${user.lastName}", style = MaterialTheme.typography.titleMedium)
            }
            
            Button(onClick = onViewLocation) {
                Text("View Location")
            }
        }
    }
}
