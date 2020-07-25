package com.example.podplayer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.podplayer.R
import com.example.podplayer.model.PodCastSummaryViewData

class PodcastListAdapter(private var podcastSummaryViewList: List<PodCastSummaryViewData>?,
                         private val podcastListAdapterListener: PodcastListAdapterListener
                         ) : RecyclerView.Adapter<PodcastListAdapter.ViewHolder>() {

    interface PodcastListAdapterListener {
        fun onShowDetails(podcastSummaryViewData: PodCastSummaryViewData)
    }

    inner class ViewHolder(v: View, private val podcastListAdapterListener:PodcastListAdapterListener):RecyclerView.ViewHolder(v){
       var podcastSummaryViewData: PodCastSummaryViewData? = null
        val nameTextView: TextView = v.findViewById(R.id.name)
        val lastUpdatedTextView: TextView = v.findViewById(R.id.lastUpdate)
        val podcastImageView: ImageView = v.findViewById(R.id.avatar)
        init {
            v.setOnClickListener {
                podcastSummaryViewData?.let {
                    podcastListAdapterListener.onShowDetails(it)
                }
            }
        }
    }

    fun setSearchData(podcastSummaryViewData: List<PodCastSummaryViewData>) {
        podcastSummaryViewList = podcastSummaryViewData
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.holder_podcast, parent, false),
            podcastListAdapterListener)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val searchViewList = podcastSummaryViewList ?: return
        val searchView = searchViewList[position]
        holder.podcastSummaryViewData = searchView
        holder.nameTextView.text = searchView.name
        holder.lastUpdatedTextView.text = searchView.lastUpdated
        Glide.with(holder.podcastImageView).load(searchView.imageUrl).into(holder.podcastImageView)
    }

    override fun getItemCount(): Int {
        return podcastSummaryViewList?.size ?: 0
    }

}