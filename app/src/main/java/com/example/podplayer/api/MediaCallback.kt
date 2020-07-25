package com.example.podplayer.api

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi

class MediaCallback(
    val context: Context,
    private val mediaSession: MediaSessionCompat,
    private var mediaPlayer: MediaPlayer? = null
) : MediaSessionCompat.Callback() {

    private var mediaUri: Uri? = null
    private var newMedia: Boolean = false
    private var mediaExtras: Bundle? = null
    var listener: MediaListener? = null
    private var mediaNeedsPrepare: Boolean = false



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        println("Playing ${uri.toString()}")
        if (mediaUri == uri){
            newMedia = false
            mediaExtras = null
        }else{
            mediaExtras = extras
            setNewMedia(uri)
        }
        onPlay()


    }

    override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
        super.onCommand(command, extras, cb)
        when (command){
            CMD_CHANGESPEED -> extras?.let { changeSpeed(it) }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPlay() {
        super.onPlay()
        if (ensureAudioFocus()){
            mediaSession.isActive = true
            println("onPlay called")
            initializeMediaPlayer()
            prepareMedia()
            startPlaying()
        }
    }

    override fun onStop() {
        super.onStop()
        println("onStop called")
        stopPlaying()
    }

    override fun onPause() {
        super.onPause()
        println("onPause called")
        pausePlaying()
    }

    private fun setState(state: Int, newSpeed: Float? = null) {
        var position: Long = -1
        mediaPlayer?.let {
            position = it.currentPosition.toLong()
        }
        var speed = 1.0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            speed = newSpeed ?: (mediaPlayer?.playbackParams?.speed ?: 1.0f)
            mediaPlayer?.let {
                try {
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                }catch (e: Exception){
                    mediaPlayer?.reset()
                    mediaPlayer?.setDataSource(context, mediaUri!!)
                    mediaPlayer?.prepare()
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                    mediaPlayer?.seekTo(position.toInt())
                    if (state == PlaybackStateCompat.STATE_PLAYING){
                        mediaPlayer?.start()
                    }
                }
            }
        }
        val playbackstate = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE)
            .setState(state, position, 1.0f).build()
        mediaSession.setPlaybackState(playbackstate)
        if (state == PlaybackStateCompat.STATE_PAUSED || state == PlaybackStateCompat.STATE_PLAYING){
            listener?.onStateChanged()
        }
    }
    private fun changeSpeed(extras: Bundle){
        var playbackstate = PlaybackStateCompat.STATE_PAUSED
        if (mediaSession.controller.playbackState != null){
            playbackstate = mediaSession.controller.playbackState.state
        }
        setState(playbackstate, extras.getFloat(CMD_EXTRA_SPEED))
    }

    // Stores new Media item
    private fun setNewMedia(uri: Uri?){
        newMedia = true
        mediaUri = uri
    }

    // Grabs audio focus
    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureAudioFocus(): Boolean{
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    // Give up audio Focus
    private fun removeAudioFocus(){
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(null)
    }

    // initalizes the mediaplayer
    private fun initializeMediaPlayer(){
        if (mediaPlayer == null){
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setOnCompletionListener { setState(PlaybackStateCompat.STATE_PAUSED) }
            mediaNeedsPrepare = true
        }
    }

    // prepares media for mediaplayer
    private fun prepareMedia(){
        if (newMedia){
            newMedia = false
            mediaPlayer?.let {mediaPlayer ->
                mediaUri?.let {
                    if (mediaNeedsPrepare){
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(context, mediaUri!!)
                        mediaPlayer.prepare()
                    }
                    mediaExtras?.let {mediaExtras ->
                        mediaSession.setMetadata(MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.duration.toLong())
                            .build())
                    }
                }
            }
        }
        Log.d("TAG", "Media Displayed")
    }

    // Starts playing audio
    private fun startPlaying(){
        mediaPlayer?.let {mediaPlayer ->
            if (!mediaPlayer.isPlaying){
                mediaPlayer.start()
                setState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        mediaPlayer?.seekTo(pos.toInt())
        val playbackstate: PlaybackStateCompat? = mediaSession.controller.playbackState
        if (playbackstate != null){
            setState(playbackstate.state)
        }else{
            setState(PlaybackStateCompat.STATE_PAUSED)
        }
    }
    // pauses the audio
    private fun pausePlaying(){
        removeAudioFocus()
        mediaPlayer?.let {mediaPlayer ->  
            if (mediaPlayer.isPlaying){
                mediaPlayer.pause()
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
        listener?.onPausePlaying()
    }
    // stops audio from playing
    private fun stopPlaying(){
        removeAudioFocus()
        mediaPlayer?.let {mediaPlayer ->
            if (mediaPlayer.isPlaying){
                mediaPlayer.stop()
                setState(PlaybackStateCompat.STATE_STOPPED)
            }
        }
        listener?.onStopPlaying()
    }
    companion object{
        const val CMD_CHANGESPEED = "change_speed"
        const val CMD_EXTRA_SPEED = "speed"
    }

    interface MediaListener{
        fun onStateChanged()
        fun onStopPlaying()
        fun onPausePlaying()
    }
}