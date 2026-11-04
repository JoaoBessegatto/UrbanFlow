package com.urbanflow.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

public class TrajetosActivity extends AppCompatActivity {

    private EditText edtOrigem;
    private EditText edtDestino;
    private LinearLayout listaTrajetos;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trajetos);

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);

        btnVoltar.setOnClickListener(v -> {
            finish();
        });

        edtOrigem = findViewById(R.id.edtOrigem);
        edtDestino = findViewById(R.id.edtDestino);
        listaTrajetos = findViewById(R.id.listaTrajetos);

        Button btnSalvar = findViewById(R.id.btnSalvarTrajeto);

        preferences = getSharedPreferences("UrbanFlowTrajetos", MODE_PRIVATE);

        btnSalvar.setOnClickListener(v -> salvarTrajeto());

        carregarTrajetos();
    }

    private void salvarTrajeto() {

        String origem = edtOrigem.getText().toString().trim();
        String destino = edtDestino.getText().toString().trim();

        if (origem.isEmpty() || destino.isEmpty()) {
            Toast.makeText(this, "Informe a origem e o destino.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String dados = preferences.getString("lista", "[]");
            JSONArray trajetos = new JSONArray(dados);

            JSONObject novoTrajeto = new JSONObject();
            novoTrajeto.put("origem", origem);
            novoTrajeto.put("destino", destino);

            trajetos.put(novoTrajeto);

            preferences.edit()
                    .putString("lista", trajetos.toString())
                    .apply();

            edtOrigem.setText("");
            edtDestino.setText("");

            carregarTrajetos();

            Toast.makeText(this, "Trajeto salvo com sucesso!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível salvar o trajeto.", Toast.LENGTH_SHORT).show();
        }
    }

    private void carregarTrajetos() {

        listaTrajetos.removeAllViews();

        try {
            JSONArray trajetos = new JSONArray(
                    preferences.getString("lista", "[]")
            );

            for (int i = trajetos.length() - 1; i >= 0; i--) {

                JSONObject item = trajetos.getJSONObject(i);

                TextView card = new TextView(this);
                card.setText(
                        "Origem: " + item.getString("origem") +
                                "\nDestino: " + item.getString("destino")
                );

                card.setTextColor(0xFF102B46);
                card.setTextSize(15);
                card.setPadding(24, 24, 24, 24);
                card.setBackgroundResource(R.drawable.bg_dashboard_card);

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );

                params.bottomMargin = 16;
                card.setLayoutParams(params);

                listaTrajetos.addView(card);
            }

            if (trajetos.length() == 0) {
                TextView vazio = new TextView(this);
                vazio.setText("Você ainda não cadastrou trajetos.");
                vazio.setTextColor(0xFF627D98);
                listaTrajetos.addView(vazio);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao carregar trajetos.", Toast.LENGTH_SHORT).show();
        }
    }
}
