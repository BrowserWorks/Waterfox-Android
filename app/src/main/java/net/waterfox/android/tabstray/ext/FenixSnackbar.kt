/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray.ext

import android.view.View
import net.waterfox.android.R
import net.waterfox.android.components.WaterfoxSnackbar
import net.waterfox.android.tabstray.TabsTrayFragment.Companion.ELEVATION

internal fun WaterfoxSnackbar.collectionMessage(
    tabSize: Int,
    isNewCollection: Boolean = false,
): WaterfoxSnackbar {
    val stringRes = when {
        isNewCollection -> {
            R.string.create_collection_tabs_saved_new_collection
        }
        tabSize > 1 -> {
            R.string.create_collection_tabs_saved
        }
        else -> {
            R.string.create_collection_tab_saved
        }
    }
    setText(context.getString(stringRes))
    return this
}

internal fun WaterfoxSnackbar.bookmarkMessage(
    tabSize: Int,
): WaterfoxSnackbar {
    val stringRes = when {
        tabSize > 1 -> {
            R.string.snackbar_message_bookmarks_saved
        }
        else -> {
            R.string.bookmark_saved_snackbar
        }
    }
    setText(context.getString(stringRes))
    return this
}

internal inline fun WaterfoxSnackbar.anchorWithAction(
    anchor: View?,
    crossinline action: () -> Unit,
): WaterfoxSnackbar {
    anchorView = anchor
    view.elevation = ELEVATION

    setAction(context.getString(R.string.create_collection_view)) {
        action.invoke()
    }

    return this
}

internal fun WaterfoxSnackbar.Companion.make(view: View) = make(
    duration = LENGTH_LONG,
    isDisplayedWithBrowserToolbar = true,
    view = view,
)
