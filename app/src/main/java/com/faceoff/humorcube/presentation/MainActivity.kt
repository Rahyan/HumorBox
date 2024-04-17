/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.faceoff.humorcube.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.faceoff.humorcube.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.wear.compose.material.Card
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.ui.unit.dp
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
/*
import androidx.compose.foundation
import androidx.compose.foundation.layout
import androidx.compose.foundation.gestures
import androidx.compose.foundation.selection
import androidx.compose.foundation.lazy
import androidx.compose.foundation.interaction
import androidx.compose.foundation.text
*/

val customColor = Color(android.graphics.Color.parseColor("#8AB4F8"))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearApp()
        }
    }
}

suspend fun fetchJoke(jokeType: String): String {
    return withContext(Dispatchers.IO) {
        val url = URL("https://official-joke-api.appspot.com/jokes/$jokeType/random")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val scanner = Scanner(connection.inputStream)
            scanner.useDelimiter("\\A")
            val result = if (scanner.hasNext()) scanner.next() else ""
            scanner.close()
            result
        } else {
            throw Exception("Failed to fetch joke: HTTP response code $responseCode")
        }
    }
}

@Serializable
data class Joke(val id: Int, val type: String, val setup: String, val punchline: String)

suspend fun getJoke(jokeType: String): Joke {
    val json = Json { ignoreUnknownKeys = true }
    val jokeJson = fetchJoke(jokeType)
    val jokes = json.parseToJsonElement(jokeJson).jsonArray
    return json.decodeFromJsonElement(Joke.serializer(), jokes.first().jsonObject)
}

@SuppressLint("CoroutineCreationDuringComposition", "UnusedMaterialScaffoldPaddingParameter")
@Composable
fun WearApp() {
    var reloadTrigger by remember { mutableIntStateOf(0) }
    var jokeType by remember { mutableStateOf("general") }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    var joke by remember { mutableStateOf<Joke?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    coroutineScope.launch {
        scrollState.animateScrollToItem(0) // Scroll to the top
    }


    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = scrollState)},
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
            ScalingLazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .onRotaryScrollEvent {
                        coroutineScope.launch {
                            scrollState.scrollBy(it.verticalScrollPixels)
                            scrollState.animateScrollBy(0f)
                        }
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable(),
            ) {

                item {
                    TimeText()
                    Row(modifier = Modifier.padding(top = 20.dp)) {
                        TextCard(jokeType)
                    }
                }
                    item {
                        Column(modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SwitchJokeButton(
                                jokeType,
                                onSwitch = {
                                    jokeType =
                                        if (jokeType == "general") "programming" else "general"
                                },
                                scrollState
                            )
                            ReloadButton(jokeType, onSwitch = { jokeType = jokeType }, scrollState)

                        }
                    }
                }
            }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun TextCard(jokeType: String) {
    val coroutineScope = rememberCoroutineScope()
    var joke by remember { mutableStateOf<Joke?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(jokeType) { // Add reloadTrigger here
        coroutineScope.launch {
            try {
                isLoading = true
                joke = async { getJoke(jokeType) }.await()
                isLoading = false
            } catch (e: Exception) {
                isLoading = true
                error = e.message
                isLoading = false
            }
        }
    }

    Card(
        onClick = {},
        modifier = Modifier
            .padding(10.dp) // Adds padding around the card
    ) {
        Box(modifier = Modifier.fillMaxSize(),  contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(50.dp), color = Color.LightGray)
            } else if (error != null) {
                Text(text = "Error: $error", color = Color.Red)
            } else if (joke != null) {
                Column {
                    Text(text = "Joke number ${joke?.id}", style = MaterialTheme.typography.title1)
                    Text(text = joke?.setup ?: "", style = MaterialTheme.typography.body1)
                    Text(text = joke?.punchline ?: "", style = MaterialTheme.typography.body1)
                }
            }
        }
    }
}

@Composable
fun SwitchJokeButton(jokeType: String, onSwitch: () -> Unit, scrollState : ScalingLazyListState) {
    val coroutineScope = rememberCoroutineScope()
    Button(onClick = {
        onSwitch()
        coroutineScope.launch {
            scrollState.animateScrollToItem(0) // Scroll to the top
        }
    },
        colors = ButtonDefaults.buttonColors(contentColor = Color.Black, backgroundColor = customColor, )) {
        Text(text = "Type: ${jokeType ?: "unknown"}")
    }
}

@Composable
fun ReloadButton(jokeType : String, onSwitch: () -> Unit, scrollState: ScalingLazyListState) {
    val coroutineScope = rememberCoroutineScope()
    FloatingActionButton(onClick = {
        onSwitch()
        coroutineScope.launch {
            scrollState.animateScrollToItem(0) // Scroll to the top
        }
    }, backgroundColor = customColor) {
        Icon(painter = painterResource(id = R.drawable.ic_reload), contentDescription = null)
    }
}

@Preview(device = Devices.WEAR_OS_LARGE_ROUND, showSystemUi = true)
@Composable
fun PreviewTextCard() {
    WearApp()
}




