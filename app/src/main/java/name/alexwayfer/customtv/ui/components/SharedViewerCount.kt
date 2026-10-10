package name.alexwayfer.customtv.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.R

@Composable
fun SharedViewerCount(viewerCount: Int, color: Color, parenthesized: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (parenthesized) {
            Text(" (", color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Icon(
            painter = painterResource(R.drawable.ic_shared_viewers),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        val formatted = abbreviatedCount(viewerCount.toLong())
        Text(
            if (parenthesized) "$formatted)" else formatted,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
