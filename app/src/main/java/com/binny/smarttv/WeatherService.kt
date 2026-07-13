package com.binny.smarttv

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherData(
    val temperature: Double,
    val weatherCode: Int
) {
    val icon: String get() = when (weatherCode) {
        0 -> "☀️"
        1, 2, 3 -> "⛅"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57 -> "🌦️"
        61, 63, 65, 66, 67 -> "🌧️"
        71, 73, 75, 77 -> "🌨️"
        80, 81, 82 -> "🌧️"
        85, 86 -> "🌨️"
        95, 96, 99 -> "⛈️"
        else -> "🌤️"
    }

    val tempDisplay: String get() = "${temperature.toInt()}°"
}

object WeatherService {

    @Volatile private var latitude = 13.08
    @Volatile private var longitude = 80.27

    fun setLocation(lat: Double, lon: Double) {
        latitude = lat
        longitude = lon
    }

    fun fetchWeather(): WeatherData? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current_weather=true"
            )
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val response = conn.inputStream.bufferedReader().readText()

            val json = JSONObject(response)
            val current = json.getJSONObject("current_weather")
            WeatherData(
                temperature = current.getDouble("temperature"),
                weatherCode = current.getInt("weathercode")
            )
        } catch (e: Exception) {
            Log.e("MomTV", "Weather fetch failed", e)
            null
        } finally {
            conn?.disconnect()
        }
    }
}
