/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.ext

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.annotation.StringRes
import mozilla.components.support.locale.LocaleManager
import net.waterfox.android.WaterfoxApplication
import net.waterfox.android.components.Components
import net.waterfox.android.settings.advanced.getSelectedLocale
import java.lang.String.format
import java.util.*

/**
 * Get the BrowserApplication object from a context.
 */
val Context.application: WaterfoxApplication
    get() = applicationContext as WaterfoxApplication

/**
 * Get the requireComponents of this application.
 */
val Context.components: Components
    get() = application.components

fun Context.asActivity() = (this as? ContextThemeWrapper)?.baseContext as? Activity
    ?: this as? Activity

fun Context.getPreferenceKey(@StringRes resourceId: Int): String =
    resources.getString(resourceId)

fun Context.readBooleanPreference(key: String, defaultValue: Boolean) =
    settings().preferences.getBoolean(key, defaultValue)

fun Context.writeBooleanPreference(key: String, value: Boolean) =
    settings().preferences.edit().putBoolean(key, value).apply()

fun Context.readFloatPreference(key: String, defaultValue: Float) =
    settings().preferences.getFloat(key, defaultValue)

fun Context.writeFloatPreference(key: String, value: Float) =
    settings().preferences.edit().putFloat(key, value).apply()

fun Context.readStringPreference(key: String, defaultValue: String) =
    settings().preferences.getString(key, defaultValue)

fun Context.writeStringPreference(key: String, value: String) =
    settings().preferences.edit().putString(key, value).apply()

/**
 * Gets the Root View with an activity context
 *
 * @return ViewGroup? if it is able to get a root view from the context
 */
fun Context.getRootView(): View? =
    asActivity()?.window?.decorView?.findViewById<View>(android.R.id.content) as? ViewGroup

fun Context.settings() = components.settings

/**
 * Used to catch IllegalArgumentException that is thrown when
 * a string's placeholder is incorrectly formatted in a translation
 *
 * @return the formatted string in locale language or English as a fallback
 */
fun Context.getStringWithArgSafe(@StringRes resId: Int, formatArg: String): String {
    return try {
        format(getString(resId), formatArg)
    } catch (e: IllegalArgumentException) {
        // fallback to <en> string
        logDebug(
            "L10n",
            "String: " + resources.getResourceEntryName(resId) +
                " not properly formatted in: " + LocaleManager.getSelectedLocale(this).language
        )
        val config = resources.configuration
        config.setLocale(Locale("en"))
        val localizedContext: Context = this.createConfigurationContext(config)
        return format(localizedContext.getString(resId), formatArg)
    }
}

/**
 * Used to obtain a reference to an AccessibilityManager
 * @return accessibilityManager
 */
val Context.accessibilityManager: AccessibilityManager get() =
    getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

/**
 * Used to navigate to system notifications settings for app
 */
fun Context.navigateToNotificationsSettings() {
    val intent = Intent()
    intent.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            it.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            it.putExtra(Settings.EXTRA_APP_PACKAGE, this.packageName)
        } else {
            it.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            it.putExtra("app_package", this.packageName)
            it.putExtra("app_uid", this.applicationInfo.uid)
        }
    }
    startActivity(intent)
}
