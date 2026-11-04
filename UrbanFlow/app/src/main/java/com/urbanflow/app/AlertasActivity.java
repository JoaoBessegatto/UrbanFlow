package com.urbanflow.app;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AlertasActivity extends AppCompatActivity {

    private LinearLayout listaAlertas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alertas);

        listaAlertas = findViewById(R.id.listaAlertas);

        carregarAlertas();
    }

    private void carregarAlertas() {

        listaAlertas.removeAllViews();

        adicionarAlerta(
                "Atenção no trânsito",
                "Aqui aparecerão avisos de congestionamentos e lentidão.",
                "Trânsito",
                "●"
        );

        adicionarAlerta(
                "Ocorrências na cidade",
                "Avisos sobre acidentes, buracos e alagamentos.",
                "Ocorrências",
                "!"
        );

        adicionarAlerta(
                "Seus trajetos",
                "Aqui aparecerão alertas relacionados aos caminhos que você salvou.",
                "Meus trajetos",
                "⌖"
        );
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
}
