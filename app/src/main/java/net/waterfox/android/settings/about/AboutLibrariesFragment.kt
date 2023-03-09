/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.about

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import net.waterfox.android.R
import net.waterfox.android.databinding.FragmentAboutLibrariesBinding
import net.waterfox.android.ext.showToolbar
import java.nio.charset.Charset
import java.util.*

/**
 * Displays the licenses of all the libraries used by Waterfox.
 *
 * This is a re-implementation of play-services-oss-licenses library.
 * We can't use the official implementation in the OSS flavor of Waterfox
 * because it is proprietary and closed-source.
 *
 * There are popular FLOSS alternatives to Google's plugin and library
 * such as AboutLibraries (https://github.com/mikepenz/AboutLibraries)
 * but we considered the risk of introducing such third-party dependency
 * to Waterfox too high. Therefore, we use Google's gradle plugin to
 * extract the dependencies and their licenses, and this fragment
 * to show the extracted licenses to the end-user.
 */
class AboutLibrariesFragment : Fragment(R.layout.fragment_about_libraries) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentAboutLibrariesBinding.bind(view)
        binding.aboutLibrariesContent.libraries = parseLibraries()
    }

    override fun onResume() {
        super.onResume()
        val appName = getString(R.string.app_name)
        showToolbar(getString(R.string.open_source_licenses_title, appName))
    }

    private fun parseLibraries(): List<LibraryItem> {
        /*
            The gradle plugin "oss-licenses-plugin" creates two "raw" resources:

               - third_party_licenses which is the binary concatenation of all the licenses text for
                 all the libraries. License texts can either be an URL to a license file or just the
                 raw text of the license.

               - third_party_licenses_metadata which contains one dependency per line formatted in
                 the following way: "[start_offset]:[length] [name]"

                 [start_offset]     : first byte in third_party_licenses that contains the license
                                      text for this library.
                 [length]           : length of the license text for this library in
                                      third_party_licenses.
                 [name]             : either the name of the library, or its artifact name.

            See https://github.com/google/play-services-plugins/tree/master/oss-licenses-plugin
        */
        val licensesData = resources
            .openRawResource(R.raw.third_party_licenses)
            .readBytes()
        val licensesMetadataReader = resources
            .openRawResource(R.raw.third_party_license_metadata)
            .bufferedReader()

        return licensesMetadataReader.use { reader -> reader.readLines() }.map { line ->
            val (section, name) = line.split(" ", limit = 2)
            val (startOffset, length) = section.split(":", limit = 2).map(String::toInt)
            val licenseData = licensesData.sliceArray(startOffset until startOffset + length)
            val licenseText = licenseData.toString(Charset.forName("UTF-8"))
            LibraryItem(name, licenseText)
        }.sortedBy { item -> item.name.lowercase(Locale.ROOT) }
    }
}

class LibraryItem(val name: String, val license: String) {
    override fun toString() = name
}
