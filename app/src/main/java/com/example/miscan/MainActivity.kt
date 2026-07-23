package com.example.miscan

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miscan.ViewModels.BluetoothViewModel
import com.example.miscan.models.ScannedDevice


// Para que reconozca viewModel()
import androidx.lifecycle.viewmodel.compose.viewModel

// Para que reconozca el items() dentro de LazyColumn y el it.address
import androidx.compose.foundation.lazy.items

// Para que no marque error la palabra "by" en los estados (remember y collectAsState)
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Reemplaza esto con el tema de tu app si tienes uno generado (ej. ClipPastesTheme)
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppWithPermissions()
                }
            }
        }
    }
}

@Composable
fun AppWithPermissions() {
    // Definimos qué permisos pedir dependiendo de la versión de Android
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ (API 31+)
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        // Android 11 o inferior
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    var permissionsGranted by remember { mutableStateOf(false) }

    // Lanzador para solicitar los permisos al usuario
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        // Verificamos si todos los permisos solicitados fueron aceptados
        permissionsGranted = permissionsMap.values.all { it }
    }

    // Efecto que se ejecuta al iniciar la pantalla para pedir los permisos inmediatamente
    LaunchedEffect(Unit) {
        permissionLauncher.launch(bluetoothPermissions)
    }

    // Lógica de navegación simple basada en permisos
    if (permissionsGranted) {
        // Si hay permisos, mostramos la pantalla que creamos en el ViewModel
        BluetoothScannerScreen()
    } else {
        // Si no hay permisos, mostramos un mensaje (aquí podrías poner un botón para re-intentar)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Se requieren permisos de Bluetooth y Ubicación para escanear.")
        }
    }
}

@Composable
fun BluetoothScannerScreen(viewModel: BluetoothViewModel = viewModel<BluetoothViewModel>()) {
    val devices by viewModel.devicesList.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Ventana 1: Botón de Pánico (Actualizar Bluetooth)
        PanicButtonSection(
            isScanning = isScanning,
            onScanClick = {
                if (isScanning) viewModel.stopScan() else viewModel.startScan()
            },
            modifier = Modifier.weight(0.4f)
        )

        Divider(color = Color.LightGray, thickness = 1.dp)

        // Ventana 2: Listado Compacto de Alcances
        DeviceListSection(
            devices = devices,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
fun PanicButtonSection(
    isScanning: Boolean,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onScanClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isScanning) Color(0xFFD32F2FL) else Color(0xFF1976D2)
            ),
            modifier = Modifier
                .size(150.dp) // Botón grande y redondo
                .padding(8.dp)
        ) {
            Text(
                text = if (isScanning) "PARAR" else "ESCANEAR",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun DeviceListSection(
    devices: List<ScannedDevice>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Dispositivos Cercanos",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp) // Listado compacto
        ) {
            items(items = devices, key = { it.address }) { device ->
                CompactDeviceItem(device)
            }
        }
    }
}

@Composable
fun CompactDeviceItem(device: ScannedDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Dispositivo Desconocido",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = device.address,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            // Indicador de alcance (RSSI)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${device.rssi} dBm",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}