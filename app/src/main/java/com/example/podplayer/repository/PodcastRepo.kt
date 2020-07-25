package com.example.podplayer.repository

import androidx.lifecycle.LiveData
import com.example.podplayer.AppExecutors
import com.example.podplayer.api.FeedService
import com.example.podplayer.db.PodCastDao
import com.example.podplayer.model.Episode
import com.example.podplayer.model.PodCast
import com.example.podplayer.model.RssFeedResponse
import com.example.podplayer.util.DateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


class PodcastRepo(private val feedService : FeedService, private val podCastDao: PodCastDao) : CoroutineScope {
    private val appExecutors = AppExecutors()

    fun getPodcast(feedUrl: String, callback: (PodCast?) -> Unit){
        appExecutors.diskIO().execute {
            val podcast = podCastDao.loadPodcasts(feedUrl)
            if (podcast != null) {
                podcast.id?.let {
                    podcast.episode = podCastDao.loadEpisodes(it)
                    launch {
                        callback(podcast)
                    }
                }
            }else{
                appExecutors.networkIO().execute {
                    feedService.getFeed(feedUrl) { rssFeedResponse ->
                        var  podcast: PodCast? = null
                        if (rssFeedResponse != null){
                            podcast = rssResponseToPodcast(feedUrl, "", rssFeedResponse)
                        }
                        launch {
                            callback(podcast)
                        }
                    }
                }
            }
            }
    }

    private fun rssItemsToEpisodes(episodeResponse: List<RssFeedResponse.EpisodeResponse>): List<Episode>{
        return episodeResponse.map {
            Episode(
                it.guid ?: "",
                null,
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtil.xmlDateToDate(it.pubDate),
                it.duration ?: ""
            )
        }
    }

    private fun rssResponseToPodcast(feedUrl: String, imageUrl: String, rssFeedResponse: RssFeedResponse): PodCast?{
        val items = rssFeedResponse.episodes ?: return null
        val description =   if (rssFeedResponse.description == "") rssFeedResponse.summary else rssFeedResponse.description

        return PodCast(null, feedUrl, rssFeedResponse.title, description, imageUrl, rssFeedResponse.lastUpdated,episode = rssItemsToEpisodes(items) )
    }

    override val coroutineContext: CoroutineContext
        get() = Job() + Dispatchers.Main

    // add podcast to the database and display as subscribed
    fun save(podcast: PodCast){
        appExecutors.diskIO().execute {
            val podcastId = podCastDao.insertPodcast(podcast)
            for (episode in podcast.episode){
                episode.podcastId = podcastId
                podCastDao.insertEpisode(episode)
            }
        }
    }

    // Get all the podcast in the database
    fun getAll(): LiveData<List<PodCast>>{
        return podCastDao.loadPodcast()
    }

    // Delete the podcast from the database by unsubscribing the podcast
    fun delete(podcast:PodCast){
        appExecutors.diskIO().execute {
            podCastDao.deletPodcast(podcast)
        }
    }

    private fun getNewEpisodes(localPodcast: PodCast, callback: (List<Episode>) -> Unit){
        appExecutors.networkIO().execute {
            feedService.getFeed(localPodcast.feedUrl){rssFeedResponse ->
                if (rssFeedResponse != null){
                    val remotePodcast = rssResponseToPodcast(localPodcast.feedUrl, localPodcast.imageUrl, rssFeedResponse)
                    remotePodcast?.let {
                        val localEpisodes = podCastDao.loadEpisodes(localPodcast.id!!)
                        val newEpisodes = remotePodcast.episode.filter { episode ->
                            localEpisodes.find { episode.guid == it.guid } == null
                        }
                        callback(newEpisodes)
                    }
                }else{
                    callback(listOf())
                }
            }
        }
    }
    private fun saveNewEpisodes(podcastId: Long, episodes: List<Episode>){
        appExecutors.diskIO().execute {
            for (episode in episodes){
                episode.podcastId = podcastId
                podCastDao.insertEpisode(episode)
            }
        }
    }

    inner class PodcastUpdateInfo(val feedUrl: String, val name: String, val newCount: Int)

    fun updatePodcastEpisodes(callback: (List<PodcastUpdateInfo>) -> Unit){
        val updatedPodcasts: MutableList<PodcastUpdateInfo> = mutableListOf()

        val podcasts = podCastDao.loadPodcastStatic()

        var processCount = podcasts.count()
        for (podcast in podcasts){
            getNewEpisodes(podcast){episodes ->
                if (episodes.count() > 0){
                    saveNewEpisodes(podcast.id!!, episodes)
                    updatedPodcasts.add(PodcastUpdateInfo(podcast.feedUrl, podcast.feedTitle, episodes.count()))
                }
                processCount--
                if (processCount == 0){
                    callback(updatedPodcasts)
                }
            }
        }
    }
}

