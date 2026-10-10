package name.alexwayfer.customtv.ui

import androidx.compose.runtime.staticCompositionLocalOf
import name.alexwayfer.customtv.ui.account.TwitchUserAccess

/** The signed-in account's access for a request a sheet sends itself; null while signed out. */
internal val LocalUserAccess = staticCompositionLocalOf<suspend () -> TwitchUserAccess?> { { null } }
