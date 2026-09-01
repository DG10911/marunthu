package com.marunthu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marunthu.ui.theme.Teal

/** White elevated card with soft shadow + rounded corners — the base surface of the app. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp), clip = false, spotColor = Color(0x22000000)),
        color = accent ?: MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

/** Big gradient call-to-action with an icon. */
@Composable
fun GradientButton(
    label: String,
    icon: ImageVector,
    brush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 110.dp,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(height)
            .shadow(14.dp, RoundedCornerShape(30.dp), spotColor = Teal.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
    ) {
        Box(Modifier.background(brush), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(14.dp))
                Text(label, color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** Small pill used for trust signals ("Offline", "Private", "On-device AI"). */
@Composable
fun TrustPill(icon: String, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$icon ", style = MaterialTheme.typography.labelMedium)
            Text(text, color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun TrustRow(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TrustPill("✈️", "Offline")
        TrustPill("🔒", "Private")
        TrustPill("🧠", "On-device AI")
    }
}

/** Colored status chip for the result hero (OK / Warning / Uncertain). */
@Composable
fun StatusPill(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))) {
        Text(text, color = color, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
    }
}

/** Segmented language selector (pill group). */
@Composable
fun LanguageSelector(
    options: List<Pair<String, String>>,   // code to display
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (code, label) ->
                val active = code == selected
                Surface(
                    onClick = { onSelect(code) },
                    color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        label,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
