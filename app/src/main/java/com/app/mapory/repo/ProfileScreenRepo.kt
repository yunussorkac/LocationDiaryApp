package com.app.mapory.repo

import android.os.Build
import androidx.annotation.RequiresApi
import com.app.mapory.model.LocationCategory
import com.app.mapory.model.MapLocation
import com.app.mapory.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class ProfileScreenRepo {

    suspend fun getUserProfile(firestore: FirebaseFirestore, userId: String): User? {
        return try {
            firestore.collection("Users")
                .document(userId)
                .get()
                .await()
                .toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserEmail(firestore : FirebaseFirestore, userId: String): Result<String> = try {
        val userSnapshot = firestore.collection("Users")
            .document(userId)
            .get()
            .await()

        if (userSnapshot.exists()) {
            val user = userSnapshot.toObject(User::class.java)
            Result.success(user?.email ?: "")
        } else {
            Result.failure(Exception("User not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUsername(firestore : FirebaseFirestore, userId: String): Result<String> = try {
        val userSnapshot = firestore.collection("Users")
            .document(userId)
            .get()
            .await()

        if (userSnapshot.exists()) {
            val user = userSnapshot.toObject(User::class.java)
            Result.success(user?.username ?: "")
        } else {
            Result.failure(Exception("User not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getLatestLocationById(firestore: FirebaseFirestore,userId: String): Result<MapLocation> = try {
        val snapshot = firestore.collection("Locations")
            .document(userId)
            .collection("Locations")
            .orderBy("id", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        if (snapshot.isEmpty) {
            Result.failure(Exception("No location found"))
        } else {
            Result.success(snapshot.documents[0].toObject(MapLocation::class.java)!!)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getLocationStats(firestore: FirebaseFirestore, userId: String): Result<LocationStats> = try {
        val locations = firestore.collection("Locations")
            .document(userId)
            .collection("Locations")
            .get()
            .await()
            .toObjects(MapLocation::class.java)

        val currentDate = LocalDateTime.now(ZoneOffset.UTC)
        val startOfMonth = currentDate.withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
        val startOfWeek = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .withHour(0)
            .withMinute(0)
            .withSecond(0)

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        var monthlyCount = 0
        var weeklyCount = 0
        val categoryCount = mutableMapOf<LocationCategory, Int>()

        locations.forEach { location ->
            val locationDate = LocalDate.parse(location.date, dateFormatter).atStartOfDay()

            if (locationDate >= startOfMonth) {
                monthlyCount++
            }

            if (locationDate >= startOfWeek) {
                weeklyCount++
            }

            categoryCount[location.category] = categoryCount.getOrDefault(location.category, 0) + 1
        }

        Result.success(
            LocationStats(
                totalVisits = locations.size,
                monthlyVisits = monthlyCount,
                weeklyVisits = weeklyCount,
                categoryStats = categoryCount
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

}

data class LocationStats(
    val totalVisits: Int = 0,
    val monthlyVisits: Int = 0,
    val weeklyVisits: Int = 0,
    val categoryStats: Map<LocationCategory, Int> = emptyMap()
)