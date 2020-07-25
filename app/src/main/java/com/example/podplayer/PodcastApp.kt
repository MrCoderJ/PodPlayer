package com.example.podplayer

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PodcastApp: Application() {

    companion object{
        fun getViewModelFactory(app: Application): ViewModelProvider.Factory{
            return object: ViewModelProvider.Factory{
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return modelClass.getConstructor(Application::class.java).newInstance(app)
                }

            }
        }
    }
}