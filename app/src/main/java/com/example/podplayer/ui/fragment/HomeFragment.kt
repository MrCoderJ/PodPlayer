package com.example.podplayer.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.podplayer.PodcastApp
import com.example.podplayer.R
import com.example.podplayer.api.FeedService
import com.example.podplayer.db.PodCastDb
import com.example.podplayer.repository.PodCastRepo
import com.example.podplayer.repository.PodcastRepo
import com.example.podplayer.ui.adapter.PodCastListAdapter
import com.example.podplayer.ui.adapter.PodcastListAdapter
import com.example.podplayer.viewModel.PodcastViewModel
import com.example.podplayer.viewModel.SearchViewModel
import kotlinx.android.synthetic.main.activity_main.recycler_view
import kotlinx.android.synthetic.main.fragment_home.*


/**
 * A simple subclass.
 * Use the factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment(){
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var podcastViewModel: PodcastViewModel
    private lateinit var adapter : PodCastListAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModels()
        initViews()
        setRecyclerView()

    }

    private fun initViews(){
        searchView.run {
            searchView.addTextChangedListener(object: TextWatcher{
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if ( s != null){
                        val string = s.toString()
                        if (string.isEmpty()){
                            Toast.makeText(context, "Nothing to Display", Toast.LENGTH_LONG).show()
                        }else{
                            performSearch(string)
                        }
                    }
                }

            })
        }
    }

    private fun showProgressBar() {
        progressBars.visibility = View.VISIBLE
    }

    // Hides the progress bar
    private fun hideProgressBar() {
        progressBars.visibility = View.INVISIBLE
    }

    private fun performSearch(term: String) {
        showProgressBar()
        searchViewModel.searchPodCasts(term) { result ->
            hideProgressBar()
            podcastListAdapter.setSearchData(result)
        }
        val podcastRepo = PodCastRepo(requireActivity().application)
        podcastRepo.searchByTerm(term) { Log.i("PodCast", "Result = $it") }
    }
    private fun setupViewModels(){
        val podcastRepo = PodCastRepo(requireActivity().application)
        val factory = PodcastApp.getViewModelFactory(requireActivity().application)
        searchViewModel = ViewModelProvider(this, factory).get(SearchViewModel::class.java)
        searchViewModel.podCastRepo = podcastRepo

        podcastViewModel = ViewModelProvider(this, factory).get(PodcastViewModel::class.java)
        val feedService = FeedService.instance
        val podcastDao = PodCastDb.getDb(requireActivity().application).podcastDao()
        podcastViewModel.podcastRepo = PodcastRepo(feedService, podcastDao)

    }
    private fun setRecyclerView() {
        recycler_view.setHasFixedSize(true)
        val manager = LinearLayoutManager(context)
        recycler_view.layoutManager = manager

        //podcastListAdapter = PodcastListAdapter(null, this)
        adapter = PodCastListAdapter {  }
        //recycler_view.adapter = podcastListAdapter
        recycler_view.adapter = adapter
    }



    private fun showError(message: String) {
        AlertDialog.Builder(context).setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null).create().show()
    }

  /**  override fun onShowDetails(podcastSummaryViewData: PodCastSummaryViewData) {
        val feedUrl =  podcastSummaryViewData.feedUrl ?: return
        showProgressBar()
        podcastViewModel.getPodcast(podcastSummaryViewData){
            hideProgressBar()
            if (it != null){
                startActivity(Intent(context, EpisodeActivity::class.java))
            }else{
                showError("Error loading feed $feedUrl")
            }
        }
    }
  **/


}