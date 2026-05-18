package dev.trinitychurch.lyrics.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.trinitychurch.lyrics.domain.Slide

@Composable
fun SlideThumbnailCard(
    slide: Slide,
    isCurrentDisplay: Boolean,
    isCurrentOperator: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isCurrentDisplay) MaterialTheme.colors.primary else Color.Transparent
    val borderWidth = if (isCurrentDisplay) 2.dp else 0.dp
    val shape = RoundedCornerShape(4.dp)

    Card(
        modifier = modifier
            .alpha(if (isCurrentOperator && !isCurrentDisplay) 0.5f else 1f)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick),
        shape = shape,
        elevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            slide.lines.forEach { line ->
                Text(
                    text = line,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
