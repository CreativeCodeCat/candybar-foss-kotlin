package candybar.lib.helpers

import candybar.lib.items.IconShape
import sarsamurmu.adaptiveicon.AdaptiveIcon

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

object IconShapeHelper {

    @JvmStatic
    fun getShapes(): List<IconShape> {
        return listOf(
            IconShape("System default", -1),
            IconShape("Circle", AdaptiveIcon.PATH_CIRCLE),
            IconShape("Square", AdaptiveIcon.PATH_SQUARE),
            IconShape("Rounded Square", AdaptiveIcon.PATH_ROUNDED_SQUARE),
            IconShape("Squircle", AdaptiveIcon.PATH_SQUIRCLE),
            IconShape("Teardrop", AdaptiveIcon.PATH_TEARDROP)
        )
    }
}
