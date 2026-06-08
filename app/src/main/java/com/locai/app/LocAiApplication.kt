package com.locai.app

import android.app.Application

class LocAiApplication : Application() {

    val container: LocAiContainer by lazy { LocAiContainer(applicationContext) }
}
