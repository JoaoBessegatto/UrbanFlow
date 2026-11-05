package com.urbanflow.app.data;

import android.os.Handler;
import android.os.Looper;
import android.net.Uri;

import com.urbanflow.app.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cliente leve para os endpoints REST da TomTom Traffic API usados pelo UrbanFlow.
 *
 * A chave é injetada via BuildConfig a partir de local.properties ou da variável
 * de ambiente TOMTOM_API_KEY. Ela não deve ser versionada no repositório.
 */
public class TomTomTrafficService {

    private static final String FLOW_URL =
            "https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json";
    private static final String INCIDENTS_URL =
            "https://api.tomtom.com/traffic/services/5/incidentDetails";

    // Centro de Araçatuba - SP.
    public static final double ARACATUBA_LAT = -21.2089;
    public static final double ARACATUBA_LON = -50.4328;

    // Caixa urbana aproximada usada para consultar incidentes ao redor da cidade.
    private static final double MIN_LAT = -21.32;
    private static final double MIN_LON = -50.55;
    private static final double MAX_LAT = -21.10;
    private static final double MAX_LON = -50.31;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    public static class TrafficStatus {
        public final int currentSpeed;
        public final int freeFlowSpeed;
        public final double currentTravelTime;
        public final double freeFlowTravelTime;
        public final double confidence;
        public final boolean roadClosure;

        public TrafficStatus(
                int currentSpeed,
                int freeFlowSpeed,
                double currentTravelTime,
                double freeFlowTravelTime,
                double confidence,
                boolean roadClosure
        ) {
            this.currentSpeed = currentSpeed;
            this.freeFlowSpeed = freeFlowSpeed;
            this.currentTravelTime = currentTravelTime;
            this.freeFlowTravelTime = freeFlowTravelTime;
            this.confidence = confidence;
            this.roadClosure = roadClosure;
        }

        public double speedRatio() {
            if (freeFlowSpeed <= 0) return 1.0;
            return (double) currentSpeed / (double) freeFlowSpeed;
        }

        public String levelLabel() {
            if (roadClosure) return "Via interditada";
            double ratio = speedRatio();
            if (ratio >= 0.80) return "Trânsito livre";
            if (ratio >= 0.50) return "Trânsito moderado";
            return "Trânsito intenso";
        }

        public String summary() {
            if (roadClosure) {
                return "Via interditada na região monitorada";
            }
            return String.format(
                    Locale.getDefault(),
                    "%s • %d km/h (fluxo livre: %d km/h)",
                    levelLabel(),
                    currentSpeed,
                    freeFlowSpeed
            );
        }
    }

    public static class TrafficIncident {
        public final String id;
        public final String title;
        public final String description;
        public final String category;
        public final int magnitudeOfDelay;
        public final String from;
        public final String to;
        public final int delaySeconds;
        public final int lengthMeters;
        public final double latitude;
        public final double longitude;

        public TrafficIncident(
                String id,
                String title,
                String description,
                String category,
                int magnitudeOfDelay,
                String from,
                String to,
                int delaySeconds,
                int lengthMeters,
                double latitude,
                double longitude
        ) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.category = category;
            this.magnitudeOfDelay = magnitudeOfDelay;
            this.from = from;
            this.to = to;
            this.delaySeconds = delaySeconds;
            this.lengthMeters = lengthMeters;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String locationText() {
            if (!from.isEmpty() && !to.isEmpty()) {
                return from + " → " + to;
            }
            if (!from.isEmpty()) return from;
            if (!to.isEmpty()) return to;
            return "Região de Araçatuba";
        }

        public String detailsText() {
            StringBuilder text = new StringBuilder(description);
            String location = locationText();

            if (!location.isEmpty()) {
                text.append("\n").append(location);
            }

            if (delaySeconds > 0) {
                int minutes = Math.max(1, Math.round(delaySeconds / 60f));
                text.append("\nAtraso estimado: ").append(minutes).append(" min");
            }

            if (lengthMeters > 0) {
                if (lengthMeters >= 1000) {
                    text.append(String.format(
                            Locale.getDefault(),
                            "\nExtensão: %.1f km",
                            lengthMeters / 1000.0
                    ));
                } else {
                    text.append("\nExtensão: ").append(lengthMeters).append(" m");
                }
            }

            return text.toString();
        }
    }

    public void fetchTrafficStatus(Callback<TrafficStatus> callback) {
        executor.execute(() -> {
            try {
                ensureApiKey();

                String point = ARACATUBA_LAT + "," + ARACATUBA_LON;
                String url = FLOW_URL
                        + "?point=" + encode(point)
                        + "&unit=KMPH"
                        + "&key=" + encode(BuildConfig.TOMTOM_API_KEY);

                JSONObject root = requestJson(url);
                JSONObject data = root.getJSONObject("flowSegmentData");

                TrafficStatus status = new TrafficStatus(
                        data.optInt("currentSpeed", 0),
                        data.optInt("freeFlowSpeed", 0),
                        data.optDouble("currentTravelTime", 0),
                        data.optDouble("freeFlowTravelTime", 0),
                        data.optDouble("confidence", 0),
                        data.optBoolean("roadClosure", false)
                );

                mainHandler.post(() -> callback.onSuccess(status));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void fetchIncidents(Callback<List<TrafficIncident>> callback) {
        executor.execute(() -> {
            try {
                ensureApiKey();

                String bbox = MIN_LON + "," + MIN_LAT + "," + MAX_LON + "," + MAX_LAT;
                String fields =
                        "{incidents{type,geometry{type,coordinates},properties{"
                                + "id,iconCategory,magnitudeOfDelay,events{description,code},"
                                + "startTime,endTime,from,to,length,delay,roadNumbers,"
                                + "timeValidity,probabilityOfOccurrence,numberOfReports,lastReportTime"
                                + "}}}";

                String url = INCIDENTS_URL
                        + "?bbox=" + encode(bbox)
                        + "&fields=" + encode(fields)
                        + "&language=pt-BR"
                        + "&timeValidityFilter=present"
                        + "&key=" + encode(BuildConfig.TOMTOM_API_KEY);

                JSONObject root = requestJson(url);
                JSONArray incidentsJson = root.optJSONArray("incidents");

                if (incidentsJson == null || incidentsJson.length() == 0) {
                    mainHandler.post(() -> callback.onSuccess(Collections.emptyList()));
                    return;
                }

                List<TrafficIncident> incidents = new ArrayList<>();

                for (int i = 0; i < incidentsJson.length(); i++) {
                    JSONObject incident = incidentsJson.optJSONObject(i);
                    if (incident == null) continue;

                    JSONObject properties = incident.optJSONObject("properties");
                    JSONObject geometry = incident.optJSONObject("geometry");

                    if (properties == null) continue;

                    int iconCategory = properties.optInt("iconCategory", 0);
                    String category = categoryLabel(iconCategory);
                    String description = extractDescription(properties, category);
                    double[] coordinates = extractCoordinates(geometry);

                    TrafficIncident parsed = new TrafficIncident(
                            properties.optString("id", "tomtom-" + i),
                            category,
                            description,
                            category,
                            properties.optInt("magnitudeOfDelay", 0),
                            properties.optString("from", ""),
                            properties.optString("to", ""),
                            properties.optInt("delay", 0),
                            properties.optInt("length", 0),
                            coordinates[1],
                            coordinates[0]
                    );

                    incidents.add(parsed);
                }

                incidents.sort((a, b) ->
                        Integer.compare(b.magnitudeOfDelay, a.magnitudeOfDelay)
                );

                mainHandler.post(() -> callback.onSuccess(incidents));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private static void ensureApiKey() {
        if (BuildConfig.TOMTOM_API_KEY == null
                || BuildConfig.TOMTOM_API_KEY.trim().isEmpty()) {
            throw new IllegalStateException(
                    "TomTom API Key não configurada. Defina tomtomApiKey em local.properties "
                            + "ou TOMTOM_API_KEY no ambiente."
            );
        }
    }

    private static JSONObject requestJson(String urlString) throws Exception {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String body = readAll(stream);

            if (status < 200 || status >= 300) {
                throw new IllegalStateException(
                        "TomTom Traffic API retornou HTTP " + status
                );
            }

            return new JSONObject(body);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";

        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }

    private static String extractDescription(
            JSONObject properties,
            String fallback
    ) {
        JSONArray events = properties.optJSONArray("events");

        if (events != null) {
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.optJSONObject(i);
                if (event == null) continue;

                String description = event.optString("description", "").trim();
                if (!description.isEmpty()) {
                    return description;
                }
            }
        }

        return fallback;
    }

    private static double[] extractCoordinates(JSONObject geometry) {
        if (geometry == null) return new double[]{0, 0};

        JSONArray coordinates = geometry.optJSONArray("coordinates");
        if (coordinates == null || coordinates.length() == 0) {
            return new double[]{0, 0};
        }

        Object first = coordinates.opt(0);

        if (first instanceof Number && coordinates.length() >= 2) {
            return new double[]{
                    coordinates.optDouble(0, 0),
                    coordinates.optDouble(1, 0)
            };
        }

        if (first instanceof JSONArray) {
            JSONArray nested = (JSONArray) first;

            while (nested.length() > 0 && nested.opt(0) instanceof JSONArray) {
                nested = nested.optJSONArray(0);
                if (nested == null) break;
            }

            if (nested != null && nested.length() >= 2) {
                return new double[]{
                        nested.optDouble(0, 0),
                        nested.optDouble(1, 0)
                };
            }
        }

        return new double[]{0, 0};
    }

    private static String categoryLabel(int iconCategory) {
        switch (iconCategory) {
            case 1:
                return "Acidente";
            case 2:
                return "Neblina";
            case 3:
                return "Condição perigosa";
            case 4:
                return "Chuva";
            case 5:
                return "Pista escorregadia";
            case 6:
                return "Congestionamento";
            case 7:
                return "Faixa interditada";
            case 8:
                return "Via interditada";
            case 9:
                return "Obras";
            case 10:
                return "Vento forte";
            case 11:
                return "Alagamento";
            case 14:
                return "Veículo parado";
            default:
                return "Trânsito";
        }
    }

    private static String encode(String value) {
        return Uri.encode(value);
    }
}
