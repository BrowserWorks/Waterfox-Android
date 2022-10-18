/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.gleanplumb

import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import net.waterfox.android.BuildConfig
import net.waterfox.android.HomeActivity
import net.waterfox.android.components.AppStore
import net.waterfox.android.components.appstate.AppAction.MessagingAction.MessageClicked
import net.waterfox.android.components.appstate.AppAction.MessagingAction.MessageDismissed

/**
 * Handles default interactions with the ui of GleanPlumb messages.
 */
class DefaultMessageController(
    private val appStore: AppStore,
    private val messagingStorage: NimbusMessagingStorage,
    private val homeActivity: HomeActivity
) : MessageController {

    override fun onMessagePressed(message: Message) {
        val result = messagingStorage.getMessageAction(message)
        val action = result.second
        handleAction(action)
        appStore.dispatch(MessageClicked(message))
    }

    override fun onMessageDismissed(message: Message) {
        appStore.dispatch(MessageDismissed(message))
    }

    @VisibleForTesting
    internal fun handleAction(action: String): Intent {
        val partialAction = if (action.startsWith("http", ignoreCase = true)) {
            "://open?url=${Uri.encode(action)}"
        } else {
            action
        }
        val intent =
            Intent(Intent.ACTION_VIEW, "${BuildConfig.DEEP_LINK_SCHEME}$partialAction".toUri())
        homeActivity.processIntent(intent)

        return intent
    }
}
