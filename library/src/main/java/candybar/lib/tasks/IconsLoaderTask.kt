package candybar.lib.tasks

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import candybar.lib.R
import candybar.lib.activities.CandyBarMainActivity
import candybar.lib.helpers.IconsHelper
import candybar.lib.items.Home
import candybar.lib.utils.AsyncTaskBase
import candybar.lib.utils.listeners.HomeListener
import com.danimahardhika.android.helpers.core.utils.LogUtil
import java.lang.ref.WeakReference
import java.util.Random

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

class IconsLoaderTask(context: Context) : AsyncTaskBase() {

    private val mContext: WeakReference<Context> = WeakReference(context)
    private var mHome: Home? = null

    @SuppressLint("StringFormatInvalid")
    override fun run(): Boolean {
        if (!isCancelled) {
            try {
                Thread.sleep(1)

                val context = mContext.get() ?: return false
                IconsHelper.loadIcons(context, true)

                if (CandyBarMainActivity.sHomeIcon != null) return true

                val random = Random()
                val index = random.nextInt(CandyBarMainActivity.sSections?.size ?: 0)
                val icons = CandyBarMainActivity.sSections?.get(index)?.icons ?: return false
                val iconIndex = random.nextInt(icons.size)
                val icon = icons[iconIndex]

                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeResource(
                    context.resources,
                    icon.res, options
                )

                var iconDimension = ""

                if (options.outWidth > 0 && options.outHeight > 0) {
                    iconDimension = context.resources.getString(
                        R.string.home_icon_dimension,
                        "${options.outWidth} x ${options.outHeight}"
                    )
                }

                mHome = Home(
                    icon.res,
                    icon.title,
                    iconDimension,
                    Home.Type.DIMENSION,
                    false
                )
                CandyBarMainActivity.sHomeIcon = mHome
                return true
            } catch (e: Exception) {
                LogUtil.e(Log.getStackTraceString(e))
                return false
            }
        }
        return false
    }

    override fun postRun(ok: Boolean) {
        if (ok) {
            val home = mHome ?: return
            val context = mContext.get() ?: return

            val fm = (context as? AppCompatActivity)?.supportFragmentManager ?: return

            val fragment = fm.findFragmentByTag("home") ?: return

            val listener = fragment as? HomeListener
            listener?.onHomeDataUpdated(home)
        }
    }
}
