/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.waterfox.android.R

class BottomSpacerViewHolder(
    view: View,
) : RecyclerView.ViewHolder(view) {
    companion object {
        val LAYOUT_ID = R.layout.bottom_spacer
    }
}
