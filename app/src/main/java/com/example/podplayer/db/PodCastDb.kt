package com.example.podplayer.db

import android.app.Application
import androidx.room.*
import com.example.podplayer.model.Episode
import com.example.podplayer.model.PodCast
import java.util.*

class Converters{
    @TypeConverter
    fun fromTimestamp(value: Long?): Date?{
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long?{
        return (date?.time)
    }
}
@Database(entities = [PodCast::class, Episode::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract  class PodCastDb: RoomDatabase() {

    abstract fun podcastDao(): PodCastDao
    companion object{
        fun getDb(app: Application): PodCastDb{
            return Room.databaseBuilder(app, PodCastDb::class.java, "podcast").fallbackToDestructiveMigration().build()
        }
    }
}
