package com.example.podplayer.api

import com.example.podplayer.model.PodCastResponse
import com.example.podplayer.model.RssFeedResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RetrofitInterface {

    @GET("/search?media=podcast")
    fun searchPodcastByTerm(@Query("term") term: String): Call<PodCastResponse>

    fun getFeed(xmlFileURL: String, callBack: (RssFeedResponse?) -> Unit)
}