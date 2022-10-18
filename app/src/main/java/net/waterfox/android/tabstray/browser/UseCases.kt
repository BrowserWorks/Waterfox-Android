/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray.browser

import mozilla.components.feature.tabs.TabsUseCases

class SelectTabUseCaseWrapper(
    private val selectTab: TabsUseCases.SelectTabUseCase,
    private val onSelect: (String) -> Unit
) : TabsUseCases.SelectTabUseCase {
    operator fun invoke(tabId: String, source: String? = null) {
        selectTab(tabId)
        onSelect(tabId)
    }

    override fun invoke(tabId: String) {
        invoke(tabId, null)
    }
}

class RemoveTabUseCaseWrapper(
    private val onRemove: (String) -> Unit,
) : TabsUseCases.RemoveTabUseCase {
    operator fun invoke(tabId: String, source: String? = null) {
        onRemove(tabId)
    }

    override fun invoke(tabId: String) {
        invoke(tabId, null)
    }
}
