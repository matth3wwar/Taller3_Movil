package com.example.taller3

import android.location.Geocoder
import org.osmdroid.config.Configuration
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import com.example.taller3.navigation.AppScreens
import com.example.taller3.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
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
            Navigation(mAuth, AppScreens.Login)
        }
        //lateinit var geocoder: Geocoder
        //geocoder = Geocoder(this)
    }
}