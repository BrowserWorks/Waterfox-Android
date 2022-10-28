/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.recentvisits.interactor

import io.mockk.mockk
import io.mockk.verify
import mozilla.components.concept.storage.DocumentType
import mozilla.components.concept.storage.HistoryMetadata
import mozilla.components.concept.storage.HistoryMetadataKey
import org.junit.Before
import org.junit.Test
import net.waterfox.android.home.recentbookmarks.controller.RecentBookmarksController
import net.waterfox.android.home.recentsyncedtabs.controller.RecentSyncedTabController
import net.waterfox.android.home.recenttabs.controller.RecentTabController
import net.waterfox.android.home.recentvisits.RecentlyVisitedItem.RecentHistoryGroup
import net.waterfox.android.home.recentvisits.RecentlyVisitedItem.RecentHistoryHighlight
import net.waterfox.android.home.recentvisits.controller.RecentVisitsController
import net.waterfox.android.home.sessioncontrol.DefaultSessionControlController
import net.waterfox.android.home.sessioncontrol.SessionControlInteractor

class RecentVisitsInteractorTest {
    private val defaultSessionControlController: DefaultSessionControlController =
        mockk(relaxed = true)
    private val recentTabController: RecentTabController = mockk(relaxed = true)
    private val recentSyncedTabController: RecentSyncedTabController = mockk(relaxed = true)
    private val recentBookmarksController: RecentBookmarksController = mockk(relaxed = true)
    private val recentVisitsController: RecentVisitsController = mockk(relaxed = true)

    private lateinit var interactor: SessionControlInteractor

    @Before
    fun setup() {
        interactor = SessionControlInteractor(
            defaultSessionControlController,
            recentTabController,
            recentSyncedTabController,
            recentBookmarksController,
            recentVisitsController
        )
    }

    @Test
    fun handleRecentHistoryGroupClicked() {
        val historyGroup =
            RecentHistoryGroup(
                title = "mozilla",
                historyMetadata = listOf(
                    HistoryMetadata(
                        key = HistoryMetadataKey("http://www.mozilla.com", null, null),
                        title = "mozilla",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        totalViewTime = 10,
                        documentType = DocumentType.Regular,
                        previewImageUrl = null
                    )
                )
            )

        interactor.onRecentHistoryGroupClicked(historyGroup)
        verify {
            recentVisitsController.handleRecentHistoryGroupClicked(historyGroup)
        }
    }

    @Test
    fun handleHistoryShowAllClicked() {
        interactor.onHistoryShowAllClicked()
        verify { recentVisitsController.handleHistoryShowAllClicked() }
    }

    @Test
    fun onRemoveRecentHistoryGroup() {
        val historyMetadataKey = HistoryMetadataKey(
            "http://www.mozilla.com",
            "mozilla",
            null
        )

        val historyGroup =
            RecentHistoryGroup(
                title = "mozilla",
                historyMetadata = listOf(
                    HistoryMetadata(
                        key = historyMetadataKey,
                        title = "mozilla",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        totalViewTime = 10,
                        documentType = DocumentType.Regular,
                        previewImageUrl = null
                    )
                )
            )

        interactor.onRemoveRecentHistoryGroup(historyGroup.title)

        verify {
            recentVisitsController.handleRemoveRecentHistoryGroup(historyGroup.title)
        }
    }

    @Test
    fun onRecentHistoryHighlightClicked() {
        val historyHighlight: RecentHistoryHighlight = mockk()

        interactor.onRecentHistoryHighlightClicked(historyHighlight)

        verify { recentVisitsController.handleRecentHistoryHighlightClicked(historyHighlight) }
    }

    @Test
    fun onRemoveRecentHistoryHighlight() {
        interactor.onRemoveRecentHistoryHighlight("url")

        verify { recentVisitsController.handleRemoveRecentHistoryHighlight("url") }
    }
}
