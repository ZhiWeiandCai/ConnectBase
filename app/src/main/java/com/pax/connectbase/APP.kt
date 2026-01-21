package com.pax.connectbase

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Created by caizhiwei on 2023/11/1
 */
class APP : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        @JvmStatic
        fun runOnMain(runnable: Runnable) {
            MainScope().launch {
                runnable.run()
            }
        }

        @JvmStatic
        fun runInBackground(runnable: Runnable) {
            CoroutineScope(Dispatchers.IO).launch {
                runnable.run()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}