package com.sismptm.client.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

@SuppressLint("MissingPermission")
@Composable
fun MapServiceScreen(
    mapViewModel: MapViewModel = viewModel(),
    serviceViewModel: ServiceViewModel = viewModel(),
    onBack: () -> Unit,
    onServiceCreated: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    MapLibre.getInstance(context)

    val selectedLocation by mapViewModel.selectedLocation.collectAsState()
    val showDescriptionSheet by mapViewModel.showDescriptionSheet.collectAsState()
    val createState by serviceViewModel.createServiceState.collectAsState()

    val mapView = rememberMapViewForService(context, mapViewModel)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(createState) {
        Log.d("MapServiceScreen", "[STATE] createState changed to $createState")
        when (createState) {
            is CreateServiceUiState.Success -> {
                val serviceId = (createState as CreateServiceUiState.Success).serviceId
                Log.d("MapServiceScreen", "[SUCCESS] serviceId=$serviceId. Navigating to waiting screen.")
                mapViewModel.clearLocation()
                serviceViewModel.resetState()
                onServiceCreated(serviceId)
            }
            is CreateServiceUiState.Error -> {
                val msg = (createState as CreateServiceUiState.Error).message
                Log.e("MapServiceScreen", "[ERROR] $msg")
                snackbarHostState.showSnackbar(msg)
            }
            is CreateServiceUiState.Loading -> Log.d("MapServiceScreen", "[STATE] Loading...")
            is CreateServiceUiState.Idle -> Log.d("MapServiceScreen", "[STATE] Idle")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets(0))
    ) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        if (selectedLocation != null) {
            FloatingActionButton(
                onClick = { mapViewModel.showDescriptionSheet() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
                containerColor = Color(0xFF2196F3)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Confirm location",
                    tint = Color.White
                )
            }
        }

        // Sheet anclada al fondo del Box
        if (showDescriptionSheet) {
            // Fondo semi-transparente
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
            )
            // Contenido del sheet en la parte inferior
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                LocationDescriptionSheet(
                    viewModel = mapViewModel,
                    onConfirm = { location, description ->
                        Log.d("MapServiceScreen", "[ACTION] onConfirm received, calling createService")
                        serviceViewModel.createService(location, description)
                    },
                    onDismiss = { mapViewModel.hideDescriptionSheet() }
                )
            }
        }

        if (createState is CreateServiceUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2196F3))
            }
        }

        // Snackbar para mostrar errores al usuario
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun rememberMapViewForService(
    context: android.content.Context,
    mapViewModel: MapViewModel
): MapView {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                    map.uiSettings.apply {
                        isZoomGesturesEnabled = true
                        isScrollGesturesEnabled = true
                        isRotateGesturesEnabled = true
                        isTiltGesturesEnabled = true
                        isDoubleTapGesturesEnabled = true
                    }

                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(2.4448, -76.6147))
                        .zoom(13.0)
                        .build()

                    // Cargar drawable como bitmap para el pin
                    val drawable = androidx.core.content.ContextCompat.getDrawable(
                        context,
                        com.sismptm.client.R.drawable.service_location_icon
                    )!!
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    style.addImage("service-pin", bitmap)

                    // SymbolManager para manejar el pin
                    val symbolManager = org.maplibre.android.plugins.annotation.SymbolManager(
                        this, map, style
                    )
                    symbolManager.iconAllowOverlap = true
                    var currentSymbol: org.maplibre.android.plugins.annotation.Symbol? = null

                    map.addOnMapClickListener { latLng ->
                        mapViewModel.onLocationSelected(latLng.latitude, latLng.longitude)

                        // Reemplazar pin anterior
                        if (currentSymbol != null) {
                            symbolManager.delete(currentSymbol)
                        }

                        currentSymbol = symbolManager.create(
                            org.maplibre.android.plugins.annotation.SymbolOptions()
                                .withLatLng(latLng)
                                .withIconImage("service-pin")
                                .withIconSize(1.0f)
                                .withIconAnchor("bottom")
                        )
                        true
                    }
                }
            }
        }
    }

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