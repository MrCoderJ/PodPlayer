package com.example.podplayer.viewModel

//import com.example.podplayer.model.PodCast
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.podplayer.model.PodCastData
import com.example.podplayer.model.PodCastSummaryViewData
import com.example.podplayer.repository.PodCastRepo
import com.example.podplayer.util.DateUtil

class SearchViewModel(app: Application): AndroidViewModel(app) {

    var podCastRepo: PodCastRepo? = null

    private fun PodCastToPodcastView(podCast: PodCastData ): PodCastSummaryViewData{
        return PodCastSummaryViewData(
            podCast.collectionCensoredName,
            DateUtil.jsonDateToShortDate(podCast.releaseDate) ,
            podCast.artworkUrl100,
            podCast.feedUrl

        )
    }

    fun searchPodCasts(term: String, callback: (List<PodCastSummaryViewData>) -> Unit){
        podCastRepo?.searchByTerm(term) {result ->
            if (result == null){
                callback(emptyList())
            }else{
                val searchViews = result.map { podCast ->
                    PodCastToPodcastView(podCast)
                }
                callback(searchViews)
            }
        }
    }

}