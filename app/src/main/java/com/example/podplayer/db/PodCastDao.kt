package com.example.podplayer.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.podplayer.model.Episode
import com.example.podplayer.model.PodCast

@Dao
interface PodCastDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPodcast(podCast: PodCast): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEpisode(episode: Episode): Long

    @Query("SELECT * FROM PodCast ORDER BY FeedTitle")
    fun loadPodcast(): LiveData<List<PodCast>>

    @Query("SELECT * FROM Episode WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
    fun loadEpisodes(podcastId: Long): List<Episode>

    @Query("SELECT * FROM PodCast WHERE feedUrl = :url")
    fun loadPodcasts(url: String): PodCast?

    @Delete
    fun deletPodcast(podcast: PodCast)

    @Query("SELECT * FROM PodCast ORDER BY feedTitle")
    fun loadPodcastStatic(): List<PodCast>

}