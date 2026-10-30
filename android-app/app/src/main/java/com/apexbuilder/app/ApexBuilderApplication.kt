package com.apexbuilder.app

import android.app.Application
import com.apexbuilder.app.data.AppContainer

class ApexBuilderApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}