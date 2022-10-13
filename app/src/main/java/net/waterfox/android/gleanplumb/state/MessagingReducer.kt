/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.gleanplumb.state

import net.waterfox.android.components.appstate.AppAction
import net.waterfox.android.components.appstate.AppAction.MessagingAction.ConsumeMessageToShow
import net.waterfox.android.components.appstate.AppAction.MessagingAction.UpdateMessageToShow
import net.waterfox.android.components.appstate.AppAction.MessagingAction.UpdateMessages
import net.waterfox.android.components.appstate.AppState
import net.waterfox.android.gleanplumb.MessagingState

/**
 * Reducer for [MessagingState].
 */
internal object MessagingReducer {
    fun reduce(state: AppState, action: AppAction.MessagingAction): AppState = when (action) {
        is UpdateMessageToShow -> {
            state.copy(
                messaging = state.messaging.copy(
                    messageToShow = action.message
                )
            )
        }
        is UpdateMessages -> {
            state.copy(
                messaging = state.messaging.copy(
                    messages = action.messages
                )
            )
        }
        is ConsumeMessageToShow -> {
            state.copy(
                messaging = state.messaging.copy(
                    messageToShow = null
                )
            )
        }
        else -> state
    }
}
