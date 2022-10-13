/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.sessioncontrol.viewholders.onboarding

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.service.glean.private.NoExtras
import net.waterfox.android.GleanMetrics.Onboarding
import net.waterfox.android.R
import net.waterfox.android.databinding.OnboardingPrivacyNoticeBinding
import net.waterfox.android.home.sessioncontrol.OnboardingInteractor

class OnboardingPrivacyNoticeViewHolder(
    view: View,
    private val interactor: OnboardingInteractor
) : RecyclerView.ViewHolder(view) {

    init {
        val binding = OnboardingPrivacyNoticeBinding.bind(view)
        binding.headerText.setOnboardingIcon(R.drawable.ic_info)

        val appName = view.context.getString(R.string.app_name)
        binding.descriptionText.text = view.context.getString(R.string.onboarding_privacy_notice_description2, appName)

        binding.readButton.setOnClickListener {
            Onboarding.privacyNotice.record(NoExtras())
            interactor.onReadPrivacyNoticeClicked()
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.onboarding_privacy_notice
    }
}
