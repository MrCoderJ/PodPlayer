package com.example.podplayer.util

import android.app.Application
import android.content.Context

object Auth {
    private const val AUTH = "com.example.podPlayer"

   /* fun saveUser(ctx: Context, teacher: Teacher) {
        val gson = Gson()
        val string = gson.toJson(teacher, Teacher::class.java)
        val pref = ctx.getSharedPreferences(AUTH, Context.MODE_PRIVATE)
        pref.edit()
            .putString("user", string)
            .commit()
    }

    */

   /* fun getUser(ctx: Context): Teacher{
        val gson = Gson()
        val string = ctx.getSharedPreferences(AUTH, Context.MODE_PRIVATE)
            .getString("user", "{}")
        return gson.fromJson(string, Teacher::class.java)
    }

    */


    fun saveToken(ctx: Context, token: String) {
        val pref = ctx.getSharedPreferences(AUTH, Context.MODE_PRIVATE)
        pref.edit()
            .putString("token", token)
            .commit()
    }

    fun getToken(ctx: Context): String? {
        return ctx.getSharedPreferences(AUTH, Context.MODE_PRIVATE)
            .getString("token", "")
    }

    fun isLoggedIn(ctx: Context): Boolean {
        val pref = ctx.getSharedPreferences(AUTH, Context.MODE_PRIVATE)
        return pref.contains("user") && pref.contains("token")
    }

    fun logout(app: Application) {
        /*val agentCache = AgentCache(app)
        val contentCache = ContentCache(app)
        val contribCache = ContributorCache(app)
        val notifCache = NotificationCache(app)
        val subCache = SubCache(app)
        val transCache = TransactionCache(app)
        agentCache.deleteAll()
        contentCache.deleteAll()
        contribCache.deleteAll()
        notifCache.deleteAll()
        subCache.deleteAll()
        transCache.deleteAll()*/
        val pref = app.getSharedPreferences(AUTH, Context.MODE_PRIVATE)
        pref.edit()
            .remove("user")
            .remove("token")
            .apply()
    }
}