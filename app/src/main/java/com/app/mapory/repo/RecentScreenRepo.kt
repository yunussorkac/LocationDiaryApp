package com.app.mapory.repo

import com.app.mapory.model.LocationCategory
import com.app.mapory.model.MapLocation
import com.app.mapory.viewmodel.SortOrder
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

class RecentScreenRepo {
    private val pageSize = 5
    private var lastVisible: DocumentSnapshot? = null
    private var isFetching = false
    private var currentSortOrder = SortOrder.BY_ID

    private val _locationListState = MutableStateFlow<LocationUiState>(LocationUiState.Initial)
    val locationListState: StateFlow<LocationUiState> = _locationListState

    private var selectedCategories: Set<LocationCategory> = LocationCategory.entries.toSet()
    private var startDate: String = ""
    private var endDate: String = ""

    private fun buildBaseQuery(
        firestore: FirebaseFirestore,
        userId: String
    ): Query {
        val orderByField = when (currentSortOrder) {
            SortOrder.BY_ID -> "id"
            SortOrder.BY_DATE -> "dateMillis"
        }

        var query = firestore.collection("Locations")
            .document(userId)
            .collection("Locations")
            .orderBy(orderByField, Query.Direction.DESCENDING)

        if (startDate.isNotEmpty()) {
            if (startDate == endDate) {
                query = query.whereEqualTo("date", startDate)
            } else if (endDate.isNotEmpty()) {
                query = query
                    .whereGreaterThanOrEqualTo("date", startDate)
                    .whereLessThanOrEqualTo("date", endDate)
            }
        }

        return query
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        if (currentSortOrder != sortOrder) {
            currentSortOrder = sortOrder
            lastVisible = null
        }
    }


    suspend fun fetchInitialData(firestore: FirebaseFirestore, userId: String) {
        if (isFetching) return

        try {
            isFetching = true
            _locationListState.value = LocationUiState.Loading
            lastVisible = null

            var query = buildBaseQuery(firestore, userId)
                .limit(pageSize.toLong())

            val snapshot = query.get().await()

            if (!snapshot.isEmpty) {
                var locations = snapshot.documents
                    .mapNotNull { it.toObject(MapLocation::class.java) }

                if (selectedCategories.size != LocationCategory.entries.size) {
                    locations = locations.filter { location ->
                        location.category in selectedCategories
                    }
                }

                lastVisible = snapshot.documents.lastOrNull()
                _locationListState.value = if (locations.isEmpty()) {
                    LocationUiState.Empty
                } else {
                    LocationUiState.Success(
                        locations = locations,
                        canLoadMore = snapshot.documents.size >= pageSize
                    )
                }
            } else {
                _locationListState.value = LocationUiState.Empty
            }
        } catch (e: Exception) {
            _locationListState.value = LocationUiState.Error(e.message ?: "Unknown error occurred")
        } finally {
            isFetching = false
        }
    }

    suspend fun loadMoreData(firestore: FirebaseFirestore, userId: String) {
        if (isFetching || lastVisible == null) return

        val currentState = _locationListState.value as? LocationUiState.Success ?: return

        try {
            isFetching = true

            var query = buildBaseQuery(firestore, userId)
                .startAfter(lastVisible!!)
                .limit(pageSize.toLong())

            val snapshot = query.get().await()

            if (!snapshot.isEmpty) {
                var newLocations = snapshot.documents
                    .mapNotNull { it.toObject(MapLocation::class.java) }

                if (selectedCategories.size != LocationCategory.entries.size) {
                    newLocations = newLocations.filter { location ->
                        location.category in selectedCategories
                    }
                }

                lastVisible = snapshot.documents.lastOrNull()

                if (newLocations.isEmpty() && snapshot.documents.size >= pageSize) {
                    isFetching = false
                    loadMoreData(firestore, userId)
                    return
                }

                _locationListState.value = LocationUiState.Success(
                    locations = currentState.locations + newLocations,
                    canLoadMore = snapshot.documents.size >= pageSize
                )
            }
        } catch (e: Exception) {
            _locationListState.value = LocationUiState.Error(e.message ?: "Unknown error occurred")
        } finally {
            isFetching = false
        }
    }

    suspend fun searchLocations(firestore: FirebaseFirestore, userId: String, query: String) {
        if (isFetching) return

        try {
            isFetching = true
            _locationListState.value = LocationUiState.Loading

            val searchQuery = query.lowercase()
            var baseQuery = buildBaseQuery(firestore, userId)

            val snapshot = baseQuery.get().await()

            if (!snapshot.isEmpty) {
                var locations = snapshot.documents
                    .mapNotNull { it.toObject(MapLocation::class.java) }
                    .filter { location ->
                        location.title.lowercase().contains(searchQuery)
                    }

                // Kategori filtrelemesini memory'de yap
                if (selectedCategories.size != LocationCategory.entries.size) {
                    locations = locations.filter { location ->
                        location.category in selectedCategories
                    }
                }

                _locationListState.value = if (locations.isEmpty()) {
                    LocationUiState.Empty
                } else {
                    LocationUiState.Success(
                        locations = locations,
                        canLoadMore = false
                    )
                }
            } else {
                _locationListState.value = LocationUiState.Empty
            }
        } catch (e: Exception) {
            _locationListState.value = LocationUiState.Error(e.message ?: "Unknown error occurred")
        } finally {
            isFetching = false
        }
    }

    fun updateDateFilter(start: String, end: String) {
        startDate = start
        endDate = end
        lastVisible = null
    }

    fun updateFilters(categories: Set<LocationCategory>) {
        selectedCategories = categories
        lastVisible = null
    }

    fun resetFilters() {
        selectedCategories = LocationCategory.entries.toSet()
        startDate = ""
        endDate = ""
        lastVisible = null
    }
}

sealed class LocationUiState {
    data object Initial : LocationUiState()
    data object Loading : LocationUiState()
    data object Empty : LocationUiState()
    data class Success(
        val locations: List<MapLocation>,
        val canLoadMore: Boolean
    ) : LocationUiState()
    data class Error(val message: String) : LocationUiState()
}