package name.alexwayfer.customtv.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import name.alexwayfer.customtv.R
import name.alexwayfer.customtv.ui.theme.PremiumGold

/** The lock beside a Premium feature the free build shows; TalkBack reads that it is in Premium. */
@Composable
internal fun PremiumLockIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Outlined.Lock,
        contentDescription = stringResource(R.string.premium_locked),
        modifier = modifier,
        tint = PremiumGold,
    )
}
