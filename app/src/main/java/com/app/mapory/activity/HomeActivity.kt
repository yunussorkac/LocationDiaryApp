package com.app.mapory.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.app.mapory.screen.AddLocationScreen
import com.app.mapory.screen.LocationDetailsScreen
import com.app.mapory.screen.menu.MapsScreen
import com.app.mapory.screen.menu.ProfileScreen
import com.app.mapory.screen.menu.RecentScreen
import com.app.mapory.ui.NavigationItem
import com.app.mapory.ui.Screens
import com.app.mapory.ui.theme.AlternativeWhite
import com.app.mapory.ui.theme.AppBlue
import com.app.mapory.ui.theme.AppOrange
import com.app.mapory.ui.theme.MaporyTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemUiController = rememberSystemUiController()
            systemUiController.setStatusBarColor(
                color = AppBlue
            )
            MaporyTheme {
                HomeContent()
            }
        }
    }
}

@Composable
fun HomeContent() {
    val navController = rememberNavController()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 3 }
    )
    val coroutineScope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val items = remember {
        listOf(
            NavigationItem.Maps,
            NavigationItem.Recent,
            NavigationItem.Profile
        )
    }

    val isBottomNavRoute = remember(currentRoute) {
        currentRoute in items.map { it.route }
    }




    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = AlternativeWhite,
        bottomBar = {
            if (isBottomNavRoute) {
                key(pagerState.currentPage) {
                    BottomNavigation(
                        items = items,
                        selectedItemIndex = pagerState.currentPage,
                        onItemSelected = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page = index,
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        },
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isBottomNavRoute) {
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }
                        .collect { page ->

                        }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = pagerState.currentPage != 0,
                    key = { it }
                ) { page ->
                    key(page) {
                        when (page) {
                            0 -> MapsScreen(navController)
                            1 -> RecentScreen(navController)
                            2 -> ProfileScreen(navController)
                        }
                    }
                }
            } else {
                HomeNavigation(navController = navController)
            }
        }
    }
}

@Composable
fun HomeNavigation(navController: NavHostController) {
    val startDestination = NavigationItem.Maps.route

    NavHost(navController, startDestination = startDestination) {
        composable(NavigationItem.Maps.route) {
            MapsScreen(navController)
        }

        composable(NavigationItem.Recent.route) {
            RecentScreen(navController)
        }

        composable(NavigationItem.Profile.route) {
            ProfileScreen(navController)
        }

        composable<Screens.AddLocation> {
            val args = it.toRoute<Screens.AddLocation>()
            AddLocationScreen(navController, args.locationId)
        }

        composable<Screens.LocationDetails> {
            val args = it.toRoute<Screens.LocationDetails>()
            LocationDetailsScreen(navController, args.locationId)
        }
    }
}


@Composable
fun BottomNavigation(
    items: List<NavigationItem>,
    selectedItemIndex: Int,
    onItemSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier.height(70.dp),
        containerColor = AppBlue,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedItemIndex == index
                BottomNavigationItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onItemSelected(index) }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationItem(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tintColor = if (isSelected) AppOrange else Color.White

    Column(
        modifier = modifier.clickable( indication = null,
            interactionSource = remember { MutableInteractionSource() }){
            onClick()
        }
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = item.icon),
            contentDescription = item.title,
            modifier = Modifier.size(24.dp),
            tint = tintColor
        )

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.title,
            color = tintColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }

}