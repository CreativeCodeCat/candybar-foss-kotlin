package candybar.lib.utils

import android.content.Context
import android.widget.Toast

/*
 * CandyBar - Material Dashboard
 *
 * Copyright (c) 2014-2016 Dani Mahardhika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

abstract class Extras {

    enum class Tag(val value: String) {
        HOME("home"),
        APPLY("apply"),
        ICONS("icons"),
        REQUEST("request"),
        WALLPAPERS("wallpapers"),
        PRESETS("presets"),
        SETTINGS("settings"),
        FAQS("faqs"),
        ABOUT("about");

        val idx: Int = ordinal
    }

    enum class Error {
        APPFILTER_NULL,
        DATABASE_ERROR,
        INSTALLED_APPS_NULL,
        ICON_REQUEST_NULL,
        ICON_REQUEST_PROPERTY_NULL,
        ICON_REQUEST_PROPERTY_COMPONENT_NULL;

        val message: String
            get() = when (this) {
                APPFILTER_NULL -> "Error: Unable to read appfilter.xml"
                DATABASE_ERROR -> "Error: Unable to read database"
                INSTALLED_APPS_NULL -> "Error: Unable to collect installed apps"
                ICON_REQUEST_NULL -> "Error: Icon request is null"
                ICON_REQUEST_PROPERTY_NULL -> "Error: Icon request property is null"
                ICON_REQUEST_PROPERTY_COMPONENT_NULL -> "Error: Email client component is null"
                else -> "Error: Unknown"
            }

        fun showToast(context: Context?) {
            if (context == null) return
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_POSITION = "position"
        const val EXTRA_SIZE = "size"
        const val EXTRA_URL = "url"
        const val EXTRA_IMAGE = "image"
        const val EXTRA_RESUMED = "resumed"
    }
}
