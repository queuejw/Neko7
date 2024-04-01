package ru.dimon6018.neko11

import android.app.Application

class NekoApp: Application() {
    override fun onCreate() {
        val crashHandler = CrashHandler()
        crashHandler.setContext(applicationContext)
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        super.onCreate()
    }
}