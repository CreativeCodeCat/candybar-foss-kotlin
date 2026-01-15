package candybar.lib.items

class Wallpaper private constructor(
    @get:JvmName("getURL")
    val url: String,
    val thumbUrl: String,
    val author: String
) {
    var name: String? = null
    var color: Int = 0
    var size: Int = 0
    var mimeType: String? = null
    var dimensions: ImageSize? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Wallpaper) return false
        return author == other.author &&
                url == other.url &&
                thumbUrl == other.thumbUrl
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + thumbUrl.hashCode()
        result = 31 * result + author.hashCode()
        return result
    }

    class Builder {
        private var name: String? = null
        private var author: String = ""
        private var thumbUrl: String = ""
        private var url: String = ""
        private var color: Int = 0
        private var size: Int = 0
        private var mimeType: String? = null
        private var dimensions: ImageSize? = null

        fun name(name: String?) = apply { this.name = name }
        fun author(author: String) = apply { this.author = author }
        fun url(url: String) = apply { this.url = url }
        fun thumbUrl(thumbUrl: String) = apply { this.thumbUrl = thumbUrl }
        fun dimensions(dimensions: ImageSize?) = apply { this.dimensions = dimensions }
        fun mimeType(mimeType: String?) = apply { this.mimeType = mimeType }
        fun color(color: Int) = apply { this.color = color }
        fun size(size: Int) = apply { this.size = size }

        fun build(): Wallpaper {
            return Wallpaper(url, thumbUrl, author).apply {
                name = this@Builder.name
                dimensions = this@Builder.dimensions
                mimeType = this@Builder.mimeType
                color = this@Builder.color
                size = this@Builder.size
            }
        }
    }

    companion object {
        @JvmStatic
        fun Builder(): Builder = Builder()
    }
}
