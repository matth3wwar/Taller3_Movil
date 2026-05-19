package com.example.taller3.screens

import com.example.taller3.R
import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.hardware.*
import android.location.Geocoder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.taller3.navigation.AppScreens
import com.example.taller3.utils.NotificationHelper
import com.google.accompanist.permissions.*
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint
import java.io.File
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermission(onPermissionGranted: @Composable () -> Unit) {
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    if (locationPermissionsState.allPermissionsGranted) {
        onPermissionGranted()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            val textToShow = if (locationPermissionsState.shouldShowRationale) {
                "The map needs location access to show where you are. Please grant the permission."
            } else {
                "Location permission is required to use the map features."
            }
            Text(textToShow, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                Text("Allow Location")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermission() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val permission = rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
        if (!permission.status.isGranted) {
            LaunchedEffect(Unit) { permission.launchPermissionRequest() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Maps(navController: NavController, targetUserId: String? = null) {
    NotificationPermission()
    
    LocationPermission {
        val context = LocalContext.current
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        var isDarkMode by remember { mutableStateOf(true) }
        var isAvailable by remember { mutableStateOf(false) }
        
        var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

        var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
        var showHistory by remember { mutableStateOf(false) }
        
        var targetUserLocation by remember { mutableStateOf<GeoPoint?>(null) }
        var targetUserName by remember { mutableStateOf("") }

        val notificationHelper = remember { NotificationHelper(context) }
        var firstLoad by remember { mutableStateOf(true) }

        val roadManager = OSRMRoadManager(context, "ANDROID")

        LaunchedEffect(uid) {
            if (uid != null) {
                db.collection("users")
                    .whereEqualTo("available", true)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.w("Firestore", "Listen failed.", e)
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            if (!firstLoad) {
                                for (dc in snapshot.documentChanges) {
                                    if (dc.type == DocumentChange.Type.ADDED) {
                                        val name = dc.document.getString("firstName") ?: "A user"
                                        val userUid = dc.document.id
                                        if (userUid != uid) {
                                            notificationHelper.showNotification(name)
                                        }
                                    }
                                }
                            }
                            firstLoad = false
                        }
                    }
            }
        }

        LaunchedEffect(uid) {
            uid?.let {
                db.collection("users").document(it).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.contains("available")) {
                            isAvailable = document.getBoolean("available") ?: false
                        }
                    }
            }
        }

        LaunchedEffect(targetUserId) {
            if (!targetUserId.isNullOrEmpty()) {
                db.collection("users").document(targetUserId)
                    .addSnapshotListener { snapshot, error ->
                        if (snapshot != null && snapshot.exists()) {
                            val loc = snapshot.getGeoPoint("lastLocation")
                            val name = snapshot.getString("firstName") ?: ""
                            targetUserName = name
                            if (loc != null) {
                                val newLoc = GeoPoint(loc.latitude, loc.longitude)
                                targetUserLocation = newLoc
                                mapViewInstance?.controller?.animateTo(newLoc)
                            }
                        }
                    }
            } else {
                targetUserLocation = null
                targetUserName = ""
            }
        }

        Location { geoPoint -> userLocation = geoPoint }

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val lux = event?.values?.get(0) ?: return
                isDarkMode = lux < 20f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        DisposableEffect(Unit) {
            sensorManager.registerListener(sensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
            onDispose { sensorManager.unregisterListener(sensorListener) }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Map") },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = if (isAvailable) "Available" else "Busy",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                modifier = Modifier.scale(0.7f),
                                checked = isAvailable,
                                onCheckedChange = { newValue ->
                                    isAvailable = newValue
                                    uid?.let {
                                        db.collection("users").document(it)
                                            .update("available", newValue)
                                            .addOnFailureListener { e ->
                                                Log.e("Firestore", "Error updating availability", e)
                                            }
                                    }
                                }
                            )
                        }

                        IconButton(onClick = {
                            navController.navigate(AppScreens.AvailableUsers.name)
                        }) {
                            Icon(Icons.Default.Face, contentDescription = "Available Users")
                        }

                        if (targetUserId != null) {
                            IconButton(onClick = {
                                navController.navigate(AppScreens.Home.name) {
                                    popUpTo(AppScreens.Home.name) { inclusive = true }
                                }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Stop tracking", tint = Color.Red)
                            }
                        }

                        IconButton(onClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate(AppScreens.Login.name) {
                                popUpTo(0) { inclusive = true }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        Configuration.getInstance().userAgentValue = context.packageName

                        val mapView = MapView(context)
                        mapView.setTileSource(TileSourceFactory.MAPNIK)
                        mapView.setMultiTouchControls(true)
                        mapView.controller.setZoom(16.0)

                        val start = GeoPoint(4.627293, -74.063228)
                        mapView.controller.setCenter(start)

                        val overlayEvents = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                if (p != null) {
                                    Toast.makeText(context, "Point selected", Toast.LENGTH_SHORT).show()
                                }
                                return true
                            }
                        })

                        mapView.overlays.add(overlayEvents)
                        mapViewInstance = mapView
                        mapView
                    },
                    update = { mapView ->
                        mapView.overlays.removeIf { it is Marker || it is Polyline }

                        val matrix = ColorMatrix().apply { setSaturation(0f) }
                        if (isDarkMode) {
                            matrix.postConcat(ColorMatrix(floatArrayOf(
                                -1f,0f,0f,0f,255f,
                                0f,-1f,0f,0f,255f,
                                0f,0f,-1f,0f,255f,
                                0f,0f,-1f,0f,255f,
                                0f,0f,0f,1f,0f
                            )))
                        }

                        mapView.overlayManager.tilesOverlay.setColorFilter(
                            ColorMatrixColorFilter(matrix)
                        )

                        val savedPoints = readLocations(context)
                        savedPoints.forEach { (point, name) ->
                            val marker = Marker(mapView)
                            marker.position = point
                            marker.title = name
                            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_marker_search)?.mutate()
                            drawable?.setTint(if (isDarkMode) android.graphics.Color.GREEN else android.graphics.Color.rgb(0, 150, 0))
                            marker.icon = drawable
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            mapView.overlays.add(marker)
                        }

                        userLocation?.let {
                            val marker = Marker(mapView)
                            marker.position = it
                            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_user_dot)?.mutate()
                            drawable?.setTint(if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
                            marker.icon = drawable
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            marker.title = "Me"
                            mapView.overlays.add(marker)
                            
                            if (targetUserId == null) {
                                mapView.controller.setCenter(it)
                            }
                        }

                        targetUserLocation?.let { targetLoc ->
                            val marker = Marker(mapView)
                            marker.position = targetLoc
                            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_marker_search)?.mutate()
                            drawable?.setTint(android.graphics.Color.CYAN)
                            marker.icon = drawable
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = targetUserName
                            
                            userLocation?.let { myLoc ->
                                val dist = distance(myLoc.latitude, myLoc.longitude, targetLoc.latitude, targetLoc.longitude)
                                marker.snippet = "Distance: ${"%.2f".format(dist)} m"
                                marker.showInfoWindow()
                            }
                            
                            mapView.overlays.add(marker)
                            mapView.controller.animateTo(targetLoc)
                        }

                        mapView.invalidate()
                    }
                )
            }
        }
    }
}

@Composable
fun Location(onLocationUpdate: (GeoPoint) -> Unit) {
    val context = LocalContext.current
    val client = remember { LocationServices.getFusedLocationProviderClient(context) }

    val callback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    onLocationUpdate(GeoPoint(location.latitude, location.longitude))
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        val db = FirebaseFirestore.getInstance()
                        val firebasePoint = FirestoreGeoPoint(location.latitude, location.longitude)
                        db.collection("users").document(uid)
                            .update("lastLocation", firebasePoint)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000).build()
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }
        onDispose { client.removeLocationUpdates(callback) }
    }
}

fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat1 - lat2)
    val dLon = Math.toRadians(lon1 - lon2)
    val a = Math.sin(dLat/2)*Math.sin(dLat/2) +
            Math.cos(Math.toRadians(lat1)) *
            Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon/2)*Math.sin(dLon/2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
    return 6371 * c * 1000
}

fun readLocations(context: Context): List<Pair<GeoPoint, String>> {
    return try {
        val inputStream = context.resources.openRawResource(R.raw.locations)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val rootObject = JSONObject(jsonString)
        val jsonArray = rootObject.getJSONArray("locationsArray")
        val list = mutableListOf<Pair<GeoPoint, String>>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val point = GeoPoint(obj.getDouble("latitude"), obj.getDouble("longitude"))
            val name = obj.getString("name")
            list.add(point to name)
        }
        list
    } catch (e: Exception) {
        Log.e("readLocations", "Error reading locations.json", e)
        emptyList()
    }
}
