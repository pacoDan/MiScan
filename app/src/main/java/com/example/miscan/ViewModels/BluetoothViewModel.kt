package com.example.miscan.ViewModels

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.miscan.models.ScannedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@SuppressLint("MissingPermission") // Asegúrate de pedir permisos en la UI antes de llamar a esto
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner

    private val _scannedDevices = MutableStateFlow<Map<String, ScannedDevice>>(emptyMap())

    // Exponemos la lista ordenada por RSSI descendente (los más cercanos primero)
    private val _devicesList = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devicesList: StateFlow<List<ScannedDevice>> = _devicesList.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            val rssi = result.rssi
            val name = device.name ?: "Desconocido"

            // Actualizamos el mapa para no duplicar, usando la MAC address como llave
            _scannedDevices.update { currentMap ->
                val newMap = currentMap.toMutableMap()
                newMap[device.address] = ScannedDevice(device.address, name, rssi)
                newMap
            }

            // Ordenamos la lista: RSSI mayor (menos negativo) significa más cerca
            _devicesList.value = _scannedDevices.value.values.sortedByDescending { it.rssi }
        }
    }

    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        if (_isScanning.value) return

        _scannedDevices.value = emptyMap() // Limpiar lista anterior
        bleScanner?.startScan(scanCallback)
        _isScanning.value = true
    }

    fun stopScan() {
        bleScanner?.stopScan(scanCallback)
        _isScanning.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}