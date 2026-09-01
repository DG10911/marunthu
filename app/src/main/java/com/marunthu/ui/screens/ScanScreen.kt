package com.marunthu.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.marunthu.ocr.MlKitOcr
import com.marunthu.ui.MarunthuViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ScanScreen(vm: MarunthuViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val ocr = remember { MlKitOcr() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Text("Point at a medicine strip", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text("Keep the drug name inside the box, in good light",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxWidth().weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture,
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )
            // Guide box — helps the user frame the strip so OCR reads cleanly.
            Box(
                Modifier.align(Alignment.Center)
                    .fillMaxWidth(0.82f)
                    .height(150.dp)
                    .border(3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(14.dp)),
            )
            Text(
                "Fit the strip inside the box",
                color = Color.White,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        Text("Scanned: ${state.scanned.joinToString { it.medicine.brandName }}")
        state.hint?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
        if (state.lastOcrText.isNotBlank()) {
            Text("OCR: ${state.lastOcrText.take(40)}",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bmp = image.toBitmap().rotate(image.imageInfo.rotationDegrees)
                            image.close()
                            scope.launch {
                                val text = ocr.recognize(bmp)
                                vm.onOcrText(text)
                            }
                        }
                        override fun onError(exc: ImageCaptureException) { /* keep camera alive */ }
                    },
                )
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) { Text("Capture") }

        Spacer(Modifier.height(8.dp))
        // FAILURE-RESISTANT BACKUP: inject known demo medicines if live OCR struggles.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.addSampleById("DOLO_650_TAB") }, modifier = Modifier.weight(1f)) {
                Text("Sample: Dolo")
            }
            OutlinedButton(onClick = { vm.addSampleById("COMBIFLAM_TAB") }, modifier = Modifier.weight(1f)) {
                Text("Sample: Combiflam")
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(
                onClick = onDone,
                enabled = state.result != null,
                modifier = Modifier.weight(1f),
            ) { Text("See result") }
        }
    }
}

private fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val m = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}
