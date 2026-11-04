package com.urbanflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.tomtom.sdk.init.TomTomSdk
import com.tomtom.sdk.common.configuration.buildSdkConfiguration
import com.tomtom.sdk.telemetry.UserConsent

import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.compose.TomTomMap
import com.tomtom.sdk.map.display.compose.model.MapDisplayInfrastructure
import com.tomtom.sdk.map.display.compose.state.rememberMapViewState
import com.tomtom.sdk.map.display.camera.InitialCameraOptions

class MapComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chave = getString(R.string.tomtom_api_key)

        setContent {
            var pronto by remember {
                mutableStateOf(TomTomSdk.isInitialized)
            }

            var erro by remember {
                mutableStateOf("")
            }

            LaunchedEffect(Unit) {
                try {
                    withContext(Dispatchers.IO) {
                        if (!TomTomSdk.isInitialized) {
                            val configuracao = buildSdkConfiguration(
                                context = applicationContext,
                                apiKey = chave,
                                telemetryUserConsent = {
                                    UserConsent.TelemetryOff
                                }
                            )

                            TomTomSdk.initialize(
                                context = applicationContext,
                                sdkConfiguration = configuracao
                            )
                        }
                    }

                    pronto = true

                } catch (e: Exception) {
                    erro = e.message ?: "Erro desconhecido"
                }
            }

            when {
                erro.isNotEmpty() -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Erro: $erro")
                    }
                }

                pronto -> MapaAracatuba()

                else -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Preparando mapa de Araçatuba...")
                    }
                }
            }
        }
    }
}

@Composable
fun MapaAracatuba() {

    val posicao = GeoPoint(
        latitude = -21.2089,
        longitude = -50.4328
    )

    val camera = InitialCameraOptions.LocationBased(
        position = posicao,
        zoom = 12.0
    )

    val estado = rememberMapViewState(
        initialCameraOptions = camera
    )

    val infraestrutura = remember {
        MapDisplayInfrastructure(
            sdkContext = TomTomSdk.sdkContext
        )
    }

    TomTomMap(
        state = estado,
        infrastructure = infraestrutura,
        modifier = Modifier.fillMaxSize()
    )
}