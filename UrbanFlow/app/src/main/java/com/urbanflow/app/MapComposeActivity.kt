package com.urbanflow.app

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

import com.tomtom.sdk.init.TomTomSdk
import com.tomtom.sdk.common.configuration.buildSdkConfiguration
import com.tomtom.sdk.telemetry.UserConsent
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.MapLocationInfrastructure
import com.tomtom.sdk.map.display.compose.TomTomMap
import com.tomtom.sdk.map.display.compose.model.MapDisplayInfrastructure
import com.tomtom.sdk.map.display.compose.nodes.Traffic
import com.tomtom.sdk.map.display.compose.state.rememberMapViewState
import com.tomtom.sdk.map.display.compose.state.rememberTrafficState
import com.tomtom.sdk.map.display.camera.InitialCameraOptions

private const val MAP_TAG = "UrbanFlowMap"

class MapComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chave = getString(R.string.tomtom_api_key).trim()
        val suportaOpenGl3 = (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo.reqGlEsVersion >= 0x30000

        setContent {
            var pronto by remember { mutableStateOf(TomTomSdk.isInitialized && chave.isNotBlank() && suportaOpenGl3) }
            var erro by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(chave, suportaOpenGl3) {
                try {
                    when {
                        chave.isBlank() -> error(
                            "Configure tomtomApiKey em local.properties e sincronize o Gradle."
                        )
                        !suportaOpenGl3 -> error(
                            "O dispositivo ou emulador precisa oferecer OpenGL ES 3.0 ou superior."
                        )
                    }

                    // O SDK deve ser inicializado uma unica vez, fora da thread da UI.
                    withContext(Dispatchers.IO) {
                        if (!TomTomSdk.isInitialized) {
                            val configuracao = buildSdkConfiguration(
                                context = applicationContext,
                                apiKey = chave,
                                telemetryUserConsent = { UserConsent.TelemetryOff }
                            )
                            TomTomSdk.initialize(
                                context = applicationContext,
                                sdkConfiguration = configuracao
                            )
                        }
                    }

                    pronto = true
                    Log.i(MAP_TAG, "SDK TomTom inicializado; preparando mapa.")
                } catch (e: Exception) {
                    Log.e(MAP_TAG, "Falha na inicializacao do mapa TomTom", e)
                    erro = if (chave.isBlank() || !suportaOpenGl3) {
                        e.message
                    } else {
                        "Não foi possível iniciar o mapa. Confira sua conexão e a autorização da chave para Maps and Navigation SDK."
                    }
                }
            }

            when {
                erro != null -> MensagemMapa(erro ?: "Erro desconhecido")
                pronto -> MapaAracatuba()
                else -> MensagemMapa("Preparando o mapa de Araçatuba...", carregando = true)
            }
        }
    }
}

@Composable
private fun MensagemMapa(mensagem: String, carregando: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F6F8))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (carregando) {
            CircularProgressIndicator()
        }
        Text(
            text = mensagem,
            color = Color(0xFF102B46),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun MapaAracatuba() {
    val posicao = GeoPoint(
        latitude = -21.2089,
        longitude = -50.4328
    )

    val camera = remember {
        InitialCameraOptions.LocationBased(
            position = posicao,
            zoom = 12.0
        )
    }

    // TextureView evita problemas de sobreposicao/renderizacao de SurfaceView
    // em alguns dispositivos e emuladores Android.
    val estado = rememberMapViewState(
        initialCameraOptions = camera,
        renderToTextureView = true
    )

    val trafego = rememberTrafficState(
        showTrafficFlow = true,
        showTrafficIncidents = true
    )

    // A configuracao explicita segue o exemplo oficial do SDK Compose.
    val infraestrutura = remember {
        MapDisplayInfrastructure(sdkContext = TomTomSdk.sdkContext) {
            locationInfrastructure = MapLocationInfrastructure {
                locationProvider = TomTomSdk.locationProvider
            }
        }
    }

    var mapaPronto by remember { mutableStateOf(false) }
    var demoraNoCarregamento by remember { mutableStateOf(false) }
    var mostrarAjuda by remember { mutableStateOf(false) }

    LaunchedEffect(mapaPronto) {
        if (!mapaPronto) {
            delay(20_000L)
            demoraNoCarregamento = true
            Log.w(MAP_TAG, "Mapa nao ficou pronto em 20 segundos. Verifique chave, rede e OpenGL.")
        } else {
            demoraNoCarregamento = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F6F8))
    ) {
        TomTomMap(
            state = estado,
            infrastructure = infraestrutura,
            modifier = Modifier.fillMaxSize(),
            onMapReady = {
                mapaPronto = true
                Log.i(MAP_TAG, "onMapReady: renderizador do mapa iniciou.")
            }
        ) {
            Traffic(state = trafego)
        }

        if (!mapaPronto) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .background(Color(0xFFF3F6F8))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!demoraNoCarregamento) {
                    CircularProgressIndicator()
                }
                Text(
                    text = if (demoraNoCarregamento) {
                        "O mapa não carregou. Confira a conexão, a permissão da chave para Maps and Navigation SDK e o OpenGL ES 3.0 do dispositivo."
                    } else {
                        "Carregando mapa e trânsito de Araçatuba..."
                    },
                    color = Color(0xFF102B46),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Button(onClick = { mostrarAjuda = !mostrarAjuda }) {
                Text(if (mostrarAjuda) "Fechar ajuda" else "Mapa não aparece?")
            }

            if (mostrarAjuda) {
                Text(
                    text = "Confira: conexão do emulador/celular; chave habilitada para Maps and Navigation SDK for Android; OpenGL ES 3.0; Logcat com filtro UrbanFlowMap. O acesso à Traffic API, sozinho, não garante acesso aos dados do mapa.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F6F8))
                        .padding(16.dp),
                    color = Color(0xFF102B46)
                )
            }
        }
    }
}
