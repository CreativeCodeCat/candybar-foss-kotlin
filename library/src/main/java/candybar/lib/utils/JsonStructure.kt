package candybar.lib.utils

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

class JsonStructure private constructor(builder: Builder) {

    val arrayName: String? = builder.mArrayName
    val name: String? = builder.mName
    val author: String = builder.mAuthor
    val url: String = builder.mUrl
    val thumbUrl: String? = builder.mThumbUrl

    class Builder(val mArrayName: String?) {
        var mName: String? = "name"
            private set
        var mAuthor: String = "author"
            private set
        var mUrl: String = "url"
            private set
        var mThumbUrl: String? = "thumbUrl"
            private set

        fun name(name: String?): Builder {
            mName = name
            return this
        }

        fun author(author: String): Builder {
            mAuthor = author
            return this
        }

        fun url(url: String): Builder {
            mUrl = url
            return this
        }

        fun thumbUrl(thumbUrl: String?): Builder {
            mThumbUrl = thumbUrl
            return this
        }

        fun build(): JsonStructure {
            return JsonStructure(this)
        }
    }
}
