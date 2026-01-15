package candybar.lib.items

import android.graphics.Point
import androidx.annotation.DrawableRes

class Home(
    @DrawableRes val icon: Int,
    var title: String?,
    val subtitle: String?,
    val type: Type,
    var isLoading: Boolean
) {
    enum class Type {
        APPLY,
        DONATE,
        ICONS,
        DIMENSION
    }

    class Style(val point: Point, val type: Type) {
        enum class Type {
            CARD_SQUARE,
            CARD_LANDSCAPE,
            SQUARE,
            LANDSCAPE
        }
    }
}
