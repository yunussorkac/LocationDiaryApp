package com.app.mapory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.mapory.model.LocationCategory
import com.app.mapory.repo.LocationUiState
import com.app.mapory.repo.RecentScreenRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecentScreenViewModel(
    private val recentScreenRepo: RecentScreenRepo,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    val locationState: StateFlow<LocationUiState> = recentScreenRepo.locationListState

    private val _selectedCategories = MutableStateFlow(LocationCategory.entries.toSet())
    val selectedCategories: StateFlow<Set<LocationCategory>> = _selectedCategories

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _startDate = MutableStateFlow("")
    val startDate: StateFlow<String> = _startDate

    private val _endDate = MutableStateFlow("")
    val endDate: StateFlow<String> = _endDate

    private val _sortOrder = MutableStateFlow(SortOrder.BY_ID)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    fun updateDateFilter(startDate: String, endDate: String) {
        viewModelScope.launch {
            _startDate.value = startDate
            _endDate.value = endDate
            firebaseAuth.currentUser?.let {
                recentScreenRepo.updateDateFilter(startDate, endDate)
                recentScreenRepo.fetchInitialData(firestore, it.uid)
            }
        }
    }

    fun resetFilters() {
        viewModelScope.launch {
            _selectedCategories.value = LocationCategory.entries.toSet()
            _startDate.value = ""
            _endDate.value = ""
            firebaseAuth.currentUser?.let {
                recentScreenRepo.resetFilters()
                recentScreenRepo.fetchInitialData(firestore, it.uid)
            }
        }
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
            fetchInitialData()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                fetchInitialData()
            } else {
                firebaseAuth.currentUser?.let {
                    recentScreenRepo.searchLocations(firestore, it.uid, query)
                }
            }
        }
    }

    fun updateSelectedCategories(categories: Set<LocationCategory>) {
        viewModelScope.launch {
            _selectedCategories.value = categories
            firebaseAuth.currentUser?.let {
                recentScreenRepo.updateFilters(categories)
                recentScreenRepo.fetchInitialData(firestore, it.uid)
            }
        }
    }

    fun toggleSortOrder() {
        viewModelScope.launch {
            _sortOrder.value = if (_sortOrder.value == SortOrder.BY_ID) SortOrder.BY_DATE else SortOrder.BY_ID
            firebaseAuth.currentUser?.let {
                recentScreenRepo.updateSortOrder(_sortOrder.value)
                recentScreenRepo.fetchInitialData(firestore, it.uid)
            }
        }
    }

    fun fetchInitialData() {
        viewModelScope.launch {
            firebaseAuth.currentUser?.let {
                recentScreenRepo.fetchInitialData(firestore, it.uid)
            }
        }
    }

    fun loadMoreData() {
        viewModelScope.launch {
            firebaseAuth.currentUser?.let {
                recentScreenRepo.loadMoreData(firestore, it.uid)
            }
        }
    }

}
enum class SortOrder {
    BY_ID,
    BY_DATE
}