/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import net.waterfox.android.gradle.ext.execReadStandardOutOrThrow
import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.Year
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

object Config {
    // Synchronized build configuration for all modules
    const val compileSdkVersion = 36
    const val minSdkVersion = 21
    const val targetSdkVersion = 35

    @JvmStatic
    private fun generateDebugVersionName(project: Project): String {
        val today = Date()
        // Append the year (2 digits) and week in year (2 digits). This will make it easier to distinguish versions and
        // identify ancient versions when debugging issues. However this will still keep the same version number during
        // the week so that we do not end up with a lot of versions in tools like Sentry. As an extra this matches the
        // sections we use in the changelog (weeks).
        return if (project.hasProperty("versionName")) project.property("versionName") as String else SimpleDateFormat("1.0.yyww", Locale.US).format(today)
    }

    @JvmStatic
    fun releaseVersionName(project: Project): String {
        // Note: release builds must have the `versionName` set. However, the gradle ecosystem makes this hard to
        // ergonomically validate (sometimes IDEs default to a release variant and mysteriously fail due to the
        // validation, sometimes devs just need a release build and specifying project properties is annoying in IDEs),
        // so instead we'll allow the `versionName` to silently default to an empty string.
        return if (project.hasProperty("versionName")) project.property("versionName") as String else ""
    }

    /**
     * Generate a build date that follows the ISO-8601 format
     */
    @JvmStatic
    fun generateBuildDate(): String {
        return LocalDateTime.now().toString()
    }

    /**
     * Return the version code that consists of the current year and minutes since
     * the start of the year. E.g. 2024-09-13 8:49 -> 2024369049
     */
    @JvmStatic
    fun generateFennecVersionCode(): Int {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val year = Year.of(now.year)
        val startOfYear = OffsetDateTime.of(year.atDay(1), LocalTime.MIN, ZoneOffset.UTC)
        val minutes = Duration.between(startOfYear, now).toMinutes()
        return (year.value * 1000000 + minutes).toInt()
    }

    /**
     * Returns the git hash of the currently checked out revision. If there are uncommitted changes,
     * a "+" will be appended to the hash, e.g. "c8ba05ad0+".
     */
    @JvmStatic
    fun getGitHash(): String {
        val revisionCmd = arrayOf("git", "rev-parse", "--short", "HEAD")
        val revision = Runtime.getRuntime().execReadStandardOutOrThrow(revisionCmd)

        // Append "+" if there are uncommitted changes in the working directory.
        val statusCmd = arrayOf("git", "status", "--porcelain=v2")
        val status = Runtime.getRuntime().execReadStandardOutOrThrow(statusCmd)
        val hasUnstagedChanges = status.isNotBlank()
        val statusSuffix = if (hasUnstagedChanges) "+" else ""

        return "${revision}${statusSuffix}"
    }
}
