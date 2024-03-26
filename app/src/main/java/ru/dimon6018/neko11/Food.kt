/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package ru.dimon6018.neko11

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Icon

class Food(val type: Int) {
    fun getIcon(context: Context): Icon {
        if (sIcons == null) {
            val icons: TypedArray = context.resources.obtainTypedArray(R.array.food_icons)
            sIcons = IntArray(icons.length())
            for (i in sIcons!!.indices) {
                sIcons!![i] = icons.getResourceId(i, 0)
            }
            icons.recycle()
        }
        return Icon.createWithResource(context, sIcons!![type])
    }

    fun getName(context: Context): String {
        if (sNames == null) {
            sNames = context.resources.getStringArray(R.array.food_names)
        }
        return sNames!![type]
    }

    fun getInterval(context: Context): Long {
        return context.resources.getIntArray(R.array.food_intervals)[type].toLong()
    }

    companion object {
        private var sIcons: IntArray? = null
        private var sNames: Array<String>? = null
    }
}
