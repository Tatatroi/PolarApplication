package com.application.polarapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.application.polarapplication.polar.PermissionHelper
import com.application.polarapplication.polar.PolarManager
import com.application.polarapplication.ui.theme.PolarApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var polarManager: PolarManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestAllPermissions(this)
        }

        // 2️⃣ Initializezi managerul Polar
        polarManager = PolarManager(this)

        // 3️⃣ Conectare la senzor (PUNE ID-UL TĂU AICI!)
        polarManager.connectToDevice("A1234567")


        enableEdgeToEdge()

        setContent {
            PolarApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PolarApplicationTheme {
        Greeting("Android")
    }
}