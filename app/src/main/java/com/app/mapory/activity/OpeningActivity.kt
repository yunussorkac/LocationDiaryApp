package com.app.mapory.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.mapory.R
import com.app.mapory.ui.theme.MaporyTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OpeningActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser

        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            if (currentUser != null) {
                val intent = Intent(this@OpeningActivity, HomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this@OpeningActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }


        setContent {
            MaporyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    OpeningContent(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun OpeningContent(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_transparent),
            contentDescription = "App Logo",
            modifier = Modifier.size(300.dp)
        )
    }
}

