/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.sessioncontrol.viewholders.onboarding

import android.view.View
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import net.waterfox.android.R
import net.waterfox.android.components.accounts.WaterfoxFxAEntryPoint
import net.waterfox.android.databinding.OnboardingManualSigninBinding
import net.waterfox.android.home.HomeFragmentDirections

class OnboardingManualSignInViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private var binding: OnboardingManualSigninBinding = OnboardingManualSigninBinding.bind(view)

    init {
        binding.fxaSignInButton.setOnClickListener {
            val directions = HomeFragmentDirections.actionGlobalTurnOnSync(
                entrypoint = WaterfoxFxAEntryPoint.OnboardingManualSignIn,
            )
            Navigation.findNavController(view).navigate(directions)
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.onboarding_manual_signin
    }
}
