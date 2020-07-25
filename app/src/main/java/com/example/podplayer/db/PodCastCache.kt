package com.example.podplayer.db

import android.app.Application
import android.util.Log
import com.example.podplayer.AppExecutors
import com.example.podplayer.model.PodCast

class PodCastCache{

   //private val podcastDao = PodCastDb.getDb(app)
    private val appExecutors = AppExecutors()

    fun insert(podcast:List<PodCast>, done: () -> Unit){
        appExecutors.diskIO().execute{
            Log.d("Podcast", "Inserting ${podcast.size}")
           // podcastDao.save(podcast)
            done()
        }
    }

//    fun fetchPodcast(): LiveData<List<PodCast>>{
//        return podcastDao.fetchPodcast()
//    }
}