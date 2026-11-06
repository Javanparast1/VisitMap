package com.javanparast.visitmap.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.javanparast.visitmap.data.model.CustomerEntity
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val GILAN_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

@Composable
fun MapPickerScreen(
    customer: CustomerEntity,
    onBack: () -> Unit,
    onSubmitProposal: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val locationManager = remember(context) {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    var selectedPoint by remember { mutableStateOf<LatLng?>(null) }
    var selectedMarker by remember { mutableStateOf<Marker?>(null) }
    var currentLocationMarker by remember { mutableStateOf<Marker?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var hasCenteredOnCurrentLocation by remember { mutableStateOf(false) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val mapStatusState = remember { mutableStateOf("در حال اتصال به سرویس نقشه…") }
    val locationStatusState = remember { mutableStateOf("برای نمایش موقعیت فعلی، مجوز مکان‌یابی لازم است.") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        locationPermissionGranted =
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!locationPermissionGranted) {
            locationStatusState.value = "مجوز مکان‌یابی داده نشد؛ می‌توانید نقطه فروشگاه را دستی انتخاب کنید."
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            addOnDidFailLoadingMapListener { errorMessage ->
                mapStatusState.value = "خطا در بارگذاری نقشه: ${errorMessage.take(240)}"
            }
            addOnDidFinishLoadingStyleListener {
                mapStatusState.value = "نقشه بارگذاری شد."
            }
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(37.2808, 49.5832))
                    .zoom(11.0)
                    .build()
                map.setStyle(Style.Builder().fromUri(GILAN_STYLE_URL)) {
                    mapRef = map
                    mapReady = true
                    mapStatusState.value = "نقشه بارگذاری شد."
                    map.addOnMapClickListener { point ->
                        selectedMarker?.remove()
                        selectedMarker = map.addMarker(
                            MarkerOptions()
                                .position(point)
                                .title("مکان پیشنهادی فروشگاه")
                        )
                        selectedPoint = point
                        true
                    }
                }
            }
        }
    }

    // Keep the current-device marker separate from the manually selected shop marker.
    DisposableEffect(locationPermissionGranted, mapReady, mapRef) {
        if (!locationPermissionGranted || !mapReady || mapRef == null) {
            onDispose { }
        } else {
            val map = mapRef!!
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val point = LatLng(location.latitude, location.longitude)
                    currentLocation = point
                    currentLocationMarker?.remove()
                    val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    paint.color = android.graphics.Color.WHITE
                    canvas.drawCircle(24f, 24f, 20f, paint)
                    paint.color = android.graphics.Color.rgb(25, 118, 210)
                    canvas.drawCircle(24f, 24f, 15f, paint)
                    currentLocationMarker = map.addMarker(
                        MarkerOptions()
                            .position(point)
                            .title("موقعیت فعلی شما")
                            .icon(IconFactory.getInstance(context).fromBitmap(bitmap))
                    )
                    if (!hasCenteredOnCurrentLocation) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 16.0))
                        hasCenteredOnCurrentLocation = true
                    }
                    locationStatusState.value =
                        "موقعیت فعلی دریافت شد؛ دقت تقریبی: ${location.accuracy.toInt()} متر"
                }
            }

            try {
                val lastKnown = locationManager.getProviders(true)
                    .asSequence()
                    .mapNotNull { provider ->
                        try { locationManager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
                    }
                    .maxByOrNull { it.time }
                lastKnown?.let(listener::onLocationChanged)

                val providers = locationManager.getProviders(true)
                val usableProviders = providers.filter { provider ->
                    provider == LocationManager.NETWORK_PROVIDER || (
                        provider == LocationManager.GPS_PROVIDER &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    )
                }
                usableProviders.forEach { provider ->
                    locationManager.requestLocationUpdates(provider, 3000L, 5f, listener, Looper.getMainLooper())
                }
                if (usableProviders.isEmpty() && lastKnown == null) {
                    locationStatusState.value = "موقعیت فعلی در دسترس نیست؛ GPS یا مکان‌یابی گوشی را روشن کنید."
                }
            } catch (_: SecurityException) {
                locationStatusState.value = "دسترسی مکان‌یابی ممکن نیست؛ انتخاب دستی همچنان فعال است."
            }

            onDispose {
                try { locationManager.removeUpdates(listener) } catch (_: SecurityException) { }
            }
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("انتخاب مکان برای: ${customer.name}", style = MaterialTheme.typography.titleMedium)
        Text("نشانگر آبی موقعیت فعلی گوشی است؛ نشانگر انتخابی محل پیشنهادی فروشگاه را مشخص می‌کند.")
        Text(mapStatusState.value, style = MaterialTheme.typography.bodySmall)
        Text(locationStatusState.value, style = MaterialTheme.typography.bodySmall)
        if (!locationPermissionGranted) {
            OutlinedButton(
                onClick = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("اجازه دسترسی به موقعیت فعلی") }
        } else {
            OutlinedButton(
                onClick = {
                    hasCenteredOnCurrentLocation = false
                    currentLocation?.let { point ->
                        mapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 16.0))
                        hasCenteredOnCurrentLocation = true
                    } ?: run {
                        locationStatusState.value = "در انتظار دریافت موقعیت فعلی…"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("رفتن به موقعیت فعلی من") }
        }
        AndroidView(factory = { mapView }, modifier = Modifier.weight(1f).fillMaxWidth())
        selectedPoint?.let {
            Text("نقطه انتخاب‌شده: %.6f ، %.6f".format(it.latitude, it.longitude))
        } ?: Text("هنوز نقطه‌ای انتخاب نشده است.")
        Button(
            enabled = selectedPoint != null,
            onClick = { selectedPoint?.let { onSubmitProposal(it.latitude, it.longitude) } },
            modifier = Modifier.fillMaxWidth()
        ) { Text("ثبت به‌عنوان پیشنهاد مکان") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("بازگشت") }
    }
}
