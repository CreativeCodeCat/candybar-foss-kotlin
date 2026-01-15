package candybar.lib.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.DrawableHelper as CBDrawableHelper

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

object ConfigurationHelper {

    @JvmStatic
    fun getNavigationIcon(context: Context, navigationIcon: CandyBarApplication.NavigationIcon): Drawable? {
        return when (navigationIcon) {
            CandyBarApplication.NavigationIcon.DEFAULT -> DrawerArrowDrawable(context)
            CandyBarApplication.NavigationIcon.STYLE_1 -> CBDrawableHelper.get(context, R.drawable.ic_toolbar_navigation)
            CandyBarApplication.NavigationIcon.STYLE_2 -> CBDrawableHelper.get(context, R.drawable.ic_toolbar_navigation_2)
            CandyBarApplication.NavigationIcon.STYLE_3 -> CBDrawableHelper.get(context, R.drawable.ic_toolbar_navigation_3)
            CandyBarApplication.NavigationIcon.STYLE_4 -> CBDrawableHelper.get(context, R.drawable.ic_toolbar_navigation_4)
            else -> CBDrawableHelper.get(context, R.drawable.ic_toolbar_navigation)
        }
    }

    @JvmStatic
    fun getSocialIconColor(context: Context, iconColor: CandyBarApplication.IconColor): Int {
        return if (iconColor == CandyBarApplication.IconColor.ACCENT) {
            ColorHelper.getAttributeColor(context, com.google.android.material.R.attr.colorSecondary)
        } else {
            ColorHelper.getAttributeColor(context, android.R.attr.textColorPrimary)
        }
    }
}
