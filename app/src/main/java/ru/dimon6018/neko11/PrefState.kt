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
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener

class PrefState(private val mContext: Context) : OnSharedPreferenceChangeListener {
    private val mPrefs: SharedPreferences = mContext.getSharedPreferences(FILE_NAME, 0)
    private var mListener: PrefsListener? = null

    // Can also be used for renaming.
    fun addCat(cat: Cat) {
        mPrefs.edit()
            .putString(CAT_KEY_PREFIX + cat.seed, cat.name)
            .apply()
    }

    fun removeCat(cat: Cat) {
        mPrefs.edit().remove(CAT_KEY_PREFIX + cat.seed).apply()
    }

    val cats: List<Cat>
        get() {
            val cats = ArrayList<Cat>()
            val map = mPrefs.all
            for (key in map.keys) {
                if (key.startsWith(CAT_KEY_PREFIX)) {
                    val seed = key.substring(CAT_KEY_PREFIX.length).toLong()
                    val cat = Cat(mContext, seed)
                    cat.name = map[key].toString()
                    cats.add(cat)
                }
            }
            return cats
        }

    var foodState: Int
        get() = mPrefs.getInt(FOOD_STATE, 0)
        set(foodState) {
            mPrefs.edit().putInt(FOOD_STATE, foodState).apply()
        }

    fun setListener(listener: PrefsListener?) {
        mListener = listener
        if (mListener != null) {
            mPrefs.registerOnSharedPreferenceChangeListener(this)
        } else {
            mPrefs.unregisterOnSharedPreferenceChangeListener(this)
        }
    }
    fun isConfigured(): Boolean {
        return mPrefs.getBoolean(CONFIGURED, false)
    }
    fun setConf(boolean: Boolean) {
        mPrefs.edit().putBoolean(CONFIGURED, boolean).apply()
    }
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        mListener?.onPrefsChanged()
    }

    interface PrefsListener {
        fun onPrefsChanged()
    }

    companion object {
        private const val FILE_NAME = "mPrefs"

        private const val FOOD_STATE = "food"

        private const val CAT_KEY_PREFIX = "cat:"

        private const val CONFIGURED = "NEKO7_isConfigured"
    }
}
