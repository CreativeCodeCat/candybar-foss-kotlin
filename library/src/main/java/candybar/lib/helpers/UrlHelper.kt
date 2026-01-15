package candybar.lib.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Patterns
import android.webkit.URLUtil
import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import com.danimahardhika.android.helpers.core.DrawableHelper.getTintedDrawable

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

object UrlHelper {

    @JvmStatic
    @Nullable
    fun getSocialIcon(@NonNull context: Context, @NonNull type: Type): Drawable? {
        val color = ConfigurationHelper.getSocialIconColor(
            context,
            CandyBarApplication.getConfiguration().socialIconColor
        )

        @DrawableRes
        val drawableRes = when (type) {
            Type.EMAIL -> R.drawable.ic_toolbar_email
            Type.BEHANCE -> R.drawable.ic_toolbar_behance
            Type.BLUESKY -> R.drawable.ic_toolbar_bluesky
            Type.DRIBBBLE -> R.drawable.ic_toolbar_dribbble
            Type.DISCORD -> R.drawable.ic_toolbar_discord
            Type.FACEBOOK -> R.drawable.ic_toolbar_facebook
            Type.GITHUB -> R.drawable.ic_toolbar_github
            Type.GITLAB -> R.drawable.ic_toolbar_gitlab
            Type.GOOGLEPLAY -> R.drawable.ic_toolbar_googleplay
            Type.INSTAGRAM -> R.drawable.ic_toolbar_instagram
            Type.KOFI -> R.drawable.ic_toolbar_kofi
            Type.MASTODON -> R.drawable.ic_toolbar_mastodon
            Type.MATRIX -> R.drawable.ic_toolbar_matrix
            Type.PINTEREST -> R.drawable.ic_toolbar_pinterest
            Type.THREADS -> R.drawable.ic_toolbar_threads
            Type.TWITTER -> R.drawable.ic_toolbar_x
            Type.TELEGRAM -> R.drawable.ic_toolbar_telegram
            Type.TIKTOK -> R.drawable.ic_toolbar_tiktok
            else -> R.drawable.ic_toolbar_website
        }
        return getTintedDrawable(context, drawableRes, color)
    }

    @JvmStatic
    fun getType(url: String?): Type {
        if (url == null) return Type.INVALID
        if (!URLUtil.isValidUrl(url)) {
            if (Patterns.EMAIL_ADDRESS.matcher(url).matches()) {
                return Type.EMAIL
            }
            return Type.INVALID
        }

        return when {
            url.contains("behance.") -> Type.BEHANCE
            url.contains("bsky.") -> Type.BLUESKY
            url.contains("dribbble.") -> Type.DRIBBBLE
            url.contains("discord.") -> Type.DISCORD
            url.contains("facebook.") -> Type.FACEBOOK
            url.contains("github.") -> Type.GITHUB
            url.contains("gitlab.") -> Type.GITLAB
            url.contains("play.google.") -> Type.GOOGLEPLAY
            url.contains("instagram.") -> Type.INSTAGRAM
            url.contains("ko-fi.") -> Type.KOFI
            url.contains("mastodon.") || url.contains("mstdn.") || url.contains("mas.") || url.contains("todon.") || url.contains("fosstodon.") || url.contains("troet.") || url.contains("chaos.") || url.contains("floss.") -> Type.MASTODON
            url.contains("matrix.") -> Type.MATRIX
            url.contains("pinterest.") -> Type.PINTEREST
            url.contains("twitter.") || url.contains("https://x.com/") -> Type.TWITTER
            url.contains("threads.") -> Type.THREADS
            url.contains("t.me/") || url.contains("telegram.me/") -> Type.TELEGRAM
            url.contains("tiktok.") -> Type.TIKTOK
            else -> Type.UNKNOWN
        }
    }

    enum class Type {
        EMAIL,
        BEHANCE,
        BLUESKY,
        DRIBBBLE,
        DISCORD,
        FACEBOOK,
        GITHUB,
        GITLAB,
        GOOGLEPLAY,
        INSTAGRAM,
        KOFI,
        MASTODON,
        MATRIX,
        PINTEREST,
        THREADS,
        TWITTER,
        TELEGRAM,
        TIKTOK,
        UNKNOWN,
        INVALID
    }
}
