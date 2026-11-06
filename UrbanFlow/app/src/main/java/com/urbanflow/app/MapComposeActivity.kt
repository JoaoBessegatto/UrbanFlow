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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.style.StandardStyles

private const val MAP_TAG = "UrbanFlowMap"
private const val INITIAL_ZOOM = 15.0
private val COR_URBANFLOW = Color(0xFF167D72)
private val COR_TEXTO = Color(0xFF102B46)
private val CENTRO_ARACATUBA = GeoPoint(latitude = -21.2089, longitude = -50.4328)

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
                pronto -> MapaAracatuba(onVoltar = { finish() })
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
fun MapaAracatuba(onVoltar: () -> Unit) {
    // Zoom 15: ruas e bairros mais legiveis do que na visao ampla (zoom 12).
    val cameraInicial = remember {
        InitialCameraOptions.LocationBased(
            position = CENTRO_ARACATUBA,
            zoom = INITIAL_ZOOM,
            tilt = 0.0,
            rotation = 0.0
        )
    }

    // Mantem TextureView: no projeto anterior, SurfaceView causava tela preta.
    val estado = rememberMapViewState(
        initialCameraOptions = cameraInicial,
        initialStyle = StandardStyles.TomTomOrbisMaps.BROWSING,
        renderToTextureView = true
    )

    val trafego = rememberTrafficState(
        showTrafficFlow = true,
        showTrafficIncidents = true
    )

    val infraestrutura = remember {
        MapDisplayInfrastructure(sdkContext = TomTomSdk.sdkContext) {
            locationInfrastructure = MapLocationInfrastructure {
                locationProvider = TomTomSdk.locationProvider
            }
        }
    }

    val scope = rememberCoroutineScope()
    var mapaPronto by remember { mutableStateOf(false) }
    var demoraNoCarregamento by remember { mutableStateOf(false) }
    var mostrandoSatelite by remember { mutableStateOf(false) }
    var alterandoEstilo by remember { mutableStateOf(false) }
    var avisoEstilo by remember { mutableStateOf<String?>(null) }
    var mostrarAjuda by remember { mutableStateOf(false) }

    // Evita requisicoes extras de estilo: troca apenas sob acao do usuario.
    fun trocarEstilo(satelite: Boolean) {
        if (alterandoEstilo || satelite == mostrandoSatelite || !mapaPronto) return

        scope.launch {
            alterandoEstilo = true
            avisoEstilo = null
            try {
                estado.styleState.loadStyle(
                    if (satelite) {
                        StandardStyles.TomTomOrbisMaps.SATELLITE
                    } else {
                        StandardStyles.TomTomOrbisMaps.BROWSING
                    }
                )
                mostrandoSatelite = satelite
            } catch (e: Exception) {
                Log.e(MAP_TAG, "Falha ao mudar estilo do mapa", e)
                avisoEstilo = "Não foi possível carregar este estilo. Confira a conexão e as permissões da chave TomTom."
            } finally {
                alterandoEstilo = false
            }
        }
    }

    LaunchedEffect(mapaPronto) {
        if (!mapaPronto) {
            delay(15_000L)
            demoraNoCarregamento = true
            Log.w(MAP_TAG, "Mapa demorou para iniciar; verifique rede e autorizacao TomTom.")
        } else {
            demoraNoCarregamento = false
        }
    }

    val zoomAtual = estado.cameraState.data?.position?.zoom ?: INITIAL_ZOOM

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF3F6F8))
    ) {
        TomTomMap(
            state = estado,
            infrastructure = infraestrutura,
            modifier = Modifier.fillMaxSize(),
            onMapReady = {
                mapaPronto = true
                Log.i(MAP_TAG, "Mapa pronto; mostrando o centro de Aracatuba.")
            }
        ) {
            Traffic(state = trafego)
        }

        // Cabecalho fixo: identifica a regiao mesmo antes de os tiles carregarem.
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 5.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onVoltar) {
                    Text("← Voltar", color = COR_URBANFLOW)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mapa inteligente", fontWeight = FontWeight.Bold, color = COR_TEXTO)
                    Text("Araçatuba • SP", color = Color(0xFF627D98))
                }
            }
        }

        // Estilos nativos da TomTom; sem JSON de estilo adicional no inicio.
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 89.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { trocarEstilo(false) },
                    enabled = mapaPronto && !alterandoEstilo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!mostrandoSatelite) COR_URBANFLOW else Color(0xFF627D98)
                    )
                ) {
                    Text("Ruas")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = { trocarEstilo(true) },
                    enabled = mapaPronto && !alterandoEstilo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mostrandoSatelite) COR_URBANFLOW else Color(0xFF627D98)
                    )
                ) {
                    Text("Satélite")
                }
            }
        }

        // Controles de zoom vinculados ao estado REAL da camera, incluindo gestos.
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp)
                ) {
                    TextButton(
                        onClick = {
                            estado.cameraState.moveCamera(
                                CameraOptions(zoom = (zoomAtual + 1.0).coerceAtMost(20.0))
                            )
                        },
                        enabled = mapaPronto
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, color = COR_TEXTO)
                    }
                    TextButton(
                        onClick = {
                            estado.cameraState.moveCamera(
                                CameraOptions(zoom = (zoomAtual - 1.0).coerceAtLeast(5.0))
                            )
                        },
                        enabled = mapaPronto
                    ) {
                        Text("−", fontWeight = FontWeight.Bold, color = COR_TEXTO)
                    }
                    TextButton(
                        onClick = {
                            estado.cameraState.moveCamera(
                                CameraOptions(
                                    position = CENTRO_ARACATUBA,
                                    zoom = INITIAL_ZOOM,
                                    tilt = 0.0,
                                    rotation = 0.0
                                )
                            )
                        },
                        enabled = mapaPronto
                    ) {
                        Text("Centro", color = COR_URBANFLOW)
                    }
                }
            }
        }

        // Avisos sao mostrados sem desmontar o componente do mapa.
        if (!mapaPronto) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(22.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!demoraNoCarregamento) CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (demoraNoCarregamento) {
                            "O mapa está demorando. Verifique internet, OpenGL ES 3.0 e autorização da chave para mapas."
                        } else {
                            "Carregando ruas de Araçatuba..."
                        },
                        color = COR_TEXTO,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 5.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            trafego.showTrafficFlow = !trafego.showTrafficFlow
                        }
                    ) {
                        Text(
                            if (trafego.showTrafficFlow) "● Fluxo ligado" else "○ Fluxo oculto",
                            color = COR_URBANFLOW
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { mostrarAjuda = !mostrarAjuda }) {
                        Text(if (mostrarAjuda) "Fechar ajuda" else "Ajuda", color = COR_TEXTO)
                    }
                }

                if (trafego.showTrafficFlow) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ItemLegenda(Color(0xFF27AE60), "Livre")
                        ItemLegenda(Color(0xFFF2C94C), "Moderado")
                        ItemLegenda(Color(0xFFEB5757), "Intenso")
                    }
                }
                if (alterandoEstilo) {
                    Text("Carregando estilo do mapa...", color = Color(0xFF627D98))
                }
                if (avisoEstilo != null) {
                    Text(avisoEstilo ?: "", color = Color(0xFFB3261E))
                }
                if (mostrarAjuda) {
                    Text(
                        "Use dois dedos para aproximar ou afastar. Se faltarem ruas, confira se a chave possui acesso ao Maps and Navigation SDK, além da Traffic API, e veja o Logcat UrbanFlowMap.",
                        color = COR_TEXTO
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemLegenda(cor: Color, descricao: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("●", color = cor)
        Spacer(modifier = Modifier.width(4.dp))
        Text(descricao, color = Color(0xFF627D98))
    }
}
