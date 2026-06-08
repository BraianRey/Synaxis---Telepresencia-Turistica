package com.sismptm.client.ui.features.map

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.sismptm.client.ui.features.tour.ServiceViewModel
import com.sismptm.client.ui.features.tour.CreateServiceUiState
import com.sismptm.client.ui.theme.BlueSecondary

@SuppressLint("MissingPermission")
@Composable
fun MapServiceScreen(
    reserveMode: Boolean = false,
    mapViewModel: MapViewModel = viewModel(),
    serviceViewModel: ServiceViewModel = viewModel(),
    onBack: () -> Unit,
    onServiceCreated: (Long) -> Unit = {},
    onNavigateToReserve: (Double, Double, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    MapLibre.getInstance(context)

    val selectedLocation by mapViewModel.selectedLocation.collectAsState()
    val showDescriptionSheet by mapViewModel.showDescriptionSheet.collectAsState()
    val createState by serviceViewModel.createServiceState.collectAsState()

    val mapView = rememberMapViewForService(context, mapViewModel)
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        onBack()
    }

    MapServiceCreateEffect(
        createState = createState,
        mapViewModel = mapViewModel,
        serviceViewModel = serviceViewModel,
        snackbarHostState = snackbarHostState,
        onServiceCreated = onServiceCreated
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets(0))
    ) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        // Se pasa la alineación desde el Box padre
        MapServiceBackButton(
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // Se pasa la alineación desde el Box padre
        MapServiceConfirmButton(
            visible = selectedLocation != null,
            onClick = { mapViewModel.showDescriptionSheet() },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        if (showDescriptionSheet) {
            MapServiceDescriptionSheet(
                viewModel = mapViewModel,
                reserveMode = reserveMode,
                onNavigateToReserve = onNavigateToReserve,
                serviceViewModel = serviceViewModel,
                onDismiss = { mapViewModel.hideDescriptionSheet() }
            )
        }

        MapServiceLoadingOverlay(isLoading = createState is CreateServiceUiState.Loading)

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun MapServiceCreateEffect(
    createState: CreateServiceUiState,
    mapViewModel: MapViewModel,
    serviceViewModel: ServiceViewModel,
    snackbarHostState: SnackbarHostState,
    onServiceCreated: (Long) -> Unit
) {
    LaunchedEffect(createState) {
        Log.d("MapServiceScreen", "[STATE] createState changed to $createState")
        when (createState) {
            is CreateServiceUiState.Success -> {
                val serviceId = createState.serviceId
                Log.d(
                    "MapServiceScreen",
                    "[SUCCESS] serviceId=$serviceId. Navigating to waiting screen."
                )
                mapViewModel.clearLocation()
                serviceViewModel.resetState()
                onServiceCreated(serviceId)
            }
            is CreateServiceUiState.Error -> {
                val msg = createState.message
                Log.e("MapServiceScreen", "[ERROR] $msg")
                snackbarHostState.showSnackbar(msg)
            }
            is CreateServiceUiState.Loading -> Log.d("MapServiceScreen", "[STATE] Loading...")
            is CreateServiceUiState.Idle -> Log.d("MapServiceScreen", "[STATE] Idle")
        }
    }
}

@Composable
private fun MapServiceBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onBack,
        modifier = modifier
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
}

@Composable
private fun MapServiceConfirmButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .navigationBarsPadding()
            .padding(16.dp),
        containerColor = BlueSecondary
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Confirm location",
            tint = Color.White
        )
    }
}

@Composable
private fun MapServiceDescriptionSheet(
    viewModel: MapViewModel,
    reserveMode: Boolean,
    onNavigateToReserve: (Double, Double, String) -> Unit,
    serviceViewModel: ServiceViewModel,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
    )
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        LocationDescriptionSheet(
            viewModel = viewModel,
            reserveMode = reserveMode,
            onConfirm = { location, description ->
                if (reserveMode) {
                    Log.d(
                        "MapServiceScreen",
                        "[ACTION] Reserve mode: navigating to ReserveServiceScreen"
                    )
                    onNavigateToReserve(location.lat, location.lon, description)
                } else {
                    Log.d(
                        "MapServiceScreen",
                        "[ACTION] onConfirm received, calling createService"
                    )
                    serviceViewModel.createService(location, description)
                }
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun MapServiceLoadingOverlay(isLoading: Boolean) {
    if (!isLoading) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = BlueSecondary)
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

                    // Load drawable as bitmap for the pin
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

                    // SymbolManager to handle the pin
                    val symbolManager = org.maplibre.android.plugins.annotation.SymbolManager(
                        this, map, style
                    )
                    symbolManager.iconAllowOverlap = true
                    var currentSymbol: org.maplibre.android.plugins.annotation.Symbol? = null

                    map.addOnMapClickListener { latLng ->
                        mapViewModel.onLocationSelected(latLng.latitude, latLng.longitude)

                        // Replace previous pin
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