package com.example.podplayer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.podplayer.R
import com.example.podplayer.util.HtmlUtil
import com.example.podplayer.viewModel.PodcastViewModel
import kotlinx.android.synthetic.main.episode_item.view.*

class EpisodeListAdapter(private var episodeViewList: List<PodcastViewModel.EpisodeViewData>?, private val episodeListAdapterListener: EpisodeListAdapterListener): RecyclerView.Adapter<EpisodeListAdapter.EpisodeHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeListAdapter.EpisodeHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.episode_item, parent, false)
        return EpisodeHolder(view, episodeListAdapterListener)
    }

    override fun getItemCount(): Int = episodeViewList?.size ?: 0

    override fun onBindViewHolder(holder: EpisodeListAdapter.EpisodeHolder, position: Int) {
        val episodeViewList = episodeViewList ?: return
        val episodeView = episodeViewList[position]
        holder.bind(episodeView)
    }
    fun setViewData(episodeList: List<PodcastViewModel.EpisodeViewData>) {
        episodeViewList = episodeList
        this.notifyDataSetChanged()
    }
    interface EpisodeListAdapterListener{
        fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    inner class EpisodeHolder(itemView: View, private val episodeListAdapterListener: EpisodeListAdapterListener): RecyclerView.ViewHolder(itemView){
        fun bind(episodeViewData: PodcastViewModel.EpisodeViewData){
            itemView.titleView.text = episodeViewData.title
            itemView.descView.text = HtmlUtil.htmlToSpannable(episodeViewData.description ?: "")
            //itemView.descView.text = episodeViewData.description
            itemView.durationView.text = episodeViewData.duration
           //itemView.releaseDateView.text = episodeViewData.releaseDate?.let { DateUtil.dateToShortDate(it) }
            itemView.setOnClickListener {
                episodeViewData.let {
                    episodeListAdapterListener.onSelectedEpisode(it)
                }
            }
        }
    }
}