package com.candybar.dev.activities

import candybar.lib.activities.CandyBarMainActivity

class MainActivity : CandyBarMainActivity() {

    override fun onInit(): ActivityConfiguration {
        return ActivityConfiguration()
    }
}
