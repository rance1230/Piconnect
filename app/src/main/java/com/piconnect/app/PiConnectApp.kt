package com.piconnect.app

import android.app.Application

class PiConnectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: PiConnectApp
            private set
    }
}
