package com.example.generator.utils

import android.animation.ValueAnimator
import android.view.View
import androidx.core.animation.doOnEnd

/**
 * Expands the given view by animating its height from 0 to its measured height.
 *
 * @param view The View to expand.
 */
fun expand(view: View) {
    view.measure(
        View.MeasureSpec.makeMeasureSpec((view.parent as View).width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.UNSPECIFIED
    )
    val targetHeight = view.measuredHeight

    view.layoutParams.height = 0
    view.visibility = View.VISIBLE

    val animator = ValueAnimator.ofInt(0, targetHeight)
    animator.addUpdateListener { valueAnimator ->
        view.layoutParams.height = valueAnimator.animatedValue as Int
        view.requestLayout()
    }
    animator.duration = 300
    animator.start()
}

/**
 * Collapses the given view by animating its height from its current height to 0.
 *
 * @param view The View to collapse.
 */
fun collapse(view: View) {
    val initialHeight = view.measuredHeight

    val animator = ValueAnimator.ofInt(initialHeight, 0)
    animator.addUpdateListener { valueAnimator ->
        view.layoutParams.height = valueAnimator.animatedValue as Int
        view.requestLayout()
    }

    animator.duration = 300
    animator.start()

    animator.doOnEnd {
        view.visibility = View.GONE
    }
}