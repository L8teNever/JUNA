package bea.l8tenever.com.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.core.text.HtmlCompat

sealed class UntisResult<out T> {
    data class Success<T>(val data: T) : UntisResult<T>()
    data class Error(val message: String) : UntisResult<Nothing>()
}

class UntisRepository {

    private val gson = Gson()
    private val api  = UntisApiClient.service
    private val TAG  = "UntisRepository"

    // ─── Login ───────────────────────────────────────────────────────────────

    suspend fun login(creds: LoginCredentials): UntisResult<LoginResult> = runCatching {
        val url = UntisApiClient.buildUrl(creds.serverUrl, creds.school)
        Log.d(TAG, "=== AUTHENTICATE ===  url=$url  user=${creds.username}")

        // Login-Anfrage – noch KEIN Cookie (noch keine Session)
        val response = api.call(
            url    = url,
            body   = UntisRequest(
                method = "authenticate",
                params = LoginParams(user = creds.username, password = creds.password)
            ),
            cookie = ""   // kein Cookie beim Login selbst
        )

        Log.d(TAG, "HTTP ${response.code()} ${response.message()}")
        if (!response.isSuccessful)
            return UntisResult.Error("[authenticate] HTTP ${response.code()}: ${response.message()}")

        val body = response.body()
            ?: return UntisResult.Error("[authenticate] Leere Antwort")

        Log.d(TAG, "error=${body.error}  result=${body.result}")
        if (body.error != null)
            return UntisResult.Error("[authenticate] ${body.error.message} (Code ${body.error.code})")

        val result = gson.fromJson(body.result, LoginResult::class.java)
            ?: return UntisResult.Error("[authenticate] JSON-Parsing fehlgeschlagen")

        if (result.sessionId.isNullOrBlank())
            return UntisResult.Error("[authenticate] Keine SessionID zurückgegeben")

        // Set-Cookie Header aus der HTTP-Antwort lesen (der echte JSESSIONID-Cookie)
        val setCookieHeaders = response.headers().values("Set-Cookie")
        Log.d(TAG, "Set-Cookie Headers (${setCookieHeaders.size}): $setCookieHeaders")

        // Alle Cookies aus Set-Cookie sammeln (JSESSIONID, schoolname, Tenant-Id, ...)
        val cookieMap = mutableMapOf<String, String>()
        for (header in setCookieHeaders) {
            val nameValue = header.substringBefore(";").trim()
            if ("=" in nameValue) {
                val name  = nameValue.substringBefore("=").trim()
                val value = nameValue.substringAfter("=").trim()
                cookieMap[name] = value
                Log.d(TAG, "Cookie aus Header: $name = $value")
            }
        }

        // JSESSIONID bevorzugt aus Set-Cookie, fallback auf JSON sessionId
        val jsessionFromCookie = cookieMap["JSESSIONID"]
        val jsessionFromJson   = result.sessionId
        val finalJsession = jsessionFromCookie ?: jsessionFromJson

        Log.d(TAG, "JSESSIONID aus Set-Cookie: $jsessionFromCookie")
        Log.d(TAG, "JSESSIONID aus JSON-Body:  $jsessionFromJson")
        Log.d(TAG, "Verwendetes JSESSIONID:    $finalJsession")

        if (jsessionFromCookie != null && jsessionFromCookie != jsessionFromJson) {
            Log.w(TAG, "⚠️ UNTERSCHIED! Set-Cookie=$jsessionFromCookie  JSON=$jsessionFromJson")
        }

        // Alle Cookies in SessionStore speichern
        SessionStore.jsessionId  = finalJsession ?: jsessionFromJson
        SessionStore.allCookies  = cookieMap  // z.B. schoolname, Tenant-Id, ...

        Log.d(TAG, "Login OK → SessionID=$finalJsession  klasseId=${result.klasseId}  personId=${result.personId}")
        UntisResult.Success(result)
    }.getOrElse { e ->
        Log.e(TAG, "Login Exception", e)
        UntisResult.Error("[authenticate] Verbindungsfehler: ${e.localizedMessage ?: e.javaClass.simpleName}")
    }

    // ─── Stundenplan – identisch mit Python-Fallback-Logik ────────────────────

    suspend fun getTimetable(
        creds: LoginCredentials,
        sessionId: String,
        klasseId: Int,
        personId: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): UntisResult<List<TimetableEntry>> = runCatching {
        val fmt   = DateTimeFormatter.ofPattern("yyyyMMdd")
        val url   = UntisApiClient.buildUrl(creds.serverUrl, creds.school)
        val start = startDate.format(fmt).toInt()
        val end   = endDate.format(fmt).toInt()

        // Cookie wie Python GANZ KLASSISCH: NUR JSESSIONID wird gesendet!
        // Einige WebUntis-Server vertragen das `schoolname`-Cookie im JSON-RPC nicht.
        val sid    = SessionStore.jsessionId.ifBlank { sessionId }
        val cookie = if (sid.isNotBlank()) "JSESSIONID=$sid" else ""

        Log.d(TAG, "=== GET TIMETABLE ===  url=$url")
        Log.d(TAG, "start=$start  end=$end  klasseId=$klasseId  personId=$personId")
        Log.d(TAG, "Cookie: $cookie")

        if (cookie.isBlank()) {
            Log.e(TAG, "KEIN JSESSIONID! Bitte erst einloggen.")
            return UntisResult.Error("Nicht eingeloggt – bitte neu anmelden")
        }

        // Lookup-Tabellen: Fächer, Räume, Lehrer (wie Python: _load_master_data)
        val subjects = fetchSubjects(url, cookie)
        val rooms    = fetchRooms(url, cookie)
        val teachers = fetchTeachers(url, cookie)
        Log.d(TAG, "Lookups: ${subjects.size} Fächer, ${rooms.size} Räume, ${teachers.size} Lehrer")

        // SCHRITT 1: Persönlicher Plan (type=5, personId) – wie Python: get_timetable(person_id, 5, ...)
        var periods: List<UntisPeriod>? = null

        if (personId > 0) {
            Log.d(TAG, "Versuch 1: getTimetable(type=5, id=$personId)")
            val resp5 = api.call(
                url    = url,
                body   = UntisRequest(method = "getTimetable", params = TimetableParams(id = personId, type = 5, startDate = start, endDate = end)),
                cookie = cookie
            )
            val body5 = resp5.body()
            Log.d(TAG, "type=5: HTTP=${resp5.code()}  error=${body5?.error?.code}/${body5?.error?.message}")
            if (resp5.isSuccessful && body5 != null && body5.error == null) {
                val t = object : TypeToken<List<UntisPeriod>>() {}.type
                val parsed: List<UntisPeriod>? = try { gson.fromJson(body5.result, t) } catch (_: Exception) { null }
                if (!parsed.isNullOrEmpty()) {
                    Log.d(TAG, "type=5 erfolgreich: ${parsed.size} Perioden")
                    periods = parsed
                }
            }
        }

        // SCHRITT 2: Klassen-Plan (type=1, klasseId) – wie Python-Fallback
        if (periods == null) {
            Log.d(TAG, "Versuch 2: getTimetable(type=1, id=$klasseId)")
            if (klasseId <= 0) {
                return UntisResult.Error("[getTimetable] klasseId=$klasseId ungültig und type=5 auch fehlgeschlagen")
            }
            val resp1 = api.call(
                url    = url,
                body   = UntisRequest(method = "getTimetable", params = TimetableParams(id = klasseId, type = 1, startDate = start, endDate = end)),
                cookie = cookie
            )
            val body1 = resp1.body()
            Log.d(TAG, "type=1: HTTP=${resp1.code()}  error=${body1?.error?.code}/${body1?.error?.message}")

            if (!resp1.isSuccessful)
                return UntisResult.Error("[getTimetable] HTTP ${resp1.code()} (type=1)  Cookie=$cookie")
            if (body1 == null)
                return UntisResult.Error("[getTimetable] Leere Antwort (type=1)  Cookie=$cookie")
            if (body1.error != null)
                return UntisResult.Error(
                    "[getTimetable] Fehler: ${body1.error.message} (Code ${body1.error.code})\n" +
                    "→ type=1, id=$klasseId\nCookie gesendet: $cookie"
                )

            val t = object : TypeToken<List<UntisPeriod>>() {}.type
            periods = try { gson.fromJson(body1.result, t) ?: emptyList() } catch (e: Exception) {
                Log.w(TAG, "Parsing-Fehler: ${e.message}"); emptyList()
            }
            Log.d(TAG, "type=1 erfolgreich: ${periods.size} Perioden")
        }

        val rawEntries = periods
            .mapNotNull { it.toEntry(subjects, rooms, teachers) }
            .sortedWith(compareBy({ it.date }, { it.startTime }))

        val entries = mergeConsecutiveLessons(rawEntries)

        Log.d(TAG, "Stundenplan fertig: ${entries.size} Einträge")
        UntisResult.Success(entries)

    }.getOrElse { e ->
        Log.e(TAG, "getTimetable Exception", e)
        UntisResult.Error("[getTimetable] Verbindungsfehler: ${e.localizedMessage ?: e.javaClass.simpleName}")
    }

    // ─── Lookups ─────────────────────────────────────────────────────────────

    private fun String.fixEncoding(): String {
        var s = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        // Fallback falls WebUntis UTF-8 doppelt enkodiert hat (Beispiel Ã¤ statt ä)
        if (s.contains("Ã") || s.contains("Â")) {
            try {
                s = String(s.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
            } catch (_: Exception) {}
        }
        return s
    }

    private suspend fun fetchSubjects(url: String, cookie: String): Map<Int, String> {
        return try {
            val resp = api.call(url = url, body = UntisRequest(method = "getSubjects", params = emptyMap<String, Any>()), cookie = cookie)
            val body = resp.body() ?: return emptyMap()
            if (body.error != null) { Log.w(TAG, "getSubjects: ${body.error.message}"); return emptyMap() }
            val t = object : TypeToken<List<UntisSubject>>() {}.type
            val list: List<UntisSubject> = gson.fromJson(body.result, t) ?: emptyList()
            list.filter { it.id != null }.associate { it.id!! to (it.longName ?: it.name ?: "?").fixEncoding() }
        } catch (e: Exception) { Log.w(TAG, "getSubjects Exception: ${e.message}"); emptyMap() }
    }

    private suspend fun fetchRooms(url: String, cookie: String): Map<Int, String> {
        return try {
            val resp = api.call(url = url, body = UntisRequest(method = "getRooms", params = emptyMap<String, Any>()), cookie = cookie)
            val body = resp.body() ?: return emptyMap()
            if (body.error != null) { Log.w(TAG, "getRooms: ${body.error.message}"); return emptyMap() }
            val t = object : TypeToken<List<UntisRoom>>() {}.type
            val list: List<UntisRoom> = gson.fromJson(body.result, t) ?: emptyList()
            list.filter { it.id != null }.associate { it.id!! to (it.longName ?: it.name ?: "?").fixEncoding() }
        } catch (e: Exception) { Log.w(TAG, "getRooms Exception: ${e.message}"); emptyMap() }
    }

    private suspend fun fetchTeachers(url: String, cookie: String): Map<Int, String> {
        return try {
            val resp = api.call(url = url, body = UntisRequest(method = "getTeachers", params = emptyMap<String, Any>()), cookie = cookie)
            val body = resp.body() ?: return emptyMap()
            if (body.error != null) { Log.w(TAG, "getTeachers: ${body.error.message}"); return emptyMap() }
            val t = object : TypeToken<List<UntisTeacher>>() {}.type
            val list: List<UntisTeacher> = gson.fromJson(body.result, t) ?: emptyList()
            list.filter { it.id != null }.associate { it.id!! to (it.longName ?: it.name ?: "?").fixEncoding() }
        } catch (e: Exception) { Log.w(TAG, "getTeachers Exception: ${e.message}"); emptyMap() }
    }

    // ─── Periode → TimetableEntry ─────────────────────────────────────────────

    private fun UntisPeriod.toEntry(
        subjectMap: Map<Int, String>,
        roomMap: Map<Int, String>,
        teacherMap: Map<Int, String>
    ): TimetableEntry? {
        val d  = date      ?: return null
        val st = startTime ?: return null
        val et = endTime   ?: return null

        val dateStr  = d.toString().let { "${it.substring(0,4)}-${it.substring(4,6)}-${it.substring(6,8)}" }
        val startStr = st.toString().padStart(4, '0').let { "${it.substring(0,2)}:${it.substring(2,4)}" }
        val endStr   = et.toString().padStart(4, '0').let { "${it.substring(0,2)}:${it.substring(2,4)}" }

        fun resolveIds(refs: List<UntisIdRef>?, map: Map<Int, String>): String {
            if (refs.isNullOrEmpty()) return "---"
            return refs.mapNotNull { ref -> ref.id?.let { map[it] ?: "ID:$it" } }
                       .joinToString(", ").ifBlank { "---" }
        }

        return TimetableEntry(
            id          = id ?: 0,
            date        = dateStr,
            startTime   = startStr,
            endTime     = endStr,
            subject     = resolveIds(subjects, subjectMap),
            room        = resolveIds(rooms, roomMap),
            teacher     = resolveIds(teachers, teacherMap),
            isCancelled = isCancelled
        )
    }

    private fun mergeConsecutiveLessons(entries: List<TimetableEntry>): List<TimetableEntry> {
        if (entries.isEmpty()) return emptyList()
        val merged = mutableListOf<TimetableEntry>()
        var current = entries.first()

        for (i in 1 until entries.size) {
            val next = entries[i]

            val t1 = try { LocalTime.parse(current.endTime) } catch (e: Exception) { null }
            val t2 = try { LocalTime.parse(next.startTime) } catch (e: Exception) { null }
            
            var gapMinutes = -1L
            if (t1 != null && t2 != null) {
                gapMinutes = Duration.between(t1, t2).toMinutes()
            }

            val isConsecutive = current.date == next.date &&
                                gapMinutes in 0..20 &&
                                current.subject == next.subject &&
                                current.room == next.room &&
                                current.teacher == next.teacher &&
                                current.isCancelled == next.isCancelled &&
                                current.subject != "Pause"

            if (isConsecutive) {
                // Merge next into current
                current = current.copy(endTime = next.endTime)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }
}
