package com.example.podplayer.api

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.podplayer.AppExecutors
import com.example.podplayer.R
import com.example.podplayer.db.PodCastDb
import com.example.podplayer.repository.PodcastRepo
import com.example.podplayer.ui.activities.MainActivity
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService


class EpisodeUpdateService : JobService() {
    override fun onStopJob(params: JobParameters?): Boolean {

        val appExecutors = AppExecutors()
        val db = PodCastDb.getDb(application)
        val podcastRepo = PodcastRepo(FeedService.instance, db.podcastDao())

        appExecutors.diskIO().execute {
            podcastRepo.updatePodcastEpisodes {podcastUpdates ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    createNotificationChannel()
                }
                for (podcatUpdate in podcastUpdates){
                    displayNotification(podcatUpdate)
                }
                jobFinished(params!!, false)
            }
        }
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        return true
    }

    companion object {
        const val EPISODE_CHANNEL_ID = "podplay_episodes_channer"
        const val EXTRA_FEED_URL = "PodcastFeedUrl"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(){
        val notificationManger = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManger.getNotificationChannel(EPISODE_CHANNEL_ID) == null){
            val channel = NotificationChannel(EPISODE_CHANNEL_ID, "Episodes", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManger.createNotificationChannel(channel)
        }
    }

    private fun displayNotification(podcastInfo: PodcastRepo.PodcastUpdateInfo){
        val contentIntent = Intent(this, MainActivity::class.java)
        contentIntent.putExtra(EXTRA_FEED_URL, podcastInfo.feedUrl)
        val pendingContentIntent = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notification  = NotificationCompat.Builder(this, EPISODE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_episode_icon)
            .setContentTitle(getString(R.string.episode_notification_title))
            .setContentText(getString(R.string.episode_notification_text, podcastInfo.newCount, podcastInfo.name))
            .setNumber(podcastInfo.newCount)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(podcastInfo.name, 0, notification)
    }
}