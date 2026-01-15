package candybar.lib.items

import androidx.annotation.DrawableRes

class Setting(
    @DrawableRes val icon: Int,
    val title: String?,
    val subtitle: String?,
    val content: String?,
    var footer: String?,
    val type: Type
) {
    enum class Type {
        HEADER,
        CACHE,
        ICON_REQUEST,
        RESTORE,
        PREMIUM_REQUEST,
        THEME,
        MATERIAL_YOU,
        NOTIFICATIONS,
        LANGUAGE,
        REPORT_BUGS,
        CHANGELOG,
        RESET_TUTORIAL
    }
}
