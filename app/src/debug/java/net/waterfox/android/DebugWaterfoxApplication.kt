/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android

import android.os.StrictMode
import androidx.preference.PreferenceManager
import leakcanary.AppWatcher
import leakcanary.LeakCanary
import net.waterfox.android.ext.application
import net.waterfox.android.ext.getPreferenceKey

class DebugWaterfoxApplication : WaterfoxApplication() {

    override fun setupLeakCanary() {
        if (!AppWatcher.isInstalled) {
            AppWatcher.manualInstall(
                application = application,
                watchersToInstall = AppWatcher.appDefaultWatchers(application)
            )
        }

        val isEnabled = components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getPreferenceKey(R.string.pref_key_leakcanary), true)
        }

        updateLeakCanaryState(isEnabled)
    }

    override fun updateLeakCanaryState(isEnabled: Boolean) {
        LeakCanary.showLeakDisplayActivityLauncherIcon(isEnabled)
        components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            LeakCanary.config = LeakCanary.config.copy(dumpHeap = isEnabled)
        }
    }
}
