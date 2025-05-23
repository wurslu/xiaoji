package com.example.thebest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thebest.data.network.NetworkModule
import com.example.thebest.data.repository.SensorRepository
import com.example.thebest.ui.compose.SensorScreen
import com.example.thebest.ui.theme.TheBestTheme
import com.example.thebest.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val httpClient = NetworkModule.provideHttpClient()
        val apiService = NetworkModule.provideApiService(httpClient)
        val repository = SensorRepository(apiService)
        enableEdgeToEdge()
        setContent {
            TheBestTheme {
                val viewModel = viewModel<MainViewModel> {
                    MainViewModel(repository)
                }
                SensorScreen(viewModel = viewModel)
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
    TheBestTheme {
        Greeting("Android")
    }
}