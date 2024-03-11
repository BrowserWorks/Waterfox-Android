/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray.browser

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import mozilla.components.lib.state.ext.observeAsComposableState
import net.waterfox.android.R
import net.waterfox.android.components.WaterfoxSnackbar
import net.waterfox.android.components.components
import net.waterfox.android.compose.ComposeViewHolder
import net.waterfox.android.tabstray.TabsTrayFragment
import net.waterfox.android.tabstray.TabsTrayState
import net.waterfox.android.tabstray.TabsTrayStore
import net.waterfox.android.tabstray.TrayPagerAdapter
import net.waterfox.android.tabstray.inactivetabs.InactiveTabsList

/**
 * The [ComposeViewHolder] for displaying the section of inactive tabs in [TrayPagerAdapter].
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param lifecycleOwner [LifecycleOwner] to which this Composable will be tied to.
 * @param tabsTrayStore [TabsTrayStore] used to listen for changes to [TabsTrayState.inactiveTabs].
 * @param interactor [InactiveTabsInteractor] used to respond to interactions with the inactive tabs header
 * and the auto close dialog.
 */
class InactiveTabViewHolder(
    composeView: ComposeView,
    lifecycleOwner: LifecycleOwner,
    private val tabsTrayStore: TabsTrayStore,
    private val interactor: InactiveTabsInteractor,
) : ComposeViewHolder(composeView, lifecycleOwner) {

    @Composable
    override fun Content() {
        val expanded = components.appStore
            .observeAsComposableState { state -> state.inactiveTabsExpanded }.value ?: false
        val inactiveTabs = tabsTrayStore
            .observeAsComposableState { state -> state.inactiveTabs }.value ?: emptyList()
        val showInactiveTabsAutoCloseDialog =
            components.settings.shouldShowInactiveTabsAutoCloseDialog(inactiveTabs.size)
        var showAutoClosePrompt by remember { mutableStateOf(showInactiveTabsAutoCloseDialog) }

        if (inactiveTabs.isNotEmpty()) {
            InactiveTabsList(
                inactiveTabs = inactiveTabs,
                expanded = expanded,
                showAutoCloseDialog = showAutoClosePrompt,
                onHeaderClick = { interactor.onInactiveTabsHeaderClicked(!expanded) },
                onDeleteAllButtonClick = interactor::onDeleteAllInactiveTabsClicked,
                onAutoCloseDismissClick = {
                    interactor.onAutoCloseDialogCloseButtonClicked()
                    showAutoClosePrompt = !showAutoClosePrompt
                },
                onEnableAutoCloseClick = {
                    interactor.onEnableAutoCloseClicked()
                    showAutoClosePrompt = !showAutoClosePrompt
                    showConfirmationSnackbar()
                },
                onTabClick = interactor::onInactiveTabClicked,
                onTabCloseClick = interactor::onInactiveTabClosed,
            )
        }
    }

    override val allowPrivateTheme: Boolean
        get() = false

    private fun showConfirmationSnackbar() {
        val context = composeView.context
        val text = context.getString(R.string.inactive_tabs_auto_close_message_snackbar)
        val snackbar = WaterfoxSnackbar.make(
            view = composeView,
            duration = WaterfoxSnackbar.LENGTH_SHORT,
            isDisplayedWithBrowserToolbar = true,
        ).setText(text)
        snackbar.view.elevation = TabsTrayFragment.ELEVATION
        snackbar.show()
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}
