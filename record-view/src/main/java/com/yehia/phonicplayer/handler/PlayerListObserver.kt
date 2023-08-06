package com.yehia.phonicplayer.handler

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 * Edit by Yehia Reda on 05/03/2022.
 */

class PlayerListObserver : LifecycleObserver {
    private var actionHandler: MediaActionHandler? = null
    fun registerActionHandler(handler: MediaActionHandler?) {
        actionHandler = handler
    }

    fun registerLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        Log.e("TAG", "====>>>> lifecycle STARTED")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() {
        if (actionHandler != null) {
            actionHandler!!.onAction()
        }
    }

    companion object {
        private var mInstance: PlayerListObserver? = null

        @JvmStatic
        @get:Synchronized
        val instance: PlayerListObserver?
            get() {
                if (mInstance == null) {
                    mInstance = PlayerListObserver()
                }
                return mInstance
            }
    }
}