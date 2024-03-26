/*
 * Copyright (C) 2020 The Android Open Source Project
 * Copyright (C) 2023 Dmitry Frolkov <dimon6018t@gmail.com>
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
package ru.dimon6018.neko11

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ru.dimon6018.neko11.Cat.Companion.PURR
import ru.dimon6018.neko11.Cat.Companion.create
import ru.dimon6018.neko11.NekoLand.Companion.CHAN_ID
import java.util.Random
import java.util.concurrent.TimeUnit


class NekoService(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        var state: Result
        val context = applicationContext
        try {
            triggerFoodResponse(context)
            state = Result.success()
        } catch (e: Exception) {
            state = Result.failure()
        }
        stopFoodWork(context)
        return state
    }
    companion object {
        private var CAT_NOTIFICATION: Int = 1
        private var CAT_CAPTURE_PROB: Float = 1.1f // generous

        private var SECONDS: Long = 1000
        private var MINUTES: Long = 60 * SECONDS

        private var INTERVAL_JITTER_FRAC: Float = 0.251f

        var title_message: String? = null

        fun setupNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val noman = context.getSystemService(
                    NotificationManager::class.java
                )
                val eggChan = NotificationChannel(
                    CHAN_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                eggChan.setSound(Uri.EMPTY, Notification.AUDIO_ATTRIBUTES_DEFAULT)
                eggChan.vibrationPattern = PURR
                eggChan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                noman.createNotificationChannel(eggChan)
            }
        }

        private fun triggerFoodResponse(context: Context) {
            val prefs = PrefState(context)
            val food = prefs.foodState
            if (food != 0) {
                prefs.foodState = 0 // nom
                val rng = Random()
                if (rng.nextFloat() <= CAT_CAPTURE_PROB) {
                    val cat: Cat?
                    val cats = prefs.cats
                    val probs: IntArray = context.resources.getIntArray(R.array.food_new_cat_prob)
                    val new_cat_prob =
                        (if ((food < probs.size)) probs[food] else 50).toFloat() / 100f
                    if (cats.isEmpty() || rng.nextFloat() <= new_cat_prob) {
                        title_message = context.getString(R.string.notification_title)
                        cat = newRandomCat(context, prefs)
                    } else {
                        title_message = context.getString(R.string.notification_title_return)
                        cat = getExistingCat(prefs)
                    }
                    if(cat == null) {
                        return
                    }
                    notifyCat(context, cat, title_message)
                }
            }
        }

        private fun notifyCat(context: Context, cat: Cat, message: String?) {
            title_message = message
            val noman = context.getSystemService(
                NotificationManager::class.java
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val builder: Notification.Builder = cat.buildNotificationP(context)
                noman.notify(cat.shortcutId, CAT_NOTIFICATION, builder.build())
            } else {
                val builder: NotificationCompat.Builder = cat.buildNotificationO(context)
                noman.notify(cat.shortcutId, CAT_NOTIFICATION, builder.build())
            }
        }

        private fun newRandomCat(context: Context?, prefs: PrefState): Cat {
            val cat = create(context!!)
            prefs.addCat(cat)
            return cat
        }
        private fun getExistingCat(prefs: PrefState): Cat? {
            val cats = prefs.cats
            if (cats.isEmpty()) return null
            return cats[Random().nextInt(cats.size)]
        }
        fun scheduleFoodWork(context: Context?, intervalMinutes: Int) {
            var interval = intervalMinutes * MINUTES
            val jitter = (INTERVAL_JITTER_FRAC * interval).toLong()
            interval += (Math.random() * (2 * jitter)).toLong() - jitter
            val time = interval / MINUTES
            val workFoodRequest: OneTimeWorkRequest =
                OneTimeWorkRequest.Builder(NekoService::class.java)
                    .addTag("FOODWORK")
                    .setInitialDelay(time, TimeUnit.SECONDS)
                    .build()
            WorkManager.getInstance(context!!).enqueue(workFoodRequest)
        }

        fun stopFoodWork(context: Context?) {
            WorkManager.getInstance(context!!).cancelAllWorkByTag("FOODWORK")
        }
    }
}