package com.app.mapory.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.mapory.screen.auth.LoginScreen
import com.app.mapory.screen.auth.RegisterScreen
import com.app.mapory.ui.Screens
import com.app.mapory.ui.theme.MaporyTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return
        }

        setContent {
            MaporyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    MainContent(modifier = Modifier.padding(it))
                }
            }
        }
    }
}

@Composable
fun MainContent(modifier: Modifier = Modifier) {
    val navController = rememberNavController()



    Scaffold(
        containerColor = Color.White,
        content = { padding ->
            Box(
                modifier = Modifier.padding(padding)
            ) {
                MainNavigation(navController = navController)
            }
        }
    )
}

@Composable
fun MainNavigation(navController: NavHostController) {

    NavHost(navController = navController, startDestination = Screens.Login) {

        composable<Screens.Login> {
            LoginScreen(navController)
        }

        composable<Screens.Register> {
            RegisterScreen(navController)

        }


    }
}