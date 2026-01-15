package candybar.lib.items

import candybar.lib.utils.AlphanumComparator
import java.util.Objects

class Icon {
    var drawableName: String = ""
        private set
    var customName: String = ""
        private set
    var title: String = ""
    var res: Int = 0
        private set
    var packageName: String = ""
    var icons: List<Icon> = emptyList()

    constructor(drawableName: String?, customName: String?, res: Int) {
        this.drawableName = drawableName ?: ""
        this.customName = customName ?: ""
        this.res = res
    }

    constructor(title: String?, res: Int, packageName: String?) {
        this.title = title ?: ""
        this.res = res
        this.packageName = packageName ?: ""
    }

    constructor(title: String?, icons: List<Icon>) {
        this.title = title ?: ""
        this.icons = icons
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Icon) return false
        return res == other.res && title == other.title
    }

    override fun hashCode(): Int = Objects.hash(res, title)

    companion object {
        @JvmField
        val TitleComparator = object : AlphanumComparator() {
            override fun compare(o1: Any?, o2: Any?): Int {
                val s1 = (o1 as Icon).title
                val s2 = (o2 as Icon).title
                return super.compare(s1, s2)
            }
        }
    }
}
