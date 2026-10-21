package com.sonarfiles.app.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonarfiles.app.core.ui.theme.SonarCyanPrimary

data class PathSegment(
    val name: String,
    val fullPath: String
)

@Composable
fun BreadcrumbBar(
    currentPath: String,
    onNavigateToSegment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val segments = parsePathSegments(currentPath)

    // Auto-scroll para o final da lista quando o caminho mudar
    LaunchedEffect(currentPath) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Raiz Inicial
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigateToSegment("/") }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Raiz",
                    tint = if (segments.isEmpty()) SonarCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Raiz",
                    fontSize = 13.sp,
                    fontWeight = if (segments.isEmpty()) FontWeight.Bold else FontWeight.Medium,
                    color = if (segments.isEmpty()) SonarCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            segments.forEachIndexed { index, segment ->
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )

                val isLast = index == segments.lastIndex
                Text(
                    text = segment.name,
                    fontSize = 13.sp,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                    color = if (isLast) SonarCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = !isLast) { onNavigateToSegment(segment.fullPath) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

fun parsePathSegments(path: String): List<PathSegment> {
    val clean = path.trim().replace("\\", "/").trim('/')
    if (clean.isBlank()) return emptyList()

    val parts = clean.split('/')
    val segments = mutableListOf<PathSegment>()
    var accumulated = ""

    for (part in parts) {
        if (part.isBlank()) continue
        accumulated += "/$part"
        segments.add(PathSegment(name = part, fullPath = accumulated))
    }
    return segments
}
