package candybar.lib.helpers

import android.content.Intent

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

object IntentHelper {

    const val ACTION_DEFAULT = 0
    const val ICON_PICKER = 1
    const val IMAGE_PICKER = 2
    const val WALLPAPER_PICKER = 3

    @JvmField
    var sAction = ACTION_DEFAULT

    private const val ACTION_ADW_PICK_ICON = "org.adw.launcher.icons.ACTION_PICK_ICON"
    private const val ACTION_TURBO_PICK_ICON = "com.phonemetra.turbo.launcher.icons.ACTION_PICK_ICON"
    private const val ACTION_LAWNCHAIR_ICONPACK = "ch.deletescape.lawnchair.ICONPACK"
    private const val ACTION_NOVA_LAUNCHER = "com.novalauncher.THEME"
    private const val ACTION_ONEPLUS_PICK_ICON = "net.oneplus.launcher.icons.ACTION_PICK_ICON"
    private const val ACTION_PLUS_HOME = "jp.co.a_tm.android.launcher.icons.ACTION_PICK_ICON"
    private const val ACTION_PROJECTIVY_PICK_ICON = "com.spocky.projengmenu.icons.ACTION_PICK_ICON"

    @JvmStatic
    fun getAction(intent: Intent?): Int {
        if (intent == null) return ACTION_DEFAULT
        val action = intent.action
        return if (action != null) {
            when (action) {
                ACTION_ADW_PICK_ICON,
                ACTION_TURBO_PICK_ICON,
                ACTION_LAWNCHAIR_ICONPACK,
                ACTION_NOVA_LAUNCHER,
                ACTION_ONEPLUS_PICK_ICON,
                ACTION_PLUS_HOME,
                ACTION_PROJECTIVY_PICK_ICON -> ICON_PICKER

                Intent.ACTION_PICK,
                Intent.ACTION_GET_CONTENT -> IMAGE_PICKER

                Intent.ACTION_SET_WALLPAPER -> WALLPAPER_PICKER
                else -> ACTION_DEFAULT
            }
        } else {
            ACTION_DEFAULT
        }
    }
}
