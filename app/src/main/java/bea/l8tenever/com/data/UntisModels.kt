package bea.l8tenever.com.data

import com.google.gson.annotations.SerializedName

// ─── JSON-RPC Rahmen ────────────────────────────────────────────────────────

data class UntisRequest(
    val id: String     = "1",
    val method: String,
    val params: Any,
    val jsonrpc: String = "2.0"
)

data class UntisResponse<T>(
    val id: String?,
    val jsonrpc: String?,
    val result: T?,
    val error: UntisError?
)

data class UntisError(
    val code: Int,
    val message: String
)

// ─── Login ──────────────────────────────────────────────────────────────────

data class LoginParams(
        val user: String,
        val password: String,
        val client: String = "YUNA-App"
    )

data class LoginResult(
    val sessionId: String?,
    val personType: Int?,
    val personId: Int?,
    val klasseId: Int?
)

// ─── Stundenplan ─────────────────────────────────────────────────────────────

data class TimetableParams(
    val id: Int,
    val type: Int,
    val startDate: Int,
    val endDate: Int
)

/** Klassen-Eintrag aus getKlassen */
data class UntisKlasse(
    val id: Int?,
    val name: String?,
    @SerializedName("longName") val longName: String?
)

/**
 * Eine Periode aus der WebUntis JSON-RPC Antwort.
 * Felder sind mit @SerializedName korrekt auf die abgekürzten
 * API-Feldnamen gemappt: su=subjects, ro=rooms, te=teachers, kl=classes.
 */
data class UntisPeriod(
    val id: Int?,
    val lessonId: Int?,
    val date: Int?,            // YYYYMMDD
    val startTime: Int?,       // HHMM
    val endTime: Int?,         // HHMM
    @SerializedName("su") val subjects: List<UntisIdRef>?,
    @SerializedName("ro") val rooms: List<UntisIdRef>?,
    @SerializedName("te") val teachers: List<UntisIdRef>?,
    @SerializedName("kl") val classes: List<UntisIdRef>?,
    val code: String?,         // "cancelled" wenn Ausfall
    val activityType: String?
) {
    val isCancelled: Boolean get() = code == "cancelled"
}

/** Nur ID-Referenz in einer Periode (name wird separat aufgelöst) */
data class UntisIdRef(
    val id: Int?
)

// ─── Lookup-Tabellen (getSubjects / getRooms / getTeachers) ─────────────────

data class UntisSubject(
    val id: Int?,
    val name: String?,
    @SerializedName("longName") val longName: String?
)

data class UntisRoom(
    val id: Int?,
    val name: String?,
    @SerializedName("longName") val longName: String?
)

data class UntisTeacher(
    val id: Int?,
    val name: String?,
    @SerializedName("longName") val longName: String?,
    @SerializedName("foreName") val foreName: String?
)

// ─── App-interne Modelle ─────────────────────────────────────────────────────

data class TimetableEntry(
    val id: Int,
    val date: String,        // "YYYY-MM-DD"
    val startTime: String,   // "HH:MM"
    val endTime: String,     // "HH:MM"
    val subject: String,
    val room: String,
    val teacher: String,
    val isCancelled: Boolean
)

data class LoginCredentials(
    val serverUrl: String,
    val school: String,
    val username: String,
    val password: String
)
