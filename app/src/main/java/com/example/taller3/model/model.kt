package com.example.taller3.model

import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.Date

// CONTACTO
data class Contact(
    val id: String,
    val name: String,
    val phone: String
)

// UBICACION
data class MyLocation(
    val date: Date,
    val latitude: Double,
    val longitude: Double
) {
    fun toJSON(): JSONObject {
        val obj = JSONObject()
        obj.put("date", date.toString())
        obj.put("latitude", latitude)
        obj.put("longitude", longitude)
        return obj
    }
}

// MAPA
data class MapState(
    val userLocation: GeoPoint? = null,
    val searchedLocation: GeoPoint? = null,
    val longClickLocation: GeoPoint? = null,
    val showHistory: Boolean = false,
    val isDarkMode: Boolean = false,
    val routePoints: List<GeoPoint> = emptyList(),
    val historyPoints: List<GeoPoint> = emptyList(),
    val distanceText: String = ""
)
