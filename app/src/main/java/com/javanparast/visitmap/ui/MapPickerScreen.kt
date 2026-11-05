package com.javanparast.visitmap.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.javanparast.visitmap.data.model.CustomerEntity
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val GILAN_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun MapPickerScreen(
    customer: CustomerEntity,
    onBack: () -> Unit,
    onSubmitProposal: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var selectedPoint by remember { mutableStateOf<LatLng?>(null) }
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(37.2808, 49.5832))
                    .zoom(11.0)
                    .build()
                map.setStyle(Style.Builder().fromUri(GILAN_STYLE_URL)) {
                    map.addOnMapClickListener { point ->
                        selectedPoint = point
                        true
                    }
                }
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
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
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
        Text("انتخاب مکان برای: \${customer.name}", style = MaterialTheme.typography.titleMedium)
        Text("روی نقشه نقطه‌ی فروشگاه را لمس کنید. این انتخاب فقط پیشنهاد است و مکان تأییدشده را تغییر نمی‌دهد.")
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
