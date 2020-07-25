package com.example.podplayer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.podplayer.R
import com.example.podplayer.model.PodCastSummaryViewData
import kotlinx.android.synthetic.main.holder_podcast.view.*

class PodCastListAdapter(val onClick: (podCastSummaryViewData: PodCastSummaryViewData) -> Unit):RecyclerView.Adapter<PodCastListAdapter.PodCastHolder>() {
    private  var podcastSummaryViewList = listOf<PodCastSummaryViewData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodCastListAdapter.PodCastHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.holder_podcast, parent, false)
        return PodCastHolder(view)
    }

    interface PodcastListAdapterListener{
        fun showDetails(podCastSummaryViewData: PodCastSummaryViewData)
    }

    override fun getItemCount(): Int {
        return podcastSummaryViewList.size
    }

    fun setSearchData(podCastSummaryViewData: List<PodCastSummaryViewData>){
        podcastSummaryViewList = podCastSummaryViewData
        notifyDataSetChanged()
    }
    override fun onBindViewHolder(holder: PodCastListAdapter.PodCastHolder, position: Int) {
        val searchViewList = podcastSummaryViewList
        val searchView = searchViewList[position]
        holder.bind(searchView)
    }
    inner class PodCastHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        fun bind(podCastSummaryViewData: PodCastSummaryViewData){
            itemView.name.text = podCastSummaryViewData.name
            itemView.lastUpdate.text = podCastSummaryViewData.lastUpdated
            Glide.with(itemView.avatar).load(podCastSummaryViewData.imageUrl).into(itemView.avatar)
            itemView.setOnClickListener {
                onClick(podCastSummaryViewData)


            }
        }
    }
}


