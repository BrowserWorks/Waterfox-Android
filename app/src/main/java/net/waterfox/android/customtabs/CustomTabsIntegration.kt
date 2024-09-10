/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.customtabs

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.display.DisplayToolbar
import mozilla.components.feature.customtabs.CustomTabsToolbarFeature
import mozilla.components.feature.customtabs.CustomTabsToolbarListeners
import mozilla.components.feature.tabs.CustomTabsUseCases
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import net.waterfox.android.R
import net.waterfox.android.components.toolbar.ToolbarMenu
import net.waterfox.android.components.toolbar.interactor.BrowserToolbarInteractor
import net.waterfox.android.ext.settings
import net.waterfox.android.utils.Settings

class CustomTabsIntegration(
    store: BrowserStore,
    useCases: CustomTabsUseCases,
    toolbar: BrowserToolbar,
    sessionId: String,
    private val activity: Activity,
    private val interactor: BrowserToolbarInteractor,
    onItemTapped: (ToolbarMenu.Item) -> Unit = {},
    shouldReverseItems: Boolean,
    isPrivate: Boolean,
) : LifecycleAwareFeature, UserInteractionHandler {

    init {
        // Remove toolbar shadow
        toolbar.elevation = 0f

        toolbar.display.displayIndicatorSeparator = false
        toolbar.display.indicators = listOf(
            DisplayToolbar.Indicators.SECURITY
        )

        // If in private mode, override toolbar background to use private color
        // See #5334
        if (isPrivate) {
            toolbar.background = getDrawable(activity, R.drawable.toolbar_background)
        }
    }

    private val customTabToolbarMenu by lazy {
        CustomTabToolbarMenu(
            activity,
            store,
            sessionId,
            shouldReverseItems,
            onItemTapped = onItemTapped
        )
    }

    private val feature = CustomTabsToolbarFeature(
        store = store,
        toolbar = toolbar,
        sessionId = sessionId,
        useCases = useCases,
        menuBuilder = customTabToolbarMenu.menuBuilder,
        menuItemIndex = START_OF_MENU_ITEMS_INDEX,
        window = activity.window,
        customTabsToolbarListeners = CustomTabsToolbarListeners(
            shareListener = { interactor.onBrowserToolbarMenuItemTapped(ToolbarMenu.Item.Share) },
            refreshListener = {
                interactor.onBrowserToolbarMenuItemTapped(
                    ToolbarMenu.Item.Reload(
                        bypassCache = false,
                    ),
                )
            },
        ),
        closeListener = { activity.finishAndRemoveTask() },
        appNightMode = activity.settings().getAppNightMode(),
        forceActionButtonTinting = isPrivate,
    )

    private fun Settings.getAppNightMode() = if (shouldFollowDeviceTheme) {
        MODE_NIGHT_FOLLOW_SYSTEM
    } else {
        if (shouldUseLightTheme) {
            MODE_NIGHT_NO
        } else {
            MODE_NIGHT_YES
        }
    }

    override fun start() = feature.start()
    override fun stop() = feature.stop()
    override fun onBackPressed() = feature.onBackPressed()

    companion object {
        private const val START_OF_MENU_ITEMS_INDEX = 2
    }
}
