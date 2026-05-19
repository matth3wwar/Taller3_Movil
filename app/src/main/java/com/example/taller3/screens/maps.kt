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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.taller3.navigation.AppScreens
import com.google.accompanist.permissions.*
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
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

val locations = mutableListOf<JSONObject>()

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermission() {
    val permission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    if (!permission.status.isGranted) {
        LaunchedEffect(Unit) { permission.launchPermissionRequest() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Maps(navController: NavController) {
    LocationPermission()
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    var isDarkMode by remember { mutableStateOf(true) }
    var isAvailable by remember { mutableStateOf(false) }

    // Cargar estado inicial de disponibilidad
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

    // Mantenemos estos estados para la funcionalidad de clics largos y ubicación del usuario
    var searchedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var longClickLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    val roadManager = OSRMRoadManager(context, "ANDROID")

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
                    // Toggle de disponibilidad
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isAvailable) "Available" else "Not visible",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
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
        // Usamos un Box para que el mapa sea el fondo de toda la pantalla
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(), // El mapa ahora ocupa todo el tamaño
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
                                longClickLocation = GeoPoint(p.latitude, p.longitude)
                                showHistory = true
                                Toast.makeText(context, "Point selected", Toast.LENGTH_SHORT).show()
                            }
                            return true
                        }
                    })

                    mapView.overlays.add(overlayEvents)
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
                            0f,0f,0f,1f,0f
                        )))
                    }

                    mapView.overlayManager.tilesOverlay.setColorFilter(
                        ColorMatrixColorFilter(matrix)
                    )

                    val savedPoints = readLocations(context)
                    savedPoints.forEach { point ->
                        val marker = Marker(mapView)
                        marker.position = point
                        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_marker_search)?.mutate()
                        // Color distintivo para puntos guardados (Verde)
                        drawable?.setTint(if (isDarkMode) android.graphics.Color.GREEN else android.graphics.Color.rgb(0, 150, 0))
                        marker.icon = drawable
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        mapView.overlays.add(marker)
                    }

                    // Renderizado de usuario, marcadores y rutas (se mantiene igual)
                    userLocation?.let {
                        val marker = Marker(mapView)
                        marker.position = it
                        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_user_dot)?.mutate()
                        drawable?.setTint(if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
                        marker.icon = drawable
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        mapView.overlays.add(marker)
                        mapView.controller.setCenter(it)
                    }

                    longClickLocation?.let {
                        val marker = Marker(mapView)
                        marker.position = it
                        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_marker_click)?.mutate()
                        drawable?.setTint(if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
                        marker.icon = drawable
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        mapView.overlays.add(marker)
                    }

                    if (userLocation != null && longClickLocation != null) {
                        val points = arrayListOf(userLocation!!, longClickLocation!!)
                        val road = roadManager.getRoad(points)
                        val roadOverlay = RoadManager.buildRoadOverlay(road)
                        roadOverlay.outlinePaint.strokeWidth = 8f
                        roadOverlay.outlinePaint.color = android.graphics.Color.RED
                        mapView.overlays.add(roadOverlay)
                    }

                    mapView.invalidate()
                }
            )
        }
    }
}

// ---------------- LOCATION ----------------
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Location(onLocationUpdate: (GeoPoint) -> Unit) {

    val context = LocalContext.current
    val client = LocationServices.getFusedLocationProviderClient(context)

    val permission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) { permission.launchPermissionRequest() }

    if (permission.status.isGranted) {

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000
        ).setMinUpdateIntervalMillis(5000).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // 1. Actualizar la UI del mapa (osmdroid GeoPoint)
                    onLocationUpdate(GeoPoint(location.latitude, location.longitude))

                    // 2. Sincronizar con Firebase Firestore
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        val db = FirebaseFirestore.getInstance()
                        val firebasePoint = FirestoreGeoPoint(location.latitude, location.longitude)

                        db.collection("users").document(uid)
                            .update("lastLocation", firebasePoint)
                            .addOnFailureListener { e ->
                                Log.e("FirestoreLoc", "Error updating location: ${e.message}")
                            }
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            }
            onDispose { client.removeLocationUpdates(callback) }
        }
    }
}

// ---------------- UTILS ----------------
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

fun readLocations(context: Context): List<GeoPoint> {
    return try {// 1. Abrir el recurso
        val inputStream = context.resources.openRawResource(R.raw.locations)
        val jsonString = inputStream.bufferedReader().use { it.readText() }

        // 2. Parsear como Objeto (porque el JSON empieza con { )
        val rootObject = JSONObject(jsonString)

        // 3. Obtener el arreglo por su nombre exacto
        val jsonArray = rootObject.getJSONArray("locationsArray")

        val list = mutableListOf<GeoPoint>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(GeoPoint(obj.getDouble("latitude"), obj.getDouble("longitude")))
        }
        list
    } catch (e: Exception) {
        Log.e("readLocations", "Error al leer el JSON: ${e.message}")
        emptyList() // Retorna lista vacía en lugar de crashear
    }
}

fun findLocation(context: Context, address: String): GeoPoint? {
    val geocoder = Geocoder(context)
    val result = geocoder.getFromLocationName(address, 1)
    return if (!result.isNullOrEmpty()) {
        GeoPoint(result[0].latitude, result[0].longitude)
    } else null
}
