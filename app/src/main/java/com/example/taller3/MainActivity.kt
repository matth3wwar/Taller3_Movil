package com.example.taller3

import android.location.Geocoder
import org.osmdroid.config.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import com.example.taller3.navigation.AppScreens
import com.example.taller3.navigation.Navigation
import com.example.taller3.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {

    private val mAuth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val policy =
            StrictMode.ThreadPolicy.Builder().permitAll().build()

        StrictMode.setThreadPolicy(policy)
        Configuration.getInstance().userAgentValue = "AndroidApp"
        enableEdgeToEdge()
        setContent {
            val currentUser = remember { mAuth.currentUser }
            
            // Suscripción Global de Disponibilidad
            GlobalAvailabilityTracker(currentUser?.uid)
            
            Navigation(mAuth, AppScreens.Login)
        }
    }
}

@Composable
fun GlobalAvailabilityTracker(uid: String?) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val notificationHelper = remember { NotificationHelper(context) }
    var firstLoad by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (!firstLoad) {
                        for (dc in snapshot.documentChanges) {
                            // Notificar en caso de cambio de estado o nuevo usuario disponible
                            if (dc.type == DocumentChange.Type.MODIFIED) {
                                val name = dc.document.getString("firstName") ?: "A user"
                                val isAvailable = dc.document.getBoolean("available") ?: false
                                val userUid = dc.document.id
                                
                                if (userUid != uid) {
                                    val statusText = if (isAvailable) "available" else "not available"
                                    notificationHelper.showNotification("$name is now $statusText")
                                }
                            }
                        }
                    }
                    firstLoad = false
                }
            }
        }
    }
}
