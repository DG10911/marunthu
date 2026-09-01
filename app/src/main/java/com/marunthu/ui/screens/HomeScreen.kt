package com.marunthu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marunthu.lang.LanguageEngine
import com.marunthu.ui.MarunthuViewModel
import com.marunthu.ui.components.GradientButton
import com.marunthu.ui.components.LanguageSelector
import com.marunthu.ui.components.SoftCard
import com.marunthu.ui.components.TrustRow
import com.marunthu.ui.theme.HeroGradient
import com.marunthu.ui.theme.ScanGradient

@Composable
fun HomeScreen(vm: MarunthuViewModel, onScan: () -> Unit) {
    val state by vm.state.collectAsState()
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ---- Gradient hero ----
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(HeroGradient)
                .padding(horizontal = 24.dp, vertical = 40.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()) {
                Surface(color = Color.White.copy(alpha = 0.18f), shape = RoundedCornerShape(24.dp)) {
                    Icon(Icons.Filled.HealthAndSafety, null, tint = Color.White,
                        modifier = Modifier.padding(14.dp).size(40.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Marunthu", color = Color.White,
                    style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(6.dp))
                Text("Medicine safety. In your language.\nWithout the internet.",
                    color = Color.White.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(18.dp))
                TrustRow()
            }
        }

        Column(Modifier.padding(24.dp)) {
            Spacer(Modifier.height(8.dp))
            GradientButton(
                label = "Scan medicine",
                icon = Icons.Filled.CameraAlt,
                brush = ScanGradient,
                onClick = onScan,
            )

            if (state.catalogSize > 0) {
                Spacer(Modifier.height(12.dp))
                Text("🗂️  ${"%,d".format(state.catalogSize)} medicines · fully offline",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp))
            }

            Spacer(Modifier.height(28.dp))
            Text("Speak the result in", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            LanguageSelector(
                options = LanguageEngine.SUPPORTED.map { it.code to it.displayName },
                selected = state.language,
                onSelect = vm::setLanguage,
            )

            if (state.myMeds.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SoftCard(accent = MaterialTheme.colorScheme.primaryContainer) {
                    Text("💊  My Meds",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text("${state.myMeds.size} saved · every new scan is checked against these",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(Modifier.height(24.dp))
            SoftCard(accent = MaterialTheme.colorScheme.surfaceVariant, padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                Text("Safety information from a local prototype database. Always confirm with a " +
                    "doctor or pharmacist. Marunthu does not diagnose or prescribe.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
