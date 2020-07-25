package com.example.podplayer.ui.fragment

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ComponentName
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.podplayer.PodcastApp
import com.example.podplayer.R
import com.example.podplayer.api.MediaCallback
import com.example.podplayer.api.MediaService
import com.example.podplayer.util.HtmlUtil
import com.example.podplayer.viewModel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_episode_player.*

class EpisodePlayerFragment: Fragment() {
    private lateinit var podcastViewModel: PodcastViewModel
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    private var playerSpeed: Float = 1.0f
    private var episodeDuration: Long = 0
    private var draggingScrubber: Boolean = false
    private var progressAnimator: ValueAnimator? = null
    private var mediaSession: MediaSessionCompat? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playOnPrepare: Boolean = false
    private var isVideo: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!isVideo){
            initMediaBrowser()
        }
        setUpViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_episode_player, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        if (isVideo){
            initMediaSession()
            initVideoPlayer()
        }
        updateControl()
    }

    override fun onStart() {
        super.onStart()
        if (!isVideo){
            if (mediaBrowser.isConnected){
                if (MediaControllerCompat.getMediaController(requireActivity()) == null){
                    registerMediaController(mediaBrowser.sessionToken)
                }
                updateControllsFromController()
            }else{
                mediaBrowser.connect()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        progressAnimator?.cancel()
        if (MediaControllerCompat.getMediaController(requireActivity()) != null){
            mediaControllerCallback?.let { MediaControllerCompat.getMediaController(requireActivity()).unregisterCallback(it) }
        }
        if (isVideo){
            mediaPlayer?.setDisplay(null)
        }
        if (!requireActivity().isChangingConfigurations){
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // for the surface view to show the duration of the media
    private fun updateControlsFromMetadata(metaData: MediaMetadataCompat){
        episodeDuration = metaData.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        endTimeTextView.text = DateUtils.formatElapsedTime(episodeDuration/1000)
        seekBar.max = episodeDuration.toInt()
    }

    private fun updateControllsFromController(){
        val controller = MediaControllerCompat.getMediaController(requireActivity())
        if (controller != null){
            val metaData = controller.metadata
            if (metaData != null){
                handleStateChange(controller.playbackState.state, controller.playbackState.position, controller.playbackState.playbackSpeed)
                updateControlsFromMetadata(controller.metadata)
            }
        }
    }

    inner class MediaControllerCallback: MediaControllerCompat.Callback(){
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println("metadata changed to ${metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)}")
            metadata?.let { updateControlsFromMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            println("State changed to $state")
            val state = state ?: return
            handleStateChange(state.state, state.position, state.playbackSpeed)
        }
    }

    // Takes care of the speed in the audio playback
    private fun changeSpeed(){
        playerSpeed += 0.25f
        if (playerSpeed > 2.0f){
            playerSpeed = 0.75f
        }
        val bundle = Bundle()
        bundle.putFloat(MediaCallback.CMD_EXTRA_SPEED, playerSpeed)

        val controller = MediaControllerCompat.getMediaController(requireActivity())
        controller.sendCommand(MediaCallback.CMD_CHANGESPEED, bundle, null)
        speedButton.text = "${playerSpeed}x"
    }


    inner class MediaBrowserCallBacks: MediaBrowserCompat.ConnectionCallback(){
        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            println("onConnected")
            updateControllsFromController()
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed")
        }

    }

    // creates the mediabrowser
    private fun initMediaBrowser() {
        mediaBrowser = MediaBrowserCompat(
            activity,
            ComponentName(requireActivity(), MediaService::class.java),
            MediaBrowserCallBacks(),
            null)
    }
    private fun registerMediaController(token: MediaSessionCompat.Token){
        val mediaController = MediaControllerCompat(activity, token)
        MediaControllerCompat.setMediaController(requireActivity(), mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)

    }

    //takes care of changing the seekbar
    private fun animateScrubber(progress: Int, speed: Float){
        val timeRemaining = ((episodeDuration - progress)/speed).toInt()

        progressAnimator = ValueAnimator.ofInt(progress, episodeDuration.toInt())
        progressAnimator?.let {animator ->
            animator.duration = timeRemaining.toLong()
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                if (draggingScrubber){
                    animator.cancel()
                }else{
                    seekBar.progress = animator.animatedValue as Int
                }
            }
            animator.start()
        }
    }

    // setups the viewmodel
    private fun setUpViewModel(){
        val factory = PodcastApp.getViewModelFactory(requireActivity().application)
        podcastViewModel = ViewModelProvider(requireActivity(), factory).get(PodcastViewModel::class.java)
        isVideo = podcastViewModel.activeEpisodeViewData?.isVideo ?: false
    }

    private fun updateControl(){
        episodeDescTextView.text = podcastViewModel.activeEpisodeViewData?.title
        val htmlDesc = podcastViewModel.activeEpisodeViewData?.description ?: ""
        val descSpan = HtmlUtil.htmlToSpannable(htmlDesc)
        episodeDescTextView.text = descSpan
        episodeDescTextView.movementMethod = ScrollingMovementMethod()

        Glide.with(requireActivity()).load(podcastViewModel.activePodcastViewData?.imageUrl).into(episodeImageView)
        mediaPlayer?.let { updateControllsFromController() }
    }

    // plays the episodes
    private fun startPlaying(episodeViewData: PodcastViewModel.EpisodeViewData){
        val controller = MediaControllerCompat.getMediaController(requireActivity())

        val viewData = podcastViewModel.activePodcastViewData ?: return
        val bundle = Bundle()
        bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE, episodeViewData.title)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, viewData.feedTitle)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, viewData.imageUrl)
        controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl), bundle)
    }


    private fun togglePlayPause() {
        playOnPrepare = true
        val controller = MediaControllerCompat.getMediaController(requireActivity())
        if (controller.playbackState != null) {
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
            }
        } else {
            podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
        }
    }


    private fun setupControls(){
        playToggleButton.setOnClickListener {
            togglePlayPause()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            speedButton.setOnClickListener { changeSpeed() }
        }
        forwardButton.setOnClickListener{
            seekBy(30)
        }
        replayButton.setOnClickListener {
            seekBy(-10)
        }
        seekBar.setOnSeekBarChangeListener(@SuppressLint("AppCompatCustomView")
        object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentTimeTextView.text = DateUtils.formatElapsedTime((progress/1000).toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                draggingScrubber = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                draggingScrubber = false
                val controller = MediaControllerCompat.getMediaController(activity!!)
                if (controller.playbackState != null){
                    controller.transportControls.seekTo(seekBar!!.progress.toLong())
                }else{
                    seekBar!!.progress = 0
                }
            }

        })
    }

    private fun seekBy(seconds: Int){
        val controller = MediaControllerCompat.getMediaController(requireActivity())
        val newPosition = controller.playbackState.position + seconds*1000
        controller.transportControls.seekTo(newPosition)
    }

    private fun handleStateChange(state: Int, position: Long, speed: Float){
        progressAnimator?.let {
            it.cancel()
            progressAnimator = null
        }
        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
        playToggleButton.isActivated = isPlaying

        val progress = position.toInt()
        seekBar.progress = progress
        speedButton.text = "$playerSpeed"
        if (isPlaying){
            if (isVideo){
                setupVideoUI()
            }
            animateScrubber(progress, speed)
        }
    }

    /**
     * Code below for the video feed
     * It takes care of showing the Surface View for the video if the PodCast is a video
     */

    //creates the media session for video feed
    private fun initMediaSession(){
        if (mediaSession == null){
            mediaSession = MediaSessionCompat(requireActivity(), "EpisodePlayerFragment")
            mediaSession?.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            mediaSession?.setMediaButtonReceiver(null)
            registerMediaController(mediaSession!!.sessionToken)
        }
    }

    //set surface size to match the video aspect ratio
    private fun setSurfaceSize(){
        val mediaPlayer = mediaPlayer ?: return
        val videoWidth = mediaPlayer.videoWidth
        val videoHeight = mediaPlayer.videoHeight

        val parent = videoSurfaceView.parent as View
        val containerViewWidth = parent.width
        val containerViewHeight = parent.height

        val layoutAspectRatio = containerViewWidth.toFloat() /containerViewHeight
        val videoAspectRatio = videoWidth.toFloat() / videoHeight

        val layoutParams = videoSurfaceView.layoutParams
        if (videoAspectRatio > layoutAspectRatio){
            layoutParams.height = (containerViewWidth/ videoAspectRatio).toInt()
        }else{
            layoutParams.width = (containerViewHeight*videoAspectRatio).toInt()
        }
        videoSurfaceView.layoutParams = layoutParams
    }

    // creates the media player
    private fun initMediaPlayer(){
        if (mediaPlayer == null){
            mediaPlayer = MediaPlayer()
            mediaPlayer?.let { it ->
                it.setAudioStreamType(AudioManager.STREAM_MUSIC)
                it.setDataSource(podcastViewModel.activeEpisodeViewData?.mediaUrl)
                it.setOnPreparedListener{
                    val episodeMediaCallback = MediaCallback(requireActivity(), mediaSession!!, it)
                    mediaSession!!.setCallback(episodeMediaCallback)
                    setSurfaceSize()
                    if (playOnPrepare){
                        togglePlayPause()
                    }
                }
                it.prepareAsync()
            }
        }else{
            setSurfaceSize()
        }
    }

    // creates the surfaceview to initialize mediaplayer
    private fun initVideoPlayer(){
        videoSurfaceView.visibility = View.VISIBLE
        val surfaceHolder = videoSurfaceView.holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback{
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder?) {}

            override fun surfaceCreated(holder: SurfaceHolder?) {
                initMediaPlayer()
                mediaPlayer?.setDisplay(holder)
            }

        })
    }

    // setup the UI to clear other views when it is video
    private fun setupVideoUI(){
        episodeDescTextView.visibility = View.INVISIBLE
        headerView.visibility = View.INVISIBLE
        val activity  = activity as AppCompatActivity
        activity.supportActionBar?.hide()
        playerControls.setBackgroundColor(Color.argb(255/2, 0,0, 0))
    }

    companion object{
         fun newInstance(): EpisodePlayerFragment{
            return EpisodePlayerFragment()
        }

    }

}