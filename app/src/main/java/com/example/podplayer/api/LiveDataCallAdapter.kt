package com.example.podplayer.api

import android.util.Log
import androidx.lifecycle.LiveData
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean

class LiveDataCallAdapter<R>(private val respType: Type): CallAdapter<R, LiveData<Resource<R>>> {

    override fun responseType(): Type = respType

    override fun adapt(call: Call<R>): LiveData<Resource<R>> {
        return object: LiveData<Resource<R>>(){
            private var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)){
                    postValue(Resource.loading(null))
                    call.enqueue(object: Callback<R>{
                        override fun onResponse(call: Call<R>, response: Response<R>) {
                            Log.d("RESPONCE CODE", " IS ${response.code()}")
                            if (response.isSuccessful) {
                                postValue(Resource.success(response.body()))
                            } else {
                                when {
                                    response.code() == 500 -> postValue(Resource.error("Sorry, something went wrong", null))
                                    response.code() == 401 -> postValue(Resource.logout(null))
                                    else -> {
                                        val msg = response.errorBody()?.string()

                                        val errorMsg = if (response.code() == 400 && msg != null) {
                                            parse(msg)
                                        } else {
                                            if (msg.isNullOrEmpty()) {
                                                response.message()
                                            } else {
                                                msg
                                            }
                                        }
                                        postValue(Resource.error(errorMsg, null))
                                    }
                                }
                            }
                        }
                        override fun onFailure(call: Call<R>, t: Throwable) {
                            Log.d("ERROR MSG ", " IS ${t.message}")
                            postValue(Resource.error(t.message ?: "Unknown error", null))
                        }

                    })
                }
            }
        }
    }



    private fun parse(string: String): String {
        return try {
            val json = JSONObject(string)
            var msgs = ""
            for(key in json.keys()) {
                val values = json.getJSONArray(key)
                msgs += values.join("\n")
            }
            msgs.replace("\"", "")
        } catch (e: JSONException) {
            Log.d("PARSE JSONException ", "error -> ${e.message}")
            string
        }
    }

}