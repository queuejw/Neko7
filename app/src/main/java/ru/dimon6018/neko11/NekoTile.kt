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

import android.annotation.SuppressLint
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import ru.dimon6018.neko11.NekoService.Companion.stopFoodWork
import ru.dimon6018.neko11.PrefState.PrefsListener

class NekoTile : TileService(), PrefsListener {
    private var mPrefs: PrefState? = null

    override fun onCreate() {
        super.onCreate()
        mPrefs = PrefState(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        mPrefs!!.setListener(this)
        updateState()
    }

    override fun onStopListening() {
        super.onStopListening()
        mPrefs!!.setListener(null)
    }
    override fun onPrefsChanged() {
        updateState()
    }

    private fun updateState() {
        val tile = qsTile
        val foodState = mPrefs!!.foodState
        val food = Food(foodState)
        if (foodState != 0) {
        }
        tile.icon = food.getIcon(this)
        tile.label = food.getName(this)
        tile.state =
            if (foodState != 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        if (mPrefs!!.foodState != 0) {
            // there's already food loaded, let's empty it
            mPrefs!!.foodState = 0
            stopFoodWork(applicationContext)
        } else {
            // time to feed the cats
            if (isLocked) {
                if (isSecure) {
                    Log.d(TAG, "startActivityAndCollapse")
                    val intent = Intent(this, NekoLockedActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivityAndCollapse(intent)
                } else {
                    unlockAndRun { this.showNekoDialog() }
                }
            } else {
                showNekoDialog()
            }
        }
    }

    private fun showNekoDialog() {
        Log.d(TAG, "showNekoDialog")
        showDialog(NekoDialog(this))
    }

    companion object {
        private const val TAG = "NekoTile"
    }
}
