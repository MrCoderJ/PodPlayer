package com.example.podplayer.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import java.util.*

@Entity(
    foreignKeys = [ForeignKey(
        entity = PodCast::class, parentColumns = ["id"],
        childColumns = ["podcastId"], onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("podcastId")]
)
data class Episode(
    @PrimaryKey var guid: String = "",
    var podcastId: Long? = null,
    var title: String = "",
    var description: String = "",
    var mediaUrl: String = "",
    var mimeType: String = "",
    var releaseDate: Date = Date(),
    var duration: String = ""
)
