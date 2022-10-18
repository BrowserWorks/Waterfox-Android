/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.topsites

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.feature.top.sites.TopSite
import net.waterfox.android.R
import net.waterfox.android.databinding.TopSiteItemBinding
import net.waterfox.android.ext.bitmapForUrl
import net.waterfox.android.ext.components
import net.waterfox.android.ext.loadIntoView
import net.waterfox.android.home.sessioncontrol.TopSiteInteractor
import net.waterfox.android.settings.SupportUtils
import net.waterfox.android.utils.view.ViewHolder

class TopSiteItemViewHolder(
    view: View,
    private val viewLifecycleOwner: LifecycleOwner,
    private val interactor: TopSiteInteractor
) : ViewHolder(view) {
    private lateinit var topSite: TopSite
    private val binding = TopSiteItemBinding.bind(view)

    init {
        binding.topSiteItem.setOnLongClickListener {
            interactor.onTopSiteMenuOpened()

            val topSiteMenu = TopSiteItemMenu(
                context = view.context,
                topSite = topSite
            ) { item ->
                when (item) {
                    is TopSiteItemMenu.Item.OpenInPrivateTab -> interactor.onOpenInPrivateTabClicked(
                        topSite
                    )
                    is TopSiteItemMenu.Item.RenameTopSite -> interactor.onRenameTopSiteClicked(
                        topSite
                    )
                    is TopSiteItemMenu.Item.RemoveTopSite -> interactor.onRemoveTopSiteClicked(
                        topSite
                    )
                    is TopSiteItemMenu.Item.Settings -> interactor.onSettingsClicked()
                    is TopSiteItemMenu.Item.SponsorPrivacy -> interactor.onSponsorPrivacyClicked()
                }
            }
            val menu = topSiteMenu.menuBuilder.build(view.context).show(anchor = it)

            it.setOnTouchListener @SuppressLint("ClickableViewAccessibility") { v, event ->
                onTouchEvent(v, event, menu)
            }

            true
        }
    }

    fun bind(topSite: TopSite, position: Int) {
        binding.topSiteItem.setOnClickListener {
            interactor.onSelectTopSite(topSite, position)
        }

        binding.topSiteTitle.text = topSite.title

        if (topSite is TopSite.Pinned || topSite is TopSite.Default) {
            val pinIndicator = getDrawable(itemView.context, R.drawable.ic_new_pin)
            binding.topSiteTitle.setCompoundDrawablesWithIntrinsicBounds(pinIndicator, null, null, null)
        } else {
            binding.topSiteTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }

        if (topSite is TopSite.Provided) {
            binding.topSiteSubtitle.isVisible = true

            viewLifecycleOwner.lifecycleScope.launch(IO) {
                itemView.context.components.core.client.bitmapForUrl(topSite.imageUrl)?.let { bitmap ->
                    withContext(Main) {
                        binding.faviconImage.setImageBitmap(bitmap)
                    }
                }
            }
        } else {
            when (topSite.url) {
                SupportUtils.POCKET_TRENDING_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_pocket))
                }
                SupportUtils.BAIDU_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_baidu))
                }
                SupportUtils.JD_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_jd))
                }
                SupportUtils.PDD_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_pdd))
                }
                SupportUtils.TC_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_tc))
                }
                SupportUtils.MEITUAN_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_meituan))
                }
                else -> {
                    itemView.context.components.core.icons.loadIntoView(binding.faviconImage, topSite.url)
                }
            }
        }

        this.topSite = topSite
    }

    private fun onTouchEvent(
        v: View,
        event: MotionEvent,
        menu: PopupWindow
    ): Boolean {
        if (event.action == MotionEvent.ACTION_CANCEL) {
            menu.dismiss()
        }
        return v.onTouchEvent(event)
    }

    companion object {
        const val LAYOUT_ID = R.layout.top_site_item
    }
}
