package com.marunthu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marunthu.core.model.SafetyStatus
import com.marunthu.data.MedicineInfo
import com.marunthu.lang.LanguageEngine
import com.marunthu.ui.MarunthuViewModel
import com.marunthu.ui.components.LanguageSelector
import com.marunthu.ui.components.SoftCard
import com.marunthu.ui.components.StatusPill
import com.marunthu.ui.theme.DangerRed
import com.marunthu.ui.theme.SafeGreen
import com.marunthu.ui.theme.Teal
import com.marunthu.ui.theme.WarnAmber

private fun statusColor(s: SafetyStatus?) = when (s) {
    SafetyStatus.WARNING -> DangerRed
    SafetyStatus.UNCERTAIN -> WarnAmber
    SafetyStatus.OK -> SafeGreen
    null -> Color.Gray
}
private fun statusIcon(s: SafetyStatus?): ImageVector = when (s) {
    SafetyStatus.WARNING -> Icons.Filled.WarningAmber
    SafetyStatus.UNCERTAIN -> Icons.Filled.HelpOutline
    else -> Icons.Filled.CheckCircle
}

@Composable
fun ResultScreen(vm: MarunthuViewModel, onScanAnother: () -> Unit, onHome: () -> Unit) {
    val state by vm.state.collectAsState()
    val msg = state.message
    val color = statusColor(state.result?.status)

    LaunchedEffect(state.message, state.language) { vm.speakCurrent() }

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        // ---- Status hero ----
        SoftCard(padding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(18.dp)) {
                    Icon(statusIcon(state.result?.status), null, tint = color,
                        modifier = Modifier.padding(12.dp).size(30.dp))
                }
                Spacer(Modifier.width(14.dp))
                StatusPill(msg?.severityLabel ?: "—", color)
            }
            Spacer(Modifier.height(14.dp))
            Text(msg?.title ?: "No result", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text(msg?.body ?: "", style = MaterialTheme.typography.bodyLarge)

            val a = state.result?.medicineA?.medicine?.brandName
            val b = state.result?.medicineB?.medicine?.brandName
            if (a != null && b != null) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MedChip(a); Text("+", fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)); MedChip(b)
                }
            }
            state.result?.confidence?.let {
                Spacer(Modifier.height(12.dp))
                Text("Confidence ${(it * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---- Expiry intelligence (read off the strip) ----
        state.expiryInfo?.takeIf { it.expired || it.expiringSoon }?.let { exp ->
            Spacer(Modifier.height(14.dp))
            val c = if (exp.expired) DangerRed else WarnAmber
            SoftCard(accent = c.copy(alpha = 0.12f)) {
                Text(if (exp.expired) "⛔ Expired medicine" else "⏳ Expiring soon",
                    fontWeight = FontWeight.Bold, color = c)
                Spacer(Modifier.height(6.dp))
                Text(LanguageEngine.expiryLine(exp, state.expiryToxic, state.language),
                    fontSize = 16.sp)
            }
        }

        // ---- What it's for (medicine uses) ----
        val primaryMed = state.scanned.lastOrNull()?.medicine
        val uses = primaryMed?.let { MedicineInfo.usesFor(it.ingredientIds, state.language) }
        uses?.let {
            Spacer(Modifier.height(14.dp))
            SoftCard(accent = MaterialTheme.colorScheme.primaryContainer) {
                Text("ℹ️  ${primaryMed.brandName} — what it's for",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        // ---- Proactive My-Meds clash ----
        state.profileWarning?.let { warn ->
            Spacer(Modifier.height(14.dp))
            SoftCard(accent = DangerRed.copy(alpha = 0.10f)) {
                Text("⚠️  Clashes with your saved medicines",
                    fontWeight = FontWeight.Bold, color = DangerRed)
                Spacer(Modifier.height(6.dp))
                Text(warn, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // ---- Savings (deal card) ----
        state.substitute?.let { sub ->
            Spacer(Modifier.height(14.dp))
            SoftCard(accent = SafeGreen.copy(alpha = 0.10f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Savings, null, tint = SafeGreen, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save money — same medicine", fontWeight = FontWeight.Bold, color = SafeGreen)
                    Spacer(Modifier.width(8.dp))
                    StatusPill("−${sub.savingsPercent}%", SafeGreen)
                }
                Spacer(Modifier.height(8.dp))
                Text(LanguageEngine.savingsLine(sub, state.language),
                    style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { vm.speakCurrent() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.ttsAvailable) "Speak again" else "Voice unavailable — showing text",
                style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(20.dp))
        Text("Same medicine. Same intelligence. Change the language:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        LanguageSelector(
            options = LanguageEngine.SUPPORTED.map { it.code to it.displayName },
            selected = state.language,
            onSelect = vm::setLanguage,
        )

        Spacer(Modifier.height(20.dp))
        val primaryId = state.scanned.lastOrNull()?.canonicalId
        val alreadySaved = primaryId != null && state.myMeds.any { it.canonicalId == primaryId }
        OutlinedButton(
            onClick = { vm.addPrimaryToMyMeds() },
            enabled = primaryId != null && !alreadySaved,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp))
            Text(if (alreadySaved) "In My Meds" else "Add to My Meds")
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onHome, modifier = Modifier.weight(1f).height(52.dp)) { Text("Home") }
            Button(onClick = onScanAnother, modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text("Scan another") }
        }
        Spacer(Modifier.height(12.dp))
        Text("Always confirm medication changes with a qualified professional.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MedChip(name: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
        Text("💊 $name", color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
