/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.sessioncontrol

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.top.sites.TopSite
import net.waterfox.android.components.appstate.AppState
import net.waterfox.android.ext.components
import net.waterfox.android.ext.settings
import net.waterfox.android.ext.shouldShowRecentTabs
import net.waterfox.android.home.Mode
import net.waterfox.android.home.OnboardingState
import net.waterfox.android.home.recentbookmarks.RecentBookmark
import net.waterfox.android.home.recentvisits.RecentlyVisitedItem
import net.waterfox.android.onboarding.JumpBackInCFRDialog
import net.waterfox.android.utils.Settings

// This method got a little complex with the addition of the tab tray feature flag
// When we remove the tabs from the home screen this will get much simpler again.
@Suppress("ComplexMethod", "LongParameterList")
@VisibleForTesting
internal fun normalModeAdapterItems(
    settings: Settings,
    topSites: List<TopSite>,
    collections: List<TabCollection>,
    expandedCollections: Set<Long>,
    recentBookmarks: List<RecentBookmark>,
    showCollectionsPlaceholder: Boolean,
    showRecentTab: Boolean,
    recentVisits: List<RecentlyVisitedItem>
): List<AdapterItem> {
    val items = mutableListOf<AdapterItem>()
    var shouldShowCustomizeHome = false

    // Add a synchronous, unconditional and invisible placeholder so home is anchored to the top when created.
    items.add(AdapterItem.TopPlaceholderItem)

    if (settings.showTopSitesFeature && topSites.isNotEmpty()) {
        items.add(AdapterItem.TopSitePager(topSites))
    }

    if (showRecentTab) {
        shouldShowCustomizeHome = true
        items.add(AdapterItem.RecentTabsHeader)
        items.add(AdapterItem.RecentTabItem)
    }

    if (settings.showRecentBookmarksFeature && recentBookmarks.isNotEmpty()) {
        shouldShowCustomizeHome = true
        items.add(AdapterItem.RecentBookmarksHeader)
        items.add(AdapterItem.RecentBookmarks)
    }

    if (settings.historyMetadataUIFeature && recentVisits.isNotEmpty()) {
        shouldShowCustomizeHome = true
        items.add(AdapterItem.RecentVisitsHeader)
        items.add(AdapterItem.RecentVisitsItems)
    }

    if (collections.isEmpty()) {
        if (showCollectionsPlaceholder) {
            items.add(AdapterItem.NoCollectionsMessage)
        }
    } else {
        showCollections(collections, expandedCollections, items)
    }

    if (shouldShowCustomizeHome) {
        items.add(AdapterItem.CustomizeHomeButton)
    }

    items.add(AdapterItem.BottomSpacer)

    return items
}

private fun showCollections(
    collections: List<TabCollection>,
    expandedCollections: Set<Long>,
    items: MutableList<AdapterItem>
) {
    // If the collection is expanded, we want to add all of its tabs beneath it in the adapter
    items.add(AdapterItem.CollectionHeader)
    collections.map {
        AdapterItem.CollectionItem(it, expandedCollections.contains(it.id))
    }.forEach {
        items.add(it)
        if (it.expanded) {
            items.addAll(collectionTabItems(it.collection))
        }
    }
}

private fun privateModeAdapterItems() = listOf(AdapterItem.PrivateBrowsingDescription)

private fun onboardingAdapterItems(onboardingState: OnboardingState): List<AdapterItem> {
    val items: MutableList<AdapterItem> = mutableListOf(AdapterItem.OnboardingHeader)

    items.addAll(
        listOf(
            AdapterItem.OnboardingThemePicker,
            AdapterItem.OnboardingToolbarPositionPicker,
            AdapterItem.OnboardingTrackingProtection
        )
    )
    // Customize FxA items based on where we are with the account state:
    items.addAll(
        when (onboardingState) {
            OnboardingState.SignedOutNoAutoSignIn -> {
                listOf(
                    AdapterItem.OnboardingManualSignIn
                )
            }
            OnboardingState.SignedIn -> listOf()
        }
    )

    items.addAll(
        listOf(
            AdapterItem.OnboardingPrivacyNotice,
            AdapterItem.OnboardingFinish,
            AdapterItem.BottomSpacer
        )
    )

    return items
}

private fun AppState.toAdapterList(settings: Settings): List<AdapterItem> = when (mode) {
    is Mode.Normal -> normalModeAdapterItems(
        settings,
        topSites,
        collections,
        expandedCollections,
        recentBookmarks,
        showCollectionPlaceholder,
        shouldShowRecentTabs(settings),
        recentHistory
    )
    is Mode.Private -> privateModeAdapterItems()
    is Mode.Onboarding -> onboardingAdapterItems(mode.state)
}

@VisibleForTesting
internal fun AppState.shouldShowHomeOnboardingDialog(settings: Settings): Boolean {
    val isAnySectionsVisible = recentTabs.isNotEmpty() || recentBookmarks.isNotEmpty() ||
        recentHistory.isNotEmpty()
    return isAnySectionsVisible && !settings.hasShownHomeOnboardingDialog
}

private fun collectionTabItems(collection: TabCollection) =
    collection.tabs.mapIndexed { index, tab ->
        AdapterItem.TabInCollectionItem(collection, tab, index == collection.tabs.lastIndex)
    }

class SessionControlView(
    val containerView: View,
    viewLifecycleOwner: LifecycleOwner,
    internal val interactor: SessionControlInteractor
) {

    val view: RecyclerView = containerView as RecyclerView

    private val sessionControlAdapter = SessionControlAdapter(
        interactor,
        viewLifecycleOwner,
        containerView.context.components
    )

    init {
        view.apply {
            adapter = sessionControlAdapter
            layoutManager = object : LinearLayoutManager(containerView.context) {
                override fun onLayoutCompleted(state: RecyclerView.State?) {
                    super.onLayoutCompleted(state)

                    JumpBackInCFRDialog(view).showIfNeeded()
                }
            }
        }
    }

    fun update(state: AppState) {
        if (state.shouldShowHomeOnboardingDialog(view.context.settings())) {
            interactor.showOnboardingDialog()
        }

        sessionControlAdapter.submitList(state.toAdapterList(view.context.settings()))
    }
}
