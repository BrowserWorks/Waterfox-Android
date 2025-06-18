/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.perf

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import mozilla.components.concept.base.profiler.Profiler
import net.waterfox.android.HomeActivity
import net.waterfox.android.ext.components
import net.waterfox.android.perf.ProfilerMarkers.MEASURE_LAYOUT_DRAW_MARKER_NAME

/**
 * A [LinearLayout] that adds profiler markers for various methods and handles system window insets.
 * This is intended to be used on the root view of [HomeActivity]'s view hierarchy to understand
 * global measure/layout events and properly handle system bars.
 */
class HomeActivityRootLinearLayout(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private val profiler: Profiler? = context.components.core.engine.profiler

    init {
        // Handle system window insets to prevent content from going behind status bar
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom,
                left = systemBars.left,
                right = systemBars.right
            )
            insets
        }

        // Request that the system send us window insets
        requestApplyInsets()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val profilerStartTime = profiler?.getProfilerTime()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        profiler?.addMarker(MEASURE_LAYOUT_DRAW_MARKER_NAME, profilerStartTime, "onMeasure (HomeActivity root)")
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val profilerStartTime = profiler?.getProfilerTime()
        super.onLayout(changed, l, t, r, b)
        profiler?.addMarker(MEASURE_LAYOUT_DRAW_MARKER_NAME, profilerStartTime, "onLayout (HomeActivity root)")
    }

    override fun dispatchDraw(canvas: Canvas) {
        // We instrument dispatchDraw, for drawing children, because LinearLayout never draws itself,
        // i.e. it never calls onDraw or draw.
        val profilerStartTime = profiler?.getProfilerTime()
        super.dispatchDraw(canvas)
        profiler?.addMarker(MEASURE_LAYOUT_DRAW_MARKER_NAME, profilerStartTime, "dispatchDraw (HomeActivity root)")
    }
}
