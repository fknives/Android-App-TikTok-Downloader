package org.fnives.tiktokdownloader

import android.app.Application
import org.fnives.tiktokdownloader.di.ServiceLocator

class App : Application()  {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.start(this)
    }
}