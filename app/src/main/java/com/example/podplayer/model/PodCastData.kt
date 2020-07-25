package com.example.podplayer.model

import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Entity
data class PodCastData(
    @SerializedName("collectionCensoredName") val collectionCensoredName: String,
    @SerializedName("feedUrl") val feedUrl: String,
    @SerializedName("artworkUrl100") val artworkUrl100: String,
    @SerializedName("releaseDate") val releaseDate: String
)

data class PodCastResponse(
    val resultCount: Int,
    val results: List<PodCastData>
)