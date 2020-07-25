package com.example.podplayer.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.podplayer.model.Episode
import com.example.podplayer.model.PodCast
import com.example.podplayer.model.PodCastSummaryViewData
import com.example.podplayer.repository.PodcastRepo
import com.example.podplayer.util.DateUtil
import java.util.*

class PodcastViewModel(app: Application) : AndroidViewModel(app) {
    var podcastRepo: PodcastRepo? = null
    var activePodcastViewData: PodcastViewData? = null
    var activeEpisodeViewData: EpisodeViewData? = null
    private var activePodcast: PodCast? = null
    private var livePodcastData: LiveData<List<PodCastSummaryViewData>>? = null


    data class PodcastViewData(
        var subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episodes: List<EpisodeViewData>
    )

    data class EpisodeViewData(
        var guid: String? = "",
        var title: String? = "",
        var description: String? = "",
        var mediaUrl: String? = "",
        var releaseDate: Date? = null,
        var duration: String? = "",
        var isVideo: Boolean = false
    )

    private fun episodeToEpisodeView(episodes: List<Episode>): List<EpisodeViewData> {
        return episodes.map {
            val isVideo = it.mimeType.startsWith("video")
            EpisodeViewData(
                it.guid,
                it.title,
                it.description,
                it.mediaUrl,
                it.releaseDate,
                it.duration,
                isVideo
            )
        }
    }

    private fun podcastToPodcastView(podCast: PodCast): PodcastViewData {
        return PodcastViewData(
            podCast.id != null,
            podCast.feedTitle,
            podCast.feedUrl,
            podCast.feedDesc,
            podCast.imageUrl,
            episodeToEpisodeView(podCast.episode)
        )
    }

    //    fun getPodcast(podcastSummaryViewData: PodCastSummaryViewData, callback: (PodcastViewData?) -> Unit) {
//
//        val repo = podcastRepo ?: return
//        val feedUrl = podcastSummaryViewData.feedUrl ?: return
//        repo.getPodcast(feedUrl) {
//            it.let {
//                if (it != null) {
//                    it.feedTitle = podcastSummaryViewData.name ?: ""
//                }
//                if (it != null) {
//                    it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
//                }
//                activePodcastViewData = it?.let { it1 -> podcastToPodcastView(it1) }
//                callback(activePodcastViewData)
//            }
//        }
//    }
    fun getPodcast(podcastSummaryViewData: PodCastSummaryViewData, callback: (PodcastViewData?) -> Unit) {
        val repo = podcastRepo ?: return
        val feedUrl = podcastSummaryViewData.feedUrl ?: return
        repo.getPodcast(feedUrl) {
            it?.let {
                it.feedTitle = podcastSummaryViewData.name ?: ""
                it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                activePodcastViewData = podcastToPodcastView(it)
                activePodcast = it
                callback(activePodcastViewData)
            }
        }
    }

    fun saveActivePodcast(){
        val repo = podcastRepo ?: return
        activePodcast?.let {
            repo.save(it)
        }
    }
    private fun podcastToSummaryView(podCast: PodCast): PodCastSummaryViewData{
        return PodCastSummaryViewData(podCast.feedTitle, DateUtil.dateToShortDate(podCast.lastUpdated), podCast.imageUrl, podCast.feedUrl)
    }

    fun getPodcasts():LiveData<List<PodCastSummaryViewData>>?{
        val repo = podcastRepo ?: return null
        if(livePodcastData == null){
            val liveData = repo.getAll()
            livePodcastData = Transformations.map(liveData){
                it.map { podCast -> podcastToSummaryView(podCast)  }
            }
        }
        return livePodcastData
    }

    fun deleteActivePodcast(){
        val repo = podcastRepo ?: return
        activePodcast?.let {
            repo.delete(it)
        }
    }

    fun setActivePodcast(feedUrl: String, callback: (PodCastSummaryViewData?) -> Unit){
        val repo = podcastRepo ?: return

        repo.getPodcast(feedUrl){podCast ->
            if (podCast == null){
                callback(null)
            }else{
                activePodcastViewData = podcastToPodcastView(podCast)
                activePodcast = podCast
                callback(podcastToSummaryView(podCast))
            }
        }
    }
}