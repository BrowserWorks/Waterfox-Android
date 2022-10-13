/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.sessioncontrol.viewholders.onboarding

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.RecyclerView
import net.waterfox.android.GleanMetrics.Onboarding
import net.waterfox.android.R
import net.waterfox.android.databinding.OnboardingThemePickerBinding
import net.waterfox.android.ext.components
import net.waterfox.android.ext.settings
import net.waterfox.android.onboarding.OnboardingRadioButton
import net.waterfox.android.utils.view.addToRadioGroup

class OnboardingThemePickerViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    init {
        val binding = OnboardingThemePickerBinding.bind(view)
        val radioLightTheme = binding.themeLightRadioButton
        val radioDarkTheme = binding.themeDarkRadioButton
        val radioFollowDeviceTheme = binding.themeAutomaticRadioButton

        radioFollowDeviceTheme.key = if (SDK_INT >= Build.VERSION_CODES.P) {
            R.string.pref_key_follow_device_theme
        } else {
            R.string.pref_key_auto_battery_theme
        }

        addToRadioGroup(
            radioLightTheme,
            radioDarkTheme,
            radioFollowDeviceTheme
        )
        radioLightTheme.addIllustration(binding.themeLightImage)
        radioDarkTheme.addIllustration(binding.themeDarkImage)

        binding.themeDarkImage.setOnClickListener {
            Onboarding.prefToggledThemePicker.record(
                Onboarding.PrefToggledThemePickerExtra(
                    Theme.DARK.name
                )
            )
            radioDarkTheme.performClick()
        }

        binding.themeLightImage.setOnClickListener {
            Onboarding.prefToggledThemePicker.record(
                Onboarding.PrefToggledThemePickerExtra(
                    Theme.LIGHT.name
                )
            )
            radioLightTheme.performClick()
        }

        val automaticTitle = view.context.getString(R.string.onboarding_theme_automatic_title)
        val automaticSummary = view.context.getString(R.string.onboarding_theme_automatic_summary)
        binding.clickableRegionAutomatic.contentDescription = "$automaticTitle $automaticSummary"

        binding.clickableRegionAutomatic.setOnClickListener {
            Onboarding.prefToggledThemePicker.record(
                Onboarding.PrefToggledThemePickerExtra(
                    Theme.FOLLOW_DEVICE.name
                )
            )
            radioFollowDeviceTheme.performClick()
        }

        radioLightTheme.onClickListener {
            Onboarding.prefToggledThemePicker.record(
                Onboarding.PrefToggledThemePickerExtra(
                    Theme.LIGHT.name
                )
            )
            setNewTheme(AppCompatDelegate.MODE_NIGHT_NO)
        }

        radioDarkTheme.onClickListener {
            Onboarding.prefToggledThemePicker.record(
                Onboarding.PrefToggledThemePickerExtra(
                    Theme.DARK.name
                )
            )
            setNewTheme(AppCompatDelegate.MODE_NIGHT_YES)
        }

        radioFollowDeviceTheme.onClickListener {
            Onboarding.prefToggledThemePicker.record(
                Onboarding.PrefToggledThemePickerExtra(
                    Theme.FOLLOW_DEVICE.name
                )
            )
            if (SDK_INT >= Build.VERSION_CODES.P) {
                setNewTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                setNewTheme(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
        }

        with(view.context.settings()) {
            val radio: OnboardingRadioButton = when {
                shouldUseLightTheme -> {
                    radioLightTheme
                }
                shouldUseDarkTheme -> {
                    radioDarkTheme
                }
                else -> {
                    radioFollowDeviceTheme
                }
            }
            radio.updateRadioValue(true)
        }
    }

    private fun setNewTheme(mode: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == mode) return
        AppCompatDelegate.setDefaultNightMode(mode)
        with(itemView.context.components) {
            core.engine.settings.preferredColorScheme = core.getPreferredColorScheme()
            useCases.sessionUseCases.reload.invoke()
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.onboarding_theme_picker
        // The theme used for telemetry
        enum class Theme { LIGHT, DARK, FOLLOW_DEVICE }
    }
}
