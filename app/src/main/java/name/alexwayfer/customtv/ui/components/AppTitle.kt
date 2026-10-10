package name.alexwayfer.customtv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.TwitchPurple

/** The logo and the app name, as the home screen and the Premium access screen show them. */
@Composable
internal fun AppTitle(
    modifier: Modifier = Modifier,
    logoSize: Dp = 28.dp,
    fontSize: TextUnit = 22.sp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(AppLogo),
            contentDescription = null,
            modifier = Modifier.size(logoSize),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.home_title),
            color = TwitchPurple,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            modifier = Modifier.weight(1f),
        )
    }
}
