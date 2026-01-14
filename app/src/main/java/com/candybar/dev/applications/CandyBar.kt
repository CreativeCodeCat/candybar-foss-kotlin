package com.candybar.dev.applications

import candybar.lib.applications.CandyBarApplication
import com.candybar.dev.R

class CandyBar : CandyBarApplication() {

    override fun getDrawableClass(): Class<*> {
        return R.drawable::class.java
    }

    override fun onInit(): Configuration {
        val configuration = Configuration()

        configuration.setGenerateAppFilter(true)
        configuration.setGenerateAppMap(true)
        configuration.setGenerateThemeResources(true)
        configuration.setNavigationIcon(NavigationIcon.STYLE_4)
        configuration.setOtherApps(
            arrayOf(
                OtherApp(
                    "icon_1",
                    "App 1",
                    "Another app #1",
                    "https://play.google.com/store/apps/details?id=app.1"
                ),
                OtherApp(
                    "icon_2",
                    "App 2",
                    "Another app #2",
                    "https://play.google.com/store/apps/details?id=app.2"
                )
            )
        )

        configuration.setShowTabAllIcons(true)
        configuration.setExcludedCategoryForSearch(
            arrayOf(
                "All Apps", "Cat 1", "Cat 2", "Cat 3", "Cat 4", "Cat 5", "Cat 6", "Cat 7", "Cat 8", "Cat 9", "Cat 11"
            )
        )

        return configuration
    }
}
