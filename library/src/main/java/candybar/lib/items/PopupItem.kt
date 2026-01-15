package candybar.lib.items

import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import candybar.lib.R
import candybar.lib.preferences.Preferences

class PopupItem(val title: String) {
    @DrawableRes
    var icon: Int = 0
    var isShowCheckbox: Boolean = false
    var checkboxValue: Boolean = false
    var isSelected: Boolean = false
    var type: Type? = null

    fun setIcon(@DrawableRes icon: Int): PopupItem {
        this.icon = icon
        return this
    }

    fun setShowCheckbox(showCheckbox: Boolean): PopupItem {
        this.isShowCheckbox = showCheckbox
        return this
    }

    fun setCheckboxValue(checkboxValue: Boolean): PopupItem {
        this.checkboxValue = checkboxValue
        return this
    }

    fun setSelected(selected: Boolean): PopupItem {
        this.isSelected = selected
        return this
    }

    fun setType(type: Type?): PopupItem {
        this.type = type
        return this
    }

    enum class Type {
        WALLPAPER_CROP,
        HOMESCREEN,
        LOCKSCREEN,
        HOMESCREEN_LOCKSCREEN,
        DOWNLOAD
    }

    companion object {
        @JvmStatic
        fun getApplyItems(context: Context): List<PopupItem> {
            val items = mutableListOf<PopupItem>()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                items.add(
                    PopupItem(context.resources.getString(R.string.menu_wallpaper_crop))
                        .setType(Type.WALLPAPER_CROP)
                        .setCheckboxValue(Preferences.get(context).isCropWallpaper)
                        .setShowCheckbox(true)
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                items.add(
                    PopupItem(context.resources.getString(R.string.menu_apply_lockscreen))
                        .setType(Type.LOCKSCREEN)
                        .setIcon(R.drawable.ic_toolbar_lockscreen)
                )
            }

            items.add(
                PopupItem(context.resources.getString(R.string.menu_apply_homescreen))
                    .setType(Type.HOMESCREEN)
                    .setIcon(R.drawable.ic_toolbar_homescreen)
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                items.add(
                    PopupItem(context.resources.getString(R.string.menu_apply_homescreen_lockscreen))
                        .setType(Type.HOMESCREEN_LOCKSCREEN)
                        .setIcon(R.drawable.ic_toolbar_homescreen_lockscreen)
                )
            }

            if (context.resources.getBoolean(R.bool.enable_wallpaper_download)) {
                items.add(
                    PopupItem(context.resources.getString(R.string.menu_save))
                        .setType(Type.DOWNLOAD)
                        .setIcon(R.drawable.ic_toolbar_download)
                )
            }
            return items
        }
    }
}
