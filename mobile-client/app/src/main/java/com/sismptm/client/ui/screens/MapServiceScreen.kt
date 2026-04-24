package com.sismptm.client.ui.screens

import android.annotation.SuppressLint
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    MapLibre.getInstance(context)

    val selectedLocation by mapViewModel.selectedLocation.collectAsState()
    val showDescriptionSheet by mapViewModel.showDescriptionSheet.collectAsState()
    val createState by serviceViewModel.createServiceState.collectAsState()

    // On successful service creation, return to home
    LaunchedEffect(createState) {
        if (createState is CreateServiceUiState.Success) {
            mapViewModel.clearLocation()
            serviceViewModel.resetState()
            onBack()
        }
    }

    val mapView = rememberMapViewForService(context, mapViewModel)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(createState) {
        when (createState) {
            is CreateServiceUiState.Success -> {
                snackbarHostState.showSnackbar("Service created successfully!")
                mapViewModel.clearLocation()
                serviceViewModel.resetState()
                onBack()
            }
            is CreateServiceUiState.Error -> {
                snackbarHostState.showSnackbar(
                    (createState as CreateServiceUiState.Error).message
                )
            }
            else -> {}
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
                .statusBarsPadding()  // ← respeta status bar
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
                    .navigationBarsPadding()  // ← respeta nav bar
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

        if (showDescriptionSheet) {
            LocationDescriptionSheet(
                viewModel = mapViewModel,
                onConfirm = { location, description ->
                    serviceViewModel.createService(location, description)
                },
                onDismiss = { mapViewModel.hideDescriptionSheet() }
            )
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