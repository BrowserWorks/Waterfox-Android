/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.intent

import android.content.Intent
import androidx.navigation.NavController
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.utils.toSafeIntent
import net.waterfox.android.BrowserDirection
import net.waterfox.android.HomeActivity

/**
 * The [net.waterfox.android.IntentReceiverActivity] may set the [HomeActivity.OPEN_TO_BROWSER] flag
 * when the browser should be opened in response to an intent.
 */
class OpenBrowserIntentProcessor(
    private val activity: HomeActivity,
    private val getIntentSessionId: (SafeIntent) -> String?
) : HomeIntentProcessor {

    override fun process(intent: Intent, navController: NavController, out: Intent): Boolean {
        return if (intent.extras?.getBoolean(HomeActivity.OPEN_TO_BROWSER) == true) {
            out.putExtra(HomeActivity.OPEN_TO_BROWSER, false)

            activity.openToBrowser(BrowserDirection.FromGlobal, getIntentSessionId(intent.toSafeIntent()))
            true
        } else {
            false
        }
    }
}
