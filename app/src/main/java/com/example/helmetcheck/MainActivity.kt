package com.example.helmetcheck

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helmetcheck.ui.theme.HelmetcheckTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.livedata.observeAsState
import java.util.Locale // <-- Add this import at the top
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Data class to store lap data
data class Lap(val elapsedTime: String, val lapTime: String, val noHelmet: Boolean)

// ViewModel to manage stopwatch logic
class StopwatchViewModel : ViewModel() {
    private val _laps = MutableLiveData<List<Lap>>(emptyList())
    val laps: LiveData<List<Lap>> = _laps

    private var startTime = 0L
    private var lastLapTime = 0L
    private var isRunning = false

    val elapsedTime = MutableLiveData("00:00")

    fun startStopwatch() {
        if (isRunning) {
            stopTimer()
        } else {
            startTime = System.currentTimeMillis()
            lastLapTime = startTime
            isRunning = true
            updateTime()
        }
    }

    fun recordLap(noHelmet: Boolean) {
        if (!isRunning) return

        val now = System.currentTimeMillis()
        val lap = Lap(
            elapsedTime = formatTime(now - startTime),
            lapTime = formatTime(now - lastLapTime),
            noHelmet = noHelmet
        )
        _laps.value = _laps.value!! + lap
        lastLapTime = now
    }

    fun reset() {
        isRunning = false
        _laps.value = emptyList()
        elapsedTime.postValue("00:00")
    }

    private fun stopTimer() {
        isRunning = false
    }

    private fun updateTime() {
        viewModelScope.launch {
            while (isRunning) {
                val elapsedMillis = System.currentTimeMillis() - startTime
                if (elapsedMillis >= 15 * 60 * 1000) {
                    stopTimer()
                    break
                } else {
                    elapsedTime.postValue(formatTime(elapsedMillis))
                    delay(100)
                }
            }
        }
    }

    private fun formatTime(ms: Long): String {
        // val secs = ms / 1000
        // return String.format(Locale.US, "%02d:%02d", secs / 60, secs % 60)
        return String.format(Locale.US, "%02d:%02d.%02d",
            (ms / 60000),           // Minutes
            (ms / 1000) % 60,       // Seconds
            (ms / 10) % 100         // Hundredths of a second
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelmetcheckTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StopwatchScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun StopwatchScreen(modifier: Modifier = Modifier, viewModel: StopwatchViewModel = viewModel()) {
    val context = LocalContext.current // Needed for starting the share intent
    val elapsedTime by viewModel.elapsedTime.observeAsState("00:00")
    val laps by viewModel.laps.observeAsState(emptyList())

    // 🔹 Track whether the reset confirmation dialog should be shown
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elapsed Time Display
            Text(elapsedTime, style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(onClick = { viewModel.recordLap(false) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)) {
                    Text("Moto OK")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.recordLap(true) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Sin Casco")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { viewModel.startStopwatch() }) {
                Text("Iniciar/Parar")
            }

            /*Button(onClick = { viewModel.reset() }) {
                Text("Reset")
            }*/

            Button(onClick = { showDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Text("Reset")
            }

            // 🔹 Show the reset confirmation dialog
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false }, // Close when clicking outside
                    title = { Text("Confirmar Reset") },
                    text = { Text("¿Seguro quieres resetear los datos? Esto no se puede deshacer.") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.reset() // Reset the data
                            showDialog = false // Close the dialog
                        }) {
                            Text("Yes, Reset")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Button(onClick = { exportData(context, laps) }) {
                Text("Compartir Data")
            }
        }

        // Lap Data List (Scrollable)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 350.dp), // Adjust spacing as needed
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(laps) { lap ->
                Text("${lap.elapsedTime} - ${lap.lapTime} - ${if (lap.noHelmet) "❌ Sin Casco" else "✅ Casco"}")
            }
        }
    }
}

/*@Composable
fun StopwatchScreen(modifier: Modifier = Modifier, viewModel: StopwatchViewModel = viewModel()) {
    val context = LocalContext.current // Needed for starting the share intent
    val elapsedTime by viewModel.elapsedTime.observeAsState("00:00")
    val laps by viewModel.laps.observeAsState(emptyList())

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(elapsedTime, style = MaterialTheme.typography.headlineMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { viewModel.recordLap(false) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)) {
                Text("Moto OK")
            }
            Button(onClick = { viewModel.recordLap(true) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Sin Casco")
            }
        }

        Button(onClick = { viewModel.startStopwatch() }) {
            Text("Iniciar/Parar")
        }

        Button(onClick = { viewModel.reset() }) {
            Text("Reset")
        }

        // 🔹 NEW Share Button
        Button(onClick = { exportData(context, laps) }) {
            Text("Compartir Data")
        }

        LazyColumn {
            items(laps) { lap ->
                // Text("${lap.elapsedTime} - ${lap.lapTime} - ${lap.noHelmet}")
                Text("${lap.elapsedTime} - ${lap.lapTime} - ${if (lap.noHelmet) "❌ Sin Casco" else "✅ Casco"}")
            }
        }
    }
}*/
/*fun StopwatchScreen(modifier: Modifier = Modifier, viewModel: StopwatchViewModel = viewModel()) {
    val elapsedTime by viewModel.elapsedTime.observeAsState("00:00")
    val laps by viewModel.laps.observeAsState(emptyList())

    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(elapsedTime, style = MaterialTheme.typography.headlineLarge)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(
                onClick = { viewModel.recordLap(false) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ) {
                Text("LAP")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.recordLap(true) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("No Helmet")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.startStopwatch() }) {
            Text("Start/Stop")
        }

        Button(onClick = { viewModel.reset() }) {
            Text("Reset")
        }

        LazyColumn {
            items(laps) { lap ->
                Text("${lap.elapsedTime} - ${lap.lapTime} - ${if (lap.noHelmet) "❌ No Helmet" else "✅ Helmet"}")
            }
        }
    }
}*/

@Preview(showBackground = true)
@Composable
fun StopwatchPreview() {
    HelmetcheckTheme {
        StopwatchScreen()
    }
}

// Export lap data as a CSV file and share it
fun exportData(context: Context, laps: List<Lap>) {
    val csvData = StringBuilder("elapsed_time,lap_time,no_helmet\n")
    laps.forEach {
        csvData.append("${it.elapsedTime},${it.lapTime},${if (it.noHelmet) "TRUE" else "FALSE"}\n")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, csvData.toString())
        putExtra(Intent.EXTRA_SUBJECT, "Biker Helmet Data")
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
