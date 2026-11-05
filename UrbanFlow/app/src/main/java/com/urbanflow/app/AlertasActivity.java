package com.urbanflow.app;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.urbanflow.app.data.TomTomTrafficService;

import java.util.List;

public class AlertasActivity extends AppCompatActivity {

    private LinearLayout listaAlertas;
    private TomTomTrafficService trafficService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alertas);

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);

        btnVoltar.setOnClickListener(v -> {
            finish();
        });

        listaAlertas = findViewById(R.id.listaAlertas);
        trafficService = new TomTomTrafficService();

        carregarAlertas();
    }

    private void carregarAlertas() {

        listaAlertas.removeAllViews();

        adicionarAlerta(
                "Atualizando informações",
                "Consultando fluxo e incidentes da TomTom para Araçatuba...",
                "Trânsito",
                "●"
        );

        trafficService.fetchTrafficStatus(
                new TomTomTrafficService.Callback<TomTomTrafficService.TrafficStatus>() {
                    @Override
                    public void onSuccess(TomTomTrafficService.TrafficStatus status) {
                        carregarIncidentes(status);
                    }

                    @Override
                    public void onError(Exception error) {
                        carregarIncidentes(null);
                    }
                }
        );
    }

    private void carregarIncidentes(TomTomTrafficService.TrafficStatus status) {
        trafficService.fetchIncidents(
                new TomTomTrafficService.Callback<List<TomTomTrafficService.TrafficIncident>>() {
                    @Override
                    public void onSuccess(List<TomTomTrafficService.TrafficIncident> incidents) {
                        renderizarAlertas(status, incidents);
                    }

                    @Override
                    public void onError(Exception error) {
                        renderizarAlertas(status, null);
                    }
                }
        );
    }

    private void renderizarAlertas(
            TomTomTrafficService.TrafficStatus status,
            List<TomTomTrafficService.TrafficIncident> incidents
    ) {
        listaAlertas.removeAllViews();

        if (status != null) {
            adicionarAlerta(
                    status.levelLabel(),
                    status.summary(),
                    "Fluxo em tempo real",
                    "●"
            );
        }

        if (incidents != null && !incidents.isEmpty()) {
            int limite = Math.min(incidents.size(), 8);

            for (int i = 0; i < limite; i++) {
                TomTomTrafficService.TrafficIncident incident = incidents.get(i);

                adicionarAlerta(
                        incident.title,
                        incident.detailsText(),
                        incident.category,
                        simboloParaCategoria(incident.category)
                );
            }
        }

        if (status == null && (incidents == null || incidents.isEmpty())) {
            adicionarAlerta(
                    "Não foi possível atualizar agora",
                    "Verifique sua conexão e a configuração da chave TomTom. "
                            + "Os dados locais do UrbanFlow continuam disponíveis.",
                    "Trânsito",
                    "!"
            );
            return;
        }

        if (incidents != null && incidents.isEmpty()) {
            adicionarAlerta(
                    "Nenhum incidente ativo encontrado",
                    "A TomTom não retornou acidentes, bloqueios ou congestionamentos "
                            + "ativos na área monitorada de Araçatuba.",
                    "Incidentes",
                    "✓"
            );
        }
    }

    private String simboloParaCategoria(String categoria) {
        String valor = categoria.toLowerCase();

        if (valor.contains("acidente")
                || valor.contains("interdit")
                || valor.contains("perig")) {
            return "!";
        }

        if (valor.contains("congestion")) {
            return "●";
        }

        if (valor.contains("obra")) {
            return "◆";
        }

        return "⌖";
    }

    private void adicionarAlerta(
            String titulo,
            String descricao,
            String categoria,
            String simbolo) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 24, 24, 24);
        card.setBackgroundResource(R.drawable.bg_dashboard_card);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.bottomMargin = 18;
        card.setLayoutParams(params);

        TextView tipo = new TextView(this);
        tipo.setText(simbolo + "  " + categoria.toUpperCase());
        tipo.setTextColor(0xFF167D72);
        tipo.setTextSize(12);
        tipo.setTypeface(null, 1);

        TextView nome = new TextView(this);
        nome.setText(titulo);
        nome.setTextColor(0xFF102B46);
        nome.setTextSize(18);
        nome.setTypeface(null, 1);
        nome.setPadding(0, 12, 0, 8);

        TextView texto = new TextView(this);
        texto.setText(descricao);
        texto.setTextColor(0xFF627D98);
        texto.setTextSize(14);

        card.addView(tipo);
        card.addView(nome);
        card.addView(texto);

        listaAlertas.addView(card);
    }
    @Override
    protected void onDestroy() {
        if (trafficService != null) {
            trafficService.shutdown();
        }
        super.onDestroy();
    }
}
