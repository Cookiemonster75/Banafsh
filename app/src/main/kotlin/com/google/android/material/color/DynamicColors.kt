package com.google.android.material.color

import app.banafsh.android.ui.theme.utils.isAtLeastAndroid12

@Suppress("unused")
object DynamicColors {
    @JvmStatic
    fun isDynamicColorAvailable() = isAtLeastAndroid12
}
