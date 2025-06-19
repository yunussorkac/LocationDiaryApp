package com.app.mapory.screen.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.app.mapory.R
import com.app.mapory.model.LocationCategory
import com.app.mapory.repo.LocationUiState
import com.app.mapory.ui.Screens
import com.app.mapory.ui.components.filter.DateRangeDialog
import com.app.mapory.ui.components.filter.FilterDialog
import com.app.mapory.ui.components.recent.LocationItem
import com.app.mapory.ui.theme.AlternativeWhite
import com.app.mapory.ui.theme.AppBlue
import com.app.mapory.ui.theme.AppOrange
import com.app.mapory.viewmodel.RecentScreenViewModel
import com.app.mapory.viewmodel.SortOrder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentScreen (navHostController: NavHostController){

    val viewModel = koinViewModel<RecentScreenViewModel>()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()

    var showFilterDialog by remember { mutableStateOf(false) }

    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var showDateFilterDialog by remember { mutableStateOf(false) }
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()

    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()


    LaunchedEffect(Unit) {
        viewModel.fetchInitialData()
        listState.scrollToItem(0)
    }



    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val lastIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val currentState = locationState
            if (currentState is LocationUiState.Success) {
                val totalItems = currentState.locations.size
                lastIndex >= totalItems - 1 && currentState.canLoadMore
            } else {
                false
            }
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) {
                viewModel.loadMoreData()
            }
        }
    }

    val onLocationClick = remember {
        { locationId: String ->
            navHostController.navigate(Screens.LocationDetails(locationId))
        }
    }

    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            lastVisibleItemIndex > (totalItemsNumber - 2)
        }
    }

    LaunchedEffect(loadMore.value) {
        if (loadMore.value && locationState is LocationUiState.Success &&
            (locationState as LocationUiState.Success).canLoadMore) {
            viewModel.loadMoreData()
        }
    }



    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier
            .fillMaxSize()
            .background(AlternativeWhite)
        ) {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .focusRequester(focusRequester)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color.White,
                                focusedTextColor = Color.White
                            ),
                            placeholder = {
                                Text(
                                    "Search by title",
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = {
                                            viewModel.onSearchQueryChange("")
                                            focusRequester.requestFocus()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.White
                                        )
                                    }
                                }
                            } else null
                        )
                    } else {
                        Text(
                            text = "History",
                            fontSize = 24.sp,
                            color = Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBlue
                ),
                actions = {
                    if (isSearchActive) {
                        IconButton(
                            onClick = {
                                viewModel.setSearchActive(false)
                                focusManager.clearFocus()
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    } else {
                        Row {
                            if (startDate.isNotEmpty() || endDate.isNotEmpty() ||
                                selectedCategories.size != LocationCategory.entries.size) {
                                IconButton(
                                    onClick = { viewModel.resetFilters() },
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppOrange)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset Filters",
                                        tint = Color.White
                                    )
                                }
                            }


                            IconButton(
                                onClick = { viewModel.setSearchActive(true) },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { showDateFilterDialog = true },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DateRange,
                                    contentDescription = "Filter by Date",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { showFilterDialog = true },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_filter_alt_24),
                                    contentDescription = "Filter Categories",
                                    tint = Color.White
                                )
                            }

                        }
                    }

                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                when (val currentState = locationState) {
                    is LocationUiState.Initial -> {}
                    is LocationUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center),
                            color = AppBlue
                        )
                    }
                    is LocationUiState.Empty -> {
                    }
                    is LocationUiState.Success -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(
                                items = currentState.locations,
                                key = { location -> location.id }
                            ) { location ->
                                LocationItem(location, onLocationClick)
                            }



                        }
                    }
                    is LocationUiState.Error -> {
                    }
                }

            }

        }
        val scope = rememberCoroutineScope()
        FloatingActionButton(
            onClick = {
                viewModel.toggleSortOrder()
                scope.launch {
                    listState.scrollToItem(0)
                }
            },
            modifier = Modifier
                .padding(24.dp)
                .shadow(12.dp, CircleShape)
                .align(Alignment.BottomEnd),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            ),
            containerColor = AppBlue,
            contentColor = Color.White
        ) {
            Icon(
                painter = when (sortOrder) {
                    SortOrder.BY_ID -> painterResource(R.drawable.baseline_add_location_24)
                    SortOrder.BY_DATE ->painterResource(R.drawable.baseline_date_range_24)
                },
                contentDescription = "Toggle Sort Order",
                tint = Color.White
            )
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            selectedCategories = selectedCategories,
            onDismiss = { showFilterDialog = false },
            onSave = { categories ->
                viewModel.updateSelectedCategories(categories)
                showFilterDialog = false
            }
        )
    }

    if (showDateFilterDialog) {
        DateRangeDialog (
            startDate = startDate,
            endDate = endDate,
            onDismiss = { showDateFilterDialog = false },
            onSave = { start, end ->
                viewModel.updateDateFilter(start, end)
                showDateFilterDialog = false
            }
        )
    }

}


