package bea.l8tenever.com.data

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

/**
 * Retrofit-Service.
 * Das Cookie wird EXPLIZIT als Header übergeben – genau wie im Python-Skript:
 *   headers['Cookie'] = f"JSESSIONID={self.session_id}"
 * So gibt es keine Abhängigkeit von Interceptor-Timing oder CookieJar.
 */
interface UntisApiService {
    @POST
    suspend fun call(
        @Url    url: String,
        @Body   body: UntisRequest,
        @Header("Cookie") cookie: String = ""   // ← direkt wie Python
    ): retrofit2.Response<UntisResponse<com.google.gson.JsonElement>>
}

/**
 * Speichert die aktuelle SessionID nach dem Login.
 * Wird von UntisRepository nach erfolgreichem authenticate() gesetzt.
 */
object SessionStore {
    @Volatile
    var jsessionId: String = ""

    @Volatile
    var allCookies: Map<String, String> = emptyMap()

    fun clear() {
        jsessionId = ""
        allCookies = emptyMap()
    }

    /** Baut den Cookie-Header aus JSESSIONID und allen anderen Set-Cookies (schoolname etc.) */
    val cookieHeader: String
        get() {
            val parts = mutableListOf<String>()
            if (jsessionId.isNotBlank()) parts.add("JSESSIONID=$jsessionId")
            allCookies.forEach { (k, v) ->
                if (k != "JSESSIONID") parts.add("$k=$v")
            }
            return parts.joinToString("; ")
        }
}

object UntisApiClient {

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS  // Headers im Logcat sichtbar
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                val body = response.body
                if (body != null) {
                    val bytes = body.bytes()
                    val utf8String = String(bytes, Charsets.UTF_8)
                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val newBody = utf8String.toResponseBody(mediaType)
                    response.newBuilder().body(newBody).build()
                } else {
                    response
                }
            }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val service: UntisApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://placeholder.webuntis.com/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UntisApiService::class.java)
    }

    fun buildUrl(serverUrl: String, school: String): String {
        val base = serverUrl.trimEnd('/')
        return "$base/WebUntis/jsonrpc.do?school=${school.trim()}"
    }
}
