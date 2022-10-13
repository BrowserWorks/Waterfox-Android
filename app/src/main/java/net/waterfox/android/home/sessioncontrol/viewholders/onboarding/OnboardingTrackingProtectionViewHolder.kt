/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.sessioncontrol.viewholders.onboarding

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.waterfox.android.GleanMetrics.Onboarding
import net.waterfox.android.R
import net.waterfox.android.databinding.OnboardingTrackingProtectionBinding
import net.waterfox.android.ext.components
import net.waterfox.android.ext.settings
import net.waterfox.android.onboarding.OnboardingRadioButton
import net.waterfox.android.utils.view.addToRadioGroup

class OnboardingTrackingProtectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private var standardTrackingProtection: OnboardingRadioButton
    private var strictTrackingProtection: OnboardingRadioButton

    init {
        val binding = OnboardingTrackingProtectionBinding.bind(view)
        binding.headerText.setOnboardingIcon(R.drawable.ic_onboarding_tracking_protection)

        standardTrackingProtection = binding.trackingProtectionStandardOption
        strictTrackingProtection = binding.trackingProtectionStrictDefault

        val appName = view.context.getString(R.string.app_name)
        binding.descriptionText.text = view.context.getString(
            R.string.onboarding_tracking_protection_description_4, appName
        )

        val isTrackingProtectionEnabled = view.context.settings().shouldUseTrackingProtection
        setupRadioGroup(isTrackingProtectionEnabled)
        updateRadioGroupState(isTrackingProtectionEnabled)
    }

    private fun setupRadioGroup(isChecked: Boolean) {

        updateRadioGroupState(isChecked)

        addToRadioGroup(standardTrackingProtection, strictTrackingProtection)

        strictTrackingProtection.isChecked =
            itemView.context.settings().useStrictTrackingProtection
        standardTrackingProtection.isChecked =
            !itemView.context.settings().useStrictTrackingProtection

        standardTrackingProtection.onClickListener {
            updateTrackingProtectionPolicy()
            Onboarding.prefToggledTrackingProt.record(
                Onboarding.PrefToggledTrackingProtExtra(
                    Settings.STANDARD.name
                )
            )
        }

        strictTrackingProtection.onClickListener {
            updateTrackingProtectionPolicy()
            Onboarding.prefToggledTrackingProt.record(
                Onboarding.PrefToggledTrackingProtExtra(
                    Settings.STRICT.name
                )
            )
        }
    }

    private fun updateRadioGroupState(isChecked: Boolean) {
        standardTrackingProtection.isEnabled = isChecked
        strictTrackingProtection.isEnabled = isChecked
    }

    private fun updateTrackingProtectionPolicy() {
        itemView.context?.components?.let {
            val policy = it.core.trackingProtectionPolicyFactory
                .createTrackingProtectionPolicy()
            it.useCases.settingsUseCases.updateTrackingProtection.invoke(policy)
            it.useCases.sessionUseCases.reload.invoke()
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.onboarding_tracking_protection
        // Tracking protection policy types used for telemetry
        enum class Settings { STRICT, STANDARD }
    }
}
