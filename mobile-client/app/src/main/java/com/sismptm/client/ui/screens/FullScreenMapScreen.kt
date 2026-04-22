package com.sismptm.client.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

@SuppressLint("MissingPermission")
@Composable
fun FullScreenMapScreen(
    viewModel: MapViewModel,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    MapLibre.getInstance(context)

    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var symbolManager by remember { mutableStateOf<org.maplibre.android.plugins.annotation.SymbolManager?>(null) }
    var selectedSymbol by remember { mutableStateOf<org.maplibre.android.plugins.annotation.Symbol?>(null) }

    val mapView = rememberFullScreenMapView()

    Box(modifier = Modifier.fillMaxSize()) {

        // Mapa
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.also { mv ->
                    mv.getMapAsync { map ->
                        map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                            map.uiSettings.apply {
                                isZoomGesturesEnabled = true
                                isScrollGesturesEnabled = true
                                isRotateGesturesEnabled = true
                                isTiltGesturesEnabled = true
                                isDoubleTapGesturesEnabled = true
                            }

                            // Cámara inicial
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(2.4448, -76.6147))
                                .zoom(13.0)
                                .build()

                            val sm = org.maplibre.android.plugins.annotation.SymbolManager(
                                mv, map, style
                            )
                            symbolManager = sm

                            // Tap en el mapa para seleccionar ubicación
                            map.addOnMapClickListener { latLng ->
                                selectedLatLng = latLng
                                viewModel.onLocationSelected(latLng.latitude, latLng.longitude)

                                // Crear o mover el marcador
                                if (selectedSymbol == null) {
                                    val bitmap = createSelectionPinBitmap()
                                    style.addImage("selected-location-icon", bitmap)
                                    selectedSymbol = sm.create(
                                        org.maplibre.android.plugins.annotation.SymbolOptions()
                                            .withLatLng(latLng)
                                            .withIconImage("selected-location-icon")
                                            .withIconSize(1.0f)
                                            .withIconAnchor("bottom")
                                    )
                                } else {
                                    selectedSymbol?.latLng = latLng
                                    sm.update(selectedSymbol)
                                }
                                true
                            }
                        }
                    }
                }
            }
        )

        // Botón volver
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp)
            )
        }

        // Texto indicador
        Text(
            text = if (selectedLatLng == null) "Toca el mapa para seleccionar" else "Ubicación seleccionada",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 20.dp)
                .background(
                    color = Color(0x99000000),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Botón confirmar — solo visible si hay ubicación seleccionada
        if (selectedLatLng != null) {
            FloatingActionButton(
                onClick = onConfirm,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                containerColor = Color(0xFF2196F3)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Confirmar ubicación",
                    tint = Color.White
                )
            }
        }
    }
}

fun createSelectionPinBitmap(): android.graphics.Bitmap {
    val size = 80
    val pinHeight = 120
    val bitmap = android.graphics.Bitmap.createBitmap(size, pinHeight, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val cx = size / 2f
    val radius = size / 2f

    // Cuerpo azul del pin
    paint.color = android.graphics.Color.parseColor("#2196F3")
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawCircle(cx, radius, radius, paint)

    // Círculo blanco interior
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, radius, radius * 0.4f, paint)

    // Punta del pin
    paint.color = android.graphics.Color.parseColor("#2196F3")
    val path = android.graphics.Path().apply {
        moveTo(cx - radius * 0.5f, radius * 1.3f)
        lineTo(cx + radius * 0.5f, radius * 1.3f)
        lineTo(cx, pinHeight.toFloat())
        close()
    }
    canvas.drawPath(path, paint)

    return bitmap
}

@Composable
fun rememberFullScreenMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: LifecycleOwner) = mapView.onStop()
            override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return mapView
}