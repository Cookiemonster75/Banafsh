package app.banafsh.android.preferences

import app.banafsh.android.GlobalPreferencesHolder
import app.banafsh.android.lib.core.ui.BuiltInFontFamily
import app.banafsh.android.lib.core.ui.ColorMode
import app.banafsh.android.lib.core.ui.ColorSource
import app.banafsh.android.lib.core.ui.Darkness
import app.banafsh.android.lib.core.ui.ThumbnailRoundness

object AppearancePreferences : GlobalPreferencesHolder() {
    var colorSource by enum(ColorSource.Dynamic)
    var colorMode by enum(ColorMode.System)
    var darkness by enum(Darkness.Normal)
    var thumbnailRoundness by enum(ThumbnailRoundness.Medium)
    var fontFamily by enum(BuiltInFontFamily.Poppins)
    var applyFontPadding by boolean(false)
    val isShowingThumbnailInLockscreenProperty = boolean(true)
    var isShowingThumbnailInLockscreen by isShowingThumbnailInLockscreenProperty
    var swipeToHideSong by boolean(false)
    var swipeToHideSongConfirm by boolean(true)
    var maxThumbnailSize by int(1920)
}
