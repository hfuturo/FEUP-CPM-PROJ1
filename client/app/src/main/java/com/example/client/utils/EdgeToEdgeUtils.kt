package com.example.client.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.min

/**
 * Function to set/add paddings to a View according to the insets existent on the screen (system + hardware cutouts).
 * A negative edge value ignores the correspondent inset side, otherwise it sums the value.
 */
fun setInsetsPadding(vw: View, left: Int = -1, top: Int = -1, right: Int = -1, bottom: Int = -1) {
    ViewCompat.setOnApplyWindowInsetsListener(vw) { v, insets ->
        val totalInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val l = if (left != -1) totalInsets.left + left else 0
        val t = if (top != -1) totalInsets.top + top else 0
        val r = if (right != -1) totalInsets.right + right else 0
        val b = if (bottom != -1) totalInsets.bottom + bottom else 0
        v.setPadding(l, t, r, b)
        insets
    }
}

/**
 * Function to set/add margins to a View according to the insets existent on the screen (system + hardware cutouts).
 * A negative edge value ignores the correspondent inset side, otherwise it sums the value.
 */
fun setInsetsMargin(vw: View, left: Int = -1, top: Int = -1, right: Int = -1, bottom: Int = -1) {
    ViewCompat.setOnApplyWindowInsetsListener(vw) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val l = if (left != -1) systemBars.left + left else 0
        val t = if (top != -1) systemBars.top + top else 0
        val r = if (right != -1) systemBars.right + right else 0
        val b = if (bottom != -1) systemBars.bottom + bottom else 0
        (v.layoutParams as ViewGroup.MarginLayoutParams).setMargins(l, t, r, b)
        insets
    }
}

/**
 * Functions to set the icons on the Status/Navigation Bars as LIGHT or DARK.
 * Lightness.LIGHT makes the icons with a light color.
 * Lightness.DARK makes the icons with a dark color.
 */
enum class Lightness { LIGHT, DARK }

@Suppress("DEPRECATION")
fun setStatusBarIconColor(window: Window, lightness: Lightness) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val insetsController = window.insetsController
        if (insetsController != null)
            if (lightness == Lightness.DARK)
                insetsController.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            else
                insetsController.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
    }
    else {
        var flags = window.decorView.systemUiVisibility
        flags = if (lightness == Lightness.DARK)
            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        else
            flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        window.decorView.systemUiVisibility = flags
    }
}

@Suppress("DEPRECATION")
fun setNavigationBarIconColor(window: Window, lightness: Lightness) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val insetsController = window.insetsController
        if (insetsController != null)
            if (lightness == Lightness.DARK)
                insetsController.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            else
                insetsController.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
    }
    else {
        var flags = window.decorView.systemUiVisibility
        flags = if (lightness == Lightness.DARK)
            flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        else
            flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        window.decorView.systemUiVisibility = flags
    }
}

/**
 * Extension function of class AppCompatActivity to convert dpi's into pixels.
 */
fun AppCompatActivity.dpToPx(dp: Float) = (dp*resources.displayMetrics.density).toInt()

/**
 * Smoothing and modify the motion of a FAB, when inside a CoordinatorLayout.
 * Assign this class to the FAB app:layout_behavior attribute in the resource layout file: app:layout_behavior="PATH_TO_CLASS.MoveSnackFABBehavior.
 */
class MoveSnackFABBehavior(context: Context?, attrs: AttributeSet?) : CoordinatorLayout.Behavior<View>(context, attrs) {
    @SuppressLint("RestrictedApi")
    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }

    /**
     * Discount the snack bar height to remove the FAB inset space.
     */
    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        val translationY = min(0f, dependency.translationY - dependency.height)
        if (dependency.translationY != 0f)
            child.translationY = translationY
        return true
    }

    /**
     * To smooth the FAB fall.
     */
    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: View, dependency: View) {
        child.animate().translationY(0f).setDuration(200).start()
    }
}