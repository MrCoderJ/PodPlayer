package com.example.podplayer.ui.fragment

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.podplayer.PodcastApp
import com.example.podplayer.R
import com.example.podplayer.ui.adapter.EpisodeListAdapter
import com.example.podplayer.viewModel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_podcast_details.*

class PodCastDetailFragment : Fragment(), EpisodeListAdapter.EpisodeListAdapterListener {
    private lateinit var podcastViewModel: PodcastViewModel
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private var listner: OnPodcastDetailsListner? = null
    private var menuItem: MenuItem? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setupViewModel()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_podcast_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupRecyclerView()
        updateControls()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
        menuItem = menu.findItem(R.id.menu_feed_action)
        updateMenuItem()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.menu_feed_action -> {
                podcastViewModel.activePodcastViewData?.feedUrl?.let {
                    if (podcastViewModel.activePodcastViewData?.subscribed!!){
                        listner?.onUnsubscribe()
                    }else{
                        listner?.onSubscribe()
                    }
                }
                return true
            }else -> return super.onOptionsItemSelected(item)

        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPodcastDetailsListner){
            listner = context
        }else{
            throw RuntimeException(context.toString() + "must implement OnPodcastDetailsListner")
        }
    }

    private fun setupRecyclerView(){
        feedDescTextView.movementMethod = ScrollingMovementMethod()
        episode_recycler.setHasFixedSize(true)
        val manager = LinearLayoutManager(activity)
        episode_recycler.layoutManager = manager
       // val dividerItemDecoration = DividerItemDecoration(context, HORIZONTAL)
        //episode_recycler.addItemDecoration(dividerItemDecoration)
        episodeListAdapter = EpisodeListAdapter(podcastViewModel.activePodcastViewData?.episodes, this)
        episode_recycler.adapter = episodeListAdapter
    }

    private fun setupViewModel() {
        val factory = PodcastApp.getViewModelFactory(requireActivity().application)
        podcastViewModel = ViewModelProvider(requireActivity(), factory).get(PodcastViewModel::class.java)
    }
    private fun updateControls() {
        val viewData = podcastViewModel.activePodcastViewData ?:
        return
        feedTitleTextView.text = viewData.feedTitle
        feedDescTextView.text = viewData.feedDesc
        Glide.with(requireActivity()).load(viewData.imageUrl)
            .into(feedImageView)
    }

    private fun updateMenuItem(){
        val viewData = podcastViewModel.activePodcastViewData ?: return
        menuItem?.title = if (viewData.subscribed) getString(R.string.unsubscribe) else getString(R.string.subscribe)
    }



    companion object{
        fun getInstance(): PodCastDetailFragment {
            return PodCastDetailFragment()
        }
    }

    interface OnPodcastDetailsListner{
        fun onSubscribe()
        fun onUnsubscribe()
        fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData)
    }









    override fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
       /** val controller = MediaControllerCompat.getMediaController(activity!!)
        if (controller.playbackState != null){
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING){
                controller.transportControls.pause()
            }else {
                startPlaying(episodeViewData)
            }
        }else{
            startPlaying(episodeViewData)
        }
       **/
        listner?.onShowEpisodePlayer(episodeViewData)
    }




}