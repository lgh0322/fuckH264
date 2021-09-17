package com.vaca.fuckh264

import android.app.Application


class MainApplication : Application() {

    companion object {
        lateinit var application: Application
    }


    override fun onCreate() {
        super.onCreate()
        PathUtil.initVar(this)
        application = this

    }


}