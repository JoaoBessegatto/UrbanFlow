# Configuração da TomTom no UrbanFlow

A integração usa duas partes da plataforma TomTom:

- **Maps and Navigation SDK** para renderizar o mapa com as camadas nativas de fluxo e incidentes.
- **Traffic API** para alimentar o status do dashboard e a Central de Alertas com dados de fluxo e incidentes.

## 1. Configurar a chave sem versionar

Crie ou edite `UrbanFlow/local.properties`:

```properties
sdk.dir=/caminho/do/Android/Sdk
tomtomApiKey=SUA_CHAVE_TOMTOM
```

Alternativamente, defina a variável de ambiente:

```bash
export TOMTOM_API_KEY="SUA_CHAVE_TOMTOM"
```

Também é possível definir `tomtomApiKey` no `~/.gradle/gradle.properties`.

> Não coloque a chave em `UrbanFlow/gradle.properties`. O arquivo `local.properties` já está ignorado pelo Git.

## 2. O que está conectado

### Dashboard

`MainActivity` consulta o Flow Segment Data no centro de Araçatuba e atualiza `txtStatusTransito` com:

- situação do trânsito;
- velocidade atual;
- velocidade em fluxo livre;
- indicação visual livre/moderado/intenso.

### Mapa

`MapComposeActivity` usa o TomTom Maps SDK e ativa:

- camada de fluxo de trânsito;
- camada de incidentes de trânsito.

### Central de alertas

`AlertasActivity` consulta incidentes ativos na área urbana de Araçatuba e exibe até 8 ocorrências priorizadas pela magnitude do atraso, incluindo:

- categoria;
- descrição;
- trecho;
- atraso estimado;
- extensão afetada.

## 3. Tratamento de falhas

Se a chave não estiver configurada, a API estiver indisponível ou não houver conexão:

- o app não fecha;
- o dashboard informa indisponibilidade;
- a Central de Alertas apresenta uma mensagem de fallback;
- as funcionalidades locais de ocorrências, trajetos, perfil e acessibilidade permanecem independentes.

## 4. Segurança

Uma chave TomTom esteve previamente versionada em `UrbanFlow/gradle.properties`. A integração remove a chave do estado atual do código, mas isso **não apaga o segredo do histórico Git**. Rotacione/revogue a chave antiga no portal da TomTom e use uma nova chave local.

## 5. Arquivos principais

- `app/src/main/java/com/urbanflow/app/data/TomTomTrafficService.java`
- `app/src/main/java/com/urbanflow/app/MainActivity.java`
- `app/src/main/java/com/urbanflow/app/AlertasActivity.java`
- `app/src/main/java/com/urbanflow/app/MapComposeActivity.kt`
- `app/build.gradle`


## 6. Mapa preto ou tela sem carregamento

A tela `MapComposeActivity` usa o renderizador **Standard** do Maps and Navigation SDK 2.6.4.
O mapa utiliza `TextureView` para reduzir problemas de composição com `SurfaceView` e exibe
uma mensagem caso o evento `onMapReady` não seja recebido em aproximadamente 20 segundos.

Verifique os pontos abaixo nesta ordem:

1. **Produto da chave:** consulte `https://my.tomtom.com/` e confira se sua chave tem acesso a
   **Maps and Navigation SDK for Android** (map data). Uma chave que funciona para a
   **Traffic API REST** pode não estar autorizada para os dados de mapa do SDK.
2. **Internet:** teste se o emulador ou celular abre páginas online normalmente. O UrbanFlow
   usa o modo online com cache, sem mapa offline instalado.
3. **GPU do dispositivo:** o Standard renderer precisa de **OpenGL ES 3.0 ou superior**.
   No emulador, selecione uma configuração gráfica que forneça OpenGL ES 3.0+;
   na documentação da TomTom, a opção recomendada é "Renderer maximum (up to OpenGL ES 3.1)".
4. **Diagnóstico:** abra **View → Tool Windows → Logcat** no Android Studio,
   filtre por `UrbanFlowMap` e observe erros ao abrir "Mapa inteligente".
5. **Atualização:** use `git pull --ff-only origin feat/tomtom-traffic-integration`,
   sincronize o Gradle, faça **Build → Rebuild Project** e execute novamente.

### Como interpretar os resultados

- **Mensagem de falta de OpenGL:** o emulador/dispositivo precisa de configuração gráfica adequada.
- **Mensagem de chave não configurada:** verifique `UrbanFlow/local.properties` e sincronize.
- **Carregamento por mais de 20 segundos:** verifique permissões do produto,
  internet e mensagens do SDK no Logcat.
- **`onMapReady` aparece no Logcat, mas o mapa segue sem ruas:** o renderizador
  inicializou, porém ainda é necessário investigar os dados de mapa, a conexão e a
  autorização da chave. O callback não confirma que todos os dados/cartografia chegaram.

A alteração melhora compatibilidade e diagnóstico, mas **não consegue conceder**
permissões de serviço à chave nem consertar uma GPU sem suporte.

Referências oficiais:

- https://docs.tomtom.com/navigation/android/getting-started/low-end-configuration
- https://docs.tomtom.com/navigation/android/getting-started/project-setup
- https://docs.tomtom.com/navigation/android/guides/map-data/troubleshooting
- https://docs.tomtom.com/navigation/android/guides/map-display/map-display-for-compose/quickstart


## 7. Visualização urbana e controles do mapa

A tela do Mapa Inteligente passou a usar:

- Zoom inicial **15.0**, orientação norte e inclinação 0°, priorizando a leitura de ruas e bairros de Araçatuba.
- Estilo nativo **BROWSING** do TomTom Orbis Maps no início, com botão **Satélite** opcional. Não é necessário buscar um JSON customizado para a abertura inicial.
- Botões **+**, **−** e **Centro**. O zoom considera o estado atual da câmera mesmo após gestos de pinça no mapa.
- Botão para ligar ou desligar o fluxo de trânsito, preservando a camada de incidentes.
- Legenda com as cores verde, amarelo e vermelho para livre, moderado e intenso.
- Cabeçalho fixo com a cidade, link de retorno e instruções de diagnóstico.

### Desempenho e cobertura

A infraestrutura e o estado do mapa continuam sendo preservados pelo `remember` / `rememberMapViewState` durante recomposições. A troca de estilo só é acionada pelo usuário, e o SDK continua responsável por cachear os tiles online.

O primeiro carregamento ainda depende da internet, da chave e dos tiles/cartografia disponíveis. **Aumentar o zoom não cria ruas ausentes na origem dos dados.** Se a região continuar mostrando apenas uma avenida após aproximar o zoom, teste a opção Satélite e verifique se a chave tem autorização e cobertura dos dados TomTom Orbis Maps. O `onMapReady` indica que o renderizador está pronto, não que todos os tiles chegaram.

### Como atualizar no Android Studio

```bash
git fetch origin
git switch feat/tomtom-traffic-integration
git pull --ff-only origin feat/tomtom-traffic-integration
```

Depois sincronize o Gradle, execute `Build → Rebuild Project` e teste no celular/emulador. Nenhum arquivo com chave foi alterado.

Referências:
- https://docs.tomtom.com/navigation/android/guides/map-display/map-display-for-compose/map-styles
- https://docs.tomtom.com/navigation/android/guides/map-display/map-display-for-compose/camera-and-animations
- https://docs.tomtom.com/navigation/android/guides/map-data/caching-strategy
