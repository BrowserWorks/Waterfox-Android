/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.accessibility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.waterfox.android.R
import net.waterfox.android.ext.components
import net.waterfox.android.ext.settings
import net.waterfox.android.ext.showToolbar

/**
 * Displays font size controls for accessibility.
 *
 * Includes an automatic font sizing toggle. When turned on, font sizing follows the Android device settings.
 * When turned off, the font sizing can be controlled manually within the app.
 */
class AccessibilityFragment : Fragment() {

    lateinit var view: AccessibilitySettingsComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = AccessibilitySettingsComposeView(requireContext())
        return view
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_accessibility))
        setupPreferences()
    }

    private fun setupPreferences() {
        view.onAutoFontSizingChange = { useAutoSize ->
            val settings = requireContext().settings()
            val components = requireContext().components

            // Save the new setting value
            settings.shouldUseAutoSize = useAutoSize
            components.core.engine.settings.automaticFontSizeAdjustment = useAutoSize
            components.core.engine.settings.fontInflationEnabled = useAutoSize

            // If using manual sizing, update the engine settings with the local saved setting
            if (!useAutoSize) {
                components.core.engine.settings.fontSizeFactor = settings.fontSizeFactor
            }

            // Enable the manual sizing controls if automatic sizing is turned off.
            view.fontSizePreferenceEnabled = !useAutoSize

            // Reload the current session to reflect the new text scale
            components.useCases.sessionUseCases.reload()
        }
        view.onFontSizeChange = { newFontSize ->
            val settings = requireContext().settings()
            val components = requireContext().components

            // Save new text scale value. We assume auto sizing is off if this change listener was called.
            settings.fontSizeFactor = newFontSize
            components.core.engine.settings.fontSizeFactor = newFontSize

            // Reload the current session to reflect the new text scale
            components.useCases.sessionUseCases.reload()
        }
        view.fontSizePreferenceEnabled = !requireContext().settings().shouldUseAutoSize

        view.onForceZoomChange = { shouldForce ->
            requireContext().settings().forceEnableZoom = shouldForce
            requireContext().components.core.engine.settings.forceUserScalableContent = shouldForce
        }
    }

}
