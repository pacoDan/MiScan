package com.example.miscan.models

data class ScannedDevice(
    val address: String,
    val name: String?,
    val rssi: Int // Nivel de señal en dBm (ej: -40 es cerca, -90 es lejos)
)