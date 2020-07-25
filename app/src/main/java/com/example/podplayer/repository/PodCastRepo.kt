package com.example.podplayer.repository

import android.app.Application
import com.example.podplayer.api.RetrofitClient
import com.example.podplayer.api.RssFeedService
//import com.example.podplayer.model.PodCast
import com.example.podplayer.model.PodCastData
import com.example.podplayer.model.PodCastResponse
import com.example.podplayer.model.RssFeedResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PodCastRepo(app: Application) {

    private val webService = RetrofitClient.getClient(app.applicationContext)
    val rssFeedService = RssFeedService()
    fun searchByTerm(term: String, callback: (List<PodCastData>?) -> Unit){
        val podCastCall = webService.searchPodcastByTerm(term)
        podCastCall.enqueue(object: Callback<PodCastResponse>{
            override fun onFailure(call: Call<PodCastResponse>, t: Throwable) {
                callback(null)
            }
            override fun onResponse(
                call: Call<PodCastResponse>,
                response: Response<PodCastResponse>
            ) {
              val body = response.body()
                callback(body?.results)
            }

        })
    }




}