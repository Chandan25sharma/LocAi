package com.locai.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocAiApplication : Application() {

    val container: LocAiContainer by lazy { LocAiContainer(applicationContext) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Unload all loaded models when the user fully leaves the app (all activities stopped).
        // This frees the ~1-3 GB of RAM the models consume while LocAi isn't visible.
        // Models reload automatically the next time they're needed.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                appScope.launch {
                    container.modelManager.unload()
                    container.visionModelManager.unload()
                    container.imageGeneratorManager.unload()
                }
            }
        })
    }
}
