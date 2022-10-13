/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home

import android.content.Context
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import net.waterfox.android.browser.browsingmode.BrowsingMode
import net.waterfox.android.browser.browsingmode.BrowsingModeManager
import net.waterfox.android.ext.components
import net.waterfox.android.onboarding.WaterfoxOnboarding

/**
 * Describes various states of the home fragment UI.
 */
sealed class Mode {
    object Normal : Mode()
    object Private : Mode()
    data class Onboarding(val state: OnboardingState) : Mode()

    companion object {
        fun fromBrowsingMode(browsingMode: BrowsingMode) = when (browsingMode) {
            BrowsingMode.Normal -> Normal
            BrowsingMode.Private -> Private
        }
    }
}

/**
 * Describes various onboarding states.
 */
sealed class OnboardingState {
    // Signed out, without an option to auto-login using a shared FxA account.
    object SignedOutNoAutoSignIn : OnboardingState()
    // Signed in.
    object SignedIn : OnboardingState()
}

class CurrentMode(
    private val context: Context,
    private val onboarding: WaterfoxOnboarding,
    private val browsingModeManager: BrowsingModeManager,
    private val dispatchModeChanges: (mode: Mode) -> Unit
) : AccountObserver {

    private val accountManager by lazy { context.components.backgroundServices.accountManager }

    fun getCurrentMode() = if (onboarding.userHasBeenOnboarded()) {
        Mode.fromBrowsingMode(browsingModeManager.mode)
    } else {
        val account = accountManager.authenticatedAccount()
        if (account != null) {
            Mode.Onboarding(OnboardingState.SignedIn)
        } else {
            Mode.Onboarding(OnboardingState.SignedOutNoAutoSignIn)
        }
    }

    fun emitModeChanges() {
        dispatchModeChanges(getCurrentMode())
    }

    override fun onAuthenticated(account: OAuthAccount, authType: AuthType) = emitModeChanges()
    override fun onAuthenticationProblems() = emitModeChanges()
    override fun onLoggedOut() = emitModeChanges()
    override fun onProfileUpdated(profile: Profile) = emitModeChanges()
}
