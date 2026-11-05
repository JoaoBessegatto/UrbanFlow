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
