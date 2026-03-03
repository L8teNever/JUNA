package bea.l8tenever.com.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

data class WeatherData(
    val temperature: Double,
    val weatherCode: Int,
    val description: String,
    val isDay: Boolean
)

object WeatherHelper {

    suspend fun fetchCurrentWeather(): WeatherData? = withContext(Dispatchers.IO) {
        try {
            // 1. Get rough location based on IP
            var lat = 52.52
            var lon = 13.41 // Fallback (Berlin)

            try {
                val ipUrl = URL("http://ip-api.com/json/")
                val ipConn = ipUrl.openConnection() as HttpURLConnection
                ipConn.requestMethod = "GET"
                ipConn.connectTimeout = 3000
                ipConn.readTimeout = 3000

                if (ipConn.responseCode == 200) {
                    val ipResponse = ipConn.inputStream.bufferedReader().readText()
                    val ipJson = JSONObject(ipResponse)
                    if (ipJson.getString("status") == "success") {
                        lat = ipJson.getDouble("lat")
                        lon = ipJson.getDouble("lon")
                    }
                }
            } catch (e: Exception) {
                Log.e("WeatherHelper", "Failed to get IP location, using fallback")
            }

            // 2. Fetch Weather from free Open-Meteo API
            val weatherUrl = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,is_day,weather_code")
            val wConn = weatherUrl.openConnection() as HttpURLConnection
            wConn.requestMethod = "GET"
            wConn.connectTimeout = 5000
            wConn.readTimeout = 5000

            if (wConn.responseCode == 200) {
                val wResponse = wConn.inputStream.bufferedReader().readText()
                val wJson = JSONObject(wResponse)
                val current = wJson.getJSONObject("current")

                val temp = current.getDouble("temperature_2m")
                val isDay = current.getInt("is_day") == 1
                val code = current.getInt("weather_code")

                return@withContext WeatherData(temp, code, getWeatherDescription(code), isDay)
            }
        } catch (e: Exception) {
            Log.e("WeatherHelper", "Failed to fetch weather", e)
        }
        return@withContext null
    }

    // WMO Weather interpretation codes
    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Klar"
            1, 2, 3 -> "Bewölkt"
            45, 48 -> "Nebel"
            51, 53, 55 -> "Nieselregen"
            56, 57 -> "Gefrierender Niesel"
            61, 63, 65 -> "Regen"
            66, 67 -> "Gefrierender Regen"
            71, 73, 75 -> "Schnee"
            77 -> "Schneegriesel"
            80, 81, 82 -> "Regenschauer"
            85, 86 -> "Schneeschauer"
            95 -> "Gewitter"
            96, 99 -> "Hagel"
            else -> "Unbekannt"
        }
    }
}
