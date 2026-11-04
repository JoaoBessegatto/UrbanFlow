package com.urbanflow.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

public class OcorrenciasActivity extends AppCompatActivity {

    private Spinner spTipo;
    private EditText edtLocal;
    private EditText edtDescricao;
    private LinearLayout listaOcorrencias;

    private SharedPreferences preferences;

    private final String[] tipos = {
            "Selecione uma ocorrência",
            "Acidente",
            "Buraco na via",
            "Alagamento",
            "Interdição",
            "Semáforo com defeito",
            "Outros"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocorrencias);

        spTipo = findViewById(R.id.spTipoOcorrencia);
        edtLocal = findViewById(R.id.edtLocalOcorrencia);
        edtDescricao = findViewById(R.id.edtDescricaoOcorrencia);
        listaOcorrencias = findViewById(R.id.listaOcorrencias);

        Button btnSalvar = findViewById(R.id.btnSalvarOcorrencia);

        preferences = getSharedPreferences("UrbanFlowOcorrencias", MODE_PRIVATE);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                tipos
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spTipo.setAdapter(adapter);

        btnSalvar.setOnClickListener(v -> salvarOcorrencia());

        carregarOcorrencias();
    }

    private void salvarOcorrencia() {

        String tipo = spTipo.getSelectedItem().toString();
        String local = edtLocal.getText().toString().trim();
        String descricao = edtDescricao.getText().toString().trim();

        if (spTipo.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Selecione o tipo de ocorrência.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (local.isEmpty() || descricao.isEmpty()) {
            Toast.makeText(this, "Preencha o local e a descrição.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String dadosSalvos = preferences.getString("lista", "[]");
            JSONArray ocorrencias = new JSONArray(dadosSalvos);

            JSONObject novaOcorrencia = new JSONObject();
            novaOcorrencia.put("tipo", tipo);
            novaOcorrencia.put("local", local);
            novaOcorrencia.put("descricao", descricao);

            ocorrencias.put(novaOcorrencia);

            preferences.edit()
                    .putString("lista", ocorrencias.toString())
                    .apply();

            edtLocal.setText("");
            edtDescricao.setText("");
            spTipo.setSelection(0);

            carregarOcorrencias();

            Toast.makeText(
                    this,
                    "Ocorrência salva com sucesso!",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível salvar.", Toast.LENGTH_SHORT).show();
        }
    }

    private void carregarOcorrencias() {

        listaOcorrencias.removeAllViews();

        try {
            String dadosSalvos = preferences.getString("lista", "[]");
            JSONArray ocorrencias = new JSONArray(dadosSalvos);

            for (int i = ocorrencias.length() - 1; i >= 0; i--) {

                JSONObject item = ocorrencias.getJSONObject(i);

                String tipo = item.getString("tipo");
                String local = item.getString("local");
                String descricao = item.getString("descricao");

                TextView card = new TextView(this);

                card.setText(
                        tipo + "\n" +
                                "Local: " + local + "\n" +
                                "Descrição: " + descricao
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

                listaOcorrencias.addView(card);
            }

            if (ocorrencias.length() == 0) {
                TextView vazio = new TextView(this);
                vazio.setText("Nenhuma ocorrência registrada ainda.");
                vazio.setTextColor(0xFF627D98);
                vazio.setTextSize(14);
                listaOcorrencias.addView(vazio);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao carregar ocorrências.", Toast.LENGTH_SHORT).show();
        }
    }
}
