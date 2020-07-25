package com.example.podplayer.ui.activities

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.podplayer.PodcastApp
import com.example.podplayer.R
import com.example.podplayer.api.EpisodeUpdateService
import com.example.podplayer.api.FeedService
import com.example.podplayer.db.PodCastDb
import com.example.podplayer.model.PodCastSummaryViewData
import com.example.podplayer.repository.PodCastRepo
import com.example.podplayer.repository.PodcastRepo
import com.example.podplayer.ui.adapter.PodcastListAdapter
import com.example.podplayer.ui.fragment.EpisodePlayerFragment
import com.example.podplayer.ui.fragment.PodCastDetailFragment
import com.example.podplayer.viewModel.PodcastViewModel
import com.example.podplayer.viewModel.SearchViewModel
import com.firebase.jobdispatcher.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener,
    PodCastDetailFragment.OnPodcastDetailsListner {

    private lateinit var searchViewModel: SearchViewModel
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem
    private lateinit var podcastViewModel: PodcastViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        setupViewModels()
        setRecyclerView()
        setUpPodcastListView()


        //podcastViewModel = ViewModelProviders.of(this).get(PodcastViewModel::class.java)
        //  podcastViewModel.podcastRepo = PodcastRepo()

        /**   podcastListAdapter = PodCastListAdapter {
        showFragment(it)
        showDetailsFragment(it)
        }
         **/
        // val podCastSummaryViewData =PodCastSummaryViewData()

        handleIntent(intent)
        addBackStackListener()
        scheduleJobs()
    }

    private fun setupViewModels() {
        val factory = PodcastApp.getViewModelFactory(application)
        val podcastRepo = PodCastRepo(application)
        searchViewModel = ViewModelProvider(this, factory ).get(SearchViewModel::class.java)
        searchViewModel.podCastRepo = podcastRepo

        podcastViewModel = ViewModelProvider(this, factory).get(PodcastViewModel::class.java)
        val feedService = FeedService.instance
        val podcastDao = PodCastDb.getDb(application).podcastDao()
        podcastViewModel.podcastRepo = PodcastRepo(feedService, podcastDao)
    }

   /** private fun showFragment(podCastSummaryViewData: PodCastSummaryViewData) {
        val feedUrl = podCastSummaryViewData.feedUrl ?: return
        showProgressBar()
        podcastViewModel.getPodcast(podCastSummaryViewData) {
            hideProgressBar()
            if (it != null) {
                showDetailsFragment()
            } else {
                showError("Error Loading feed $feedUrl ")
            }
        }
    }
   **/

    // Creates menu for search item
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)
        searchMenuItem = menu.findItem(R.id.search_item)
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                showSubscribedPodcast()
                return true
            }

        })
        val searchView = searchMenuItem.actionView as SearchView

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        if (recycler_view.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }
        return true
    }

    // Performs the search
    private fun performSearch(term: String) {
        showProgressBar()
        searchViewModel.searchPodCasts(term) { result ->
            hideProgressBar()
            podcastListAdapter.setSearchData(result)
        }
        val podcastRepo = PodCastRepo(application)
        podcastRepo.searchByTerm(term) { Log.i("PodCast", "Result = $it") }
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            performSearch(query)
        }
        val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateService.EXTRA_FEED_URL)
        if (podcastFeedUrl != null){
            podcastViewModel.setActivePodcast(podcastFeedUrl){
                it?.let { podCastSummaryViewData ->  onShowDetails(podCastSummaryViewData) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    //Handles the recyclerview display
    private fun setRecyclerView() {
        recycler_view.setHasFixedSize(true)
        val manager = LinearLayoutManager(this)
        recycler_view.layoutManager = manager

        podcastListAdapter = PodcastListAdapter(null, this)
        recycler_view.adapter = podcastListAdapter
    }

    // Shows progress bar
    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    // Hides the progress bar
    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }

    companion object {
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
        private const val TAG_EPISODE_UPDATE_JOB = "com.example.podplayer.episodes"
        private const val TAG_PLAYER_FRAGMENT = "PlayerFragment"
    }

    // Schedules the notification to run
    private fun scheduleJobs() {
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
        val onehourInSeconds = 60 * 60
        val tenMinutesInSeconds = 60 * 10
        val episodeUpdateJob =
            dispatcher.newJobBuilder()
                .setService(EpisodeUpdateService::class.java)
                .setTag(TAG_EPISODE_UPDATE_JOB)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(onehourInSeconds, (onehourInSeconds + tenMinutesInSeconds)))
                .setLifetime(Lifetime.FOREVER)
                .setConstraints(Constraint.ON_UNMETERED_NETWORK, Constraint.DEVICE_CHARGING)
                .build()
        dispatcher.mustSchedule(episodeUpdateJob)
    }

    // This creates the podcastDetailFragment and attach it to the mainactivity
    private fun createPodcastDetailsFragment(): PodCastDetailFragment {
        var podcastDetailFragment =
            supportFragmentManager.findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodCastDetailFragment?
        if (podcastDetailFragment == null) {
            podcastDetailFragment = PodCastDetailFragment.getInstance()
        }
        return podcastDetailFragment
    }

    private fun showDetailsFragment() {
        val podcastDetailsFragment = createPodcastDetailsFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.podcastDetailsContainer, podcastDetailsFragment)
            .addToBackStack(TAG_DETAILS_FRAGMENT).commit()
        recycler_view.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    // Function displays error
    private fun showError(message: String) {
        AlertDialog.Builder(this).setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null).create().show()
    }

    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                recycler_view.visibility = View.VISIBLE
            }
        }
    }

    override fun onShowDetails(podcastSummaryViewData: PodCastSummaryViewData) {
        val feedUrl = podcastSummaryViewData.feedUrl ?: return
        showProgressBar()
        podcastViewModel.getPodcast(podcastSummaryViewData) {
            hideProgressBar()
            if (it != null) {
                showDetailsFragment()
            } else {
                showError("Error loading feed $feedUrl")
            }
        }
    }

    // Subscribe the the channel
    override fun onSubscribe() {
        podcastViewModel.saveActivePodcast()
        supportFragmentManager.popBackStack()
    }

    //Unsubscribe a channel
    override fun onUnsubscribe() {
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData) {
        podcastViewModel.activeEpisodeViewData = episodeViewData
        showPlayerFragment()
    }

    // to display only subscribed Podcast
    private fun showSubscribedPodcast() {
        val podcasts = podcastViewModel.getPodcasts()?.value
        if (podcasts != null) {
            podcastListAdapter.setSearchData(podcasts)
        }
    }

    // Creates the EpisodePlayerFragment
    private fun createEpisodePlayerFragment(): EpisodePlayerFragment{
        var episodePlayerFragment = supportFragmentManager.findFragmentByTag(TAG_PLAYER_FRAGMENT) as EpisodePlayerFragment?
        if (episodePlayerFragment == null){
            episodePlayerFragment = EpisodePlayerFragment.newInstance()
        }
        return episodePlayerFragment
    }

    // Displays the created fragment
    private fun showPlayerFragment(){
        val episodePlayerFragment = createEpisodePlayerFragment()
        supportFragmentManager.beginTransaction().replace(R.id.podcastDetailsContainer, episodePlayerFragment, TAG_PLAYER_FRAGMENT)
            .addToBackStack("PlayerFragment").commit()
        recycler_view.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }


    // Displays subscribed podcast in the mainactivity recyclerview
    private fun setUpPodcastListView() {
        podcastViewModel.getPodcasts()?.observe(this, Observer {
            if (it != null) {
                showSubscribedPodcast()
            }
        })
    }


}