/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.history

import mozilla.components.concept.engine.EngineSession.LoadUrlFlags
import net.waterfox.android.library.history.awesomebar.AwesomeBarInteractor
import net.waterfox.android.library.history.toolbar.ToolbarInteractor

/**
 * Interactor for the history search
 * Provides implementations for the AwesomeBarView and ToolbarView
 */
class HistorySearchDialogInteractor(
    private val historySearchController: HistorySearchDialogController
) : AwesomeBarInteractor, ToolbarInteractor {

    override fun onEditingCanceled() {
        historySearchController.handleEditingCancelled()
    }

    override fun onTextChanged(text: String) {
        historySearchController.handleTextChanged(text)
    }

    override fun onUrlTapped(url: String, flags: LoadUrlFlags) {
        historySearchController.handleUrlTapped(url, flags)
    }
}
