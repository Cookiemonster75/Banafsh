package app.banafsh.android.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.banafsh.android.ui.theme.LocalAppearance
import app.banafsh.android.ui.theme.primaryButton
import app.banafsh.android.ui.theme.utils.roundedShape
import app.banafsh.android.utils.disabled
import app.banafsh.android.utils.medium

@Composable
fun DialogTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false
) {
    val (colorPalette, typography) = LocalAppearance.current

    BasicText(
        text = text,
        style = typography.xs.medium.let {
            if (!enabled) it.disabled
            else it
        },
        modifier = modifier
            .clip(16.dp.roundedShape)
            .background(if (primary) colorPalette.primaryButton else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(all = 8.dp)
            .padding(horizontal = 8.dp)
    )
}
