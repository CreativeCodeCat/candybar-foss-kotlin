package candybar.lib.utils.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import candybar.lib.R

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

class HeaderView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    private var mWidthRatio: Int = 16
    private var mHeightRatio: Int = 9

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.HeaderView)
        try {
            mWidthRatio = typedArray.getInteger(R.styleable.HeaderView_widthRatio, 16)
            mHeightRatio = typedArray.getInteger(R.styleable.HeaderView_heightRatio, 9)
        } finally {
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val height = (widthMeasureSpec.toDouble() / mWidthRatio) * mHeightRatio
        setMeasuredDimension(widthMeasureSpec, height.toInt())
    }

    fun setRatio(widthRatio: Int, heightRatio: Int) {
        mWidthRatio = widthRatio
        mHeightRatio = heightRatio
        val height = (measuredWidth.toDouble() / mWidthRatio) * mHeightRatio
        setMeasuredDimension(measuredWidth, height.toInt())
    }
}
