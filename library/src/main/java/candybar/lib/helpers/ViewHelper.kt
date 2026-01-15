package candybar.lib.helpers

import android.app.Activity
import android.graphics.Point
import androidx.annotation.Nullable
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.items.Home
import com.danimahardhika.android.helpers.core.ColorHelper
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller
import java.util.Locale

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

object ViewHelper {

    @JvmStatic
    fun setFastScrollColor(@Nullable fastScroll: RecyclerFastScroller?) {
        if (fastScroll == null) return

        val accent = ColorHelper.getAttributeColor(
            fastScroll.context,
            com.google.android.material.R.attr.colorSecondary
        )
        fastScroll.setBarColor(ColorHelper.setColorAlpha(accent, 0.8f))
        fastScroll.setHandleNormalColor(accent)
        fastScroll.setHandlePressedColor(ColorHelper.getDarkerColor(accent, 0.7f))
    }

    @JvmStatic
    fun getWallpaperViewRatio(viewStyle: String): Point {
        // There is a case "square"
        return when (viewStyle.lowercase(Locale.getDefault())) {
            "landscape" -> Point(16, 9)
            "portrait" -> Point(4, 5)
            else -> Point(1, 1)
        }
    }

    @JvmStatic
    fun getHomeImageViewStyle(viewStyle: String): Home.Style {
        // There is a case "card_landscape"
        return when (viewStyle.lowercase(Locale.getDefault())) {
            "square" -> Home.Style(Point(1, 1), Home.Style.Type.SQUARE)
            "landscape" -> Home.Style(Point(16, 9), Home.Style.Type.LANDSCAPE)
            "card_square" -> Home.Style(Point(1, 1), Home.Style.Type.CARD_SQUARE)
            else -> Home.Style(Point(16, 9), Home.Style.Type.CARD_LANDSCAPE)
        }
    }

    @JvmStatic
    fun addBottomPadding(activity: Activity?, v: RecyclerView) {
        v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, 100)
    }
}
