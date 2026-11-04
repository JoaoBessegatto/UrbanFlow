package com.urbanflow.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Typeface;
import android.content.res.ColorStateList;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OcorrenciasActivity extends AppCompatActivity {

    private Spinner spTipo;
    private EditText edtLocal;
    private EditText edtDescricao;
    private LinearLayout listaOcorrencias;

    private TextView txtTotalRegistros;
    private TextView txtTotalConfirmacoes;
    private TextView txtPontos;
    private TextView txtConquistas;

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

        preferences = getSharedPreferences(
                "UrbanFlowOcorrencias",
                MODE_PRIVATE
        );

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        spTipo = findViewById(R.id.spTipoOcorrencia);
        edtLocal = findViewById(R.id.edtLocalOcorrencia);
        edtDescricao = findViewById(R.id.edtDescricaoOcorrencia);
        listaOcorrencias = findViewById(R.id.listaOcorrencias);

        // CONTADORES E CONQUISTAS
        txtTotalRegistros = findViewById(R.id.txtTotalRegistros);
        txtTotalConfirmacoes = findViewById(R.id.txtTotalConfirmacoes);
        txtPontos = findViewById(R.id.txtPontos);
        txtConquistas = findViewById(R.id.txtConquistas);

        Button btnSalvar = findViewById(R.id.btnSalvarOcorrencia);

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

        // Atualiza os registros antigos e carrega a tela.
        migrarOcorrencias();
        carregarOcorrencias();
    }

    // MIGRA OS REGISTROS ANTIGOS SEM APAGAR INFORMAÇÕES
    private void migrarOcorrencias() {

        try {
            JSONArray ocorrencias = new JSONArray(
                    preferences.getString("lista", "[]")
            );

            Set<String> confirmadas = new HashSet<>(
                    preferences.getStringSet(
                            "confirmadasPeloDispositivo",
                            new HashSet<>()
                    )
            );

            boolean alterou = false;

            for (int i = 0; i < ocorrencias.length(); i++) {

                JSONObject item = ocorrencias.getJSONObject(i);

                // Cria um identificador para registros antigos.
                if (!item.has("id")) {
                    item.put("id", "ocorrencia_" + i);
                    alterou = true;
                }

                // Preserva confirmações feitas na versão anterior.
                if (item.optBoolean("jaConfirmou", false)) {
                    confirmadas.add(item.getString("id"));
                }
            }

            if (alterou) {
                preferences.edit()
                        .putString("lista", ocorrencias.toString())
                        .apply();
            }

            preferences.edit()
                    .putStringSet(
                            "confirmadasPeloDispositivo",
                            new HashSet<>(confirmadas)
                    )
                    .apply();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Erro ao atualizar registros antigos.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // REGISTRAR NOVA OCORRÊNCIA
    private void salvarOcorrencia() {

        String tipo = spTipo.getSelectedItem().toString();
        String local = edtLocal.getText().toString().trim();
        String descricao = edtDescricao.getText().toString().trim();

        if (spTipo.getSelectedItemPosition() == 0) {
            Toast.makeText(
                    this,
                    "Selecione o tipo de ocorrência.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (local.isEmpty() || descricao.isEmpty()) {
            Toast.makeText(
                    this,
                    "Preencha o local e a descrição.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {

            JSONArray ocorrencias = new JSONArray(
                    preferences.getString("lista", "[]")
            );

            JSONObject novaOcorrencia = new JSONObject();

            novaOcorrencia.put("id", UUID.randomUUID().toString());
            novaOcorrencia.put("tipo", tipo);
            novaOcorrencia.put("local", local);
            novaOcorrencia.put("descricao", descricao);
            novaOcorrencia.put("confirmacoes", 0);
            novaOcorrencia.put("confirmada", false);
            novaOcorrencia.put("jaConfirmou", false);

            ocorrencias.put(novaOcorrencia);

            int pontosAtuais = preferences.getInt("pontos", 0);
            int registrosUsuario = preferences.getInt(
                    "totalRegistrosUsuario", 0
            );

            preferences.edit()
                    .putString("lista", ocorrencias.toString())
                    .putInt("pontos", pontosAtuais + 10)
                    .putInt("totalRegistrosUsuario", registrosUsuario + 1)
                    .apply();

            edtLocal.setText("");
            edtDescricao.setText("");
            spTipo.setSelection(0);

            carregarOcorrencias();

            Toast.makeText(
                    this,
                    "Ocorrência registrada! Você ganhou 10 pontos.",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Não foi possível salvar a ocorrência.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // CONFIRMAR UMA OCORRÊNCIA
    private void confirmarOcorrencia(int indice) {

        try {

            JSONArray ocorrencias = new JSONArray(
                    preferences.getString("lista", "[]")
            );

            if (indice < 0 || indice >= ocorrencias.length()) {
                return;
            }

            JSONObject item = ocorrencias.getJSONObject(indice);

            String id = item.getString("id");

            Set<String> confirmadas = new HashSet<>(
                    preferences.getStringSet(
                            "confirmadasPeloDispositivo",
                            new HashSet<>()
                    )
            );

            if (confirmadas.contains(id)) {
                Toast.makeText(
                        this,
                        "Você já confirmou esta ocorrência.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            int confirmacoes = item.optInt("confirmacoes", 0);
            confirmacoes++;

            item.put("confirmacoes", confirmacoes);
            item.put("jaConfirmou", true);

            // A ocorrência precisa de 3 confirmações.
            item.put("confirmada", confirmacoes >= 3);

            confirmadas.add(id);

            int pontosAtuais = preferences.getInt("pontos", 0);
            int confirmacoesUsuario = preferences.getInt(
                    "totalConfirmacoesUsuario", 0
            );

            preferences.edit()
                    .putString("lista", ocorrencias.toString())
                    .putStringSet(
                            "confirmadasPeloDispositivo",
                            new HashSet<>(confirmadas)
                    )
                    .putInt("pontos", pontosAtuais + 5)
                    .putInt(
                            "totalConfirmacoesUsuario",
                            confirmacoesUsuario + 1
                    )
                    .apply();

            carregarOcorrencias();

            Toast.makeText(
                    this,
                    "Confirmação registrada! Você ganhou 5 pontos.",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Não foi possível confirmar a ocorrência.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // CARREGAR E EXIBIR AS OCORRÊNCIAS
    private void carregarOcorrencias() {

        listaOcorrencias.removeAllViews();

        try {

            JSONArray ocorrencias = new JSONArray(
                    preferences.getString("lista", "[]")
            );

            int totalConfirmacoes = 0;

            for (int i = ocorrencias.length() - 1; i >= 0; i--) {

                JSONObject item = ocorrencias.getJSONObject(i);

                String tipo = item.optString("tipo", "Ocorrência");
                String local = item.optString("local", "");
                String descricao = item.optString("descricao", "");

                int confirmacoes = item.optInt("confirmacoes", 0);
                boolean confirmada = item.optBoolean(
                        "confirmada", false
                );

                totalConfirmacoes += confirmacoes;

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(24, 24, 24, 24);
                card.setBackgroundResource(R.drawable.bg_dashboard_card);

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );

                params.bottomMargin = 16;
                card.setLayoutParams(params);

                TextView titulo = new TextView(this);
                titulo.setText(tipo);
                titulo.setTextColor(0xFF102B46);
                titulo.setTextSize(18);
                titulo.setTypeface(null, Typeface.BOLD);

                TextView detalhes = new TextView(this);
                detalhes.setText(
                        "Local: " + local + "\n\n" +
                                "Descrição: " + descricao
                );
                detalhes.setTextColor(0xFF627D98);
                detalhes.setTextSize(14);
                detalhes.setPadding(0, 12, 0, 12);

                TextView status = new TextView(this);
                status.setText(
                        confirmada
                                ? "✓ Confirmada pela comunidade"
                                : "◷ Aguardando confirmações"
                );

                status.setTextColor(
                        confirmada ? 0xFF167D72 : 0xFFB7791F
                );

                status.setTextSize(13);
                status.setTypeface(null, Typeface.BOLD);

                TextView contador = new TextView(this);
                contador.setText(
                        "👥 " + confirmacoes + " confirmação(ões)"
                );
                contador.setTextColor(0xFF102B46);
                contador.setTextSize(13);
                contador.setPadding(0, 10, 0, 10);

                Button btnConfirmar = new Button(this);
                btnConfirmar.setText("✓ Confirmar problema");
                btnConfirmar.setAllCaps(false);
                btnConfirmar.setTextColor(0xFFFFFFFF);
                btnConfirmar.setBackgroundTintList(
                        ColorStateList.valueOf(0xFF167D72)
                );

                final int indice = i;

                btnConfirmar.setOnClickListener(v ->
                        confirmarOcorrencia(indice)
                );

                card.addView(titulo);
                card.addView(detalhes);
                card.addView(status);
                card.addView(contador);
                card.addView(btnConfirmar);

                listaOcorrencias.addView(card);
            }

            if (ocorrencias.length() == 0) {

                TextView vazio = new TextView(this);
                vazio.setText(
                        "Nenhuma ocorrência registrada ainda."
                );
                vazio.setTextColor(0xFF627D98);
                vazio.setTextSize(14);

                listaOcorrencias.addView(vazio);
            }

            // ATUALIZAR CONTADORES
            txtTotalRegistros.setText(
                    String.valueOf(ocorrencias.length())
            );

            txtTotalConfirmacoes.setText(
                    String.valueOf(totalConfirmacoes)
            );

            atualizarConquistas();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erro ao carregar ocorrências.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ATUALIZAR PONTOS E CONQUISTAS
    private void atualizarConquistas() {

        int pontos = preferences.getInt("pontos", 0);

        int registros = preferences.getInt(
                "totalRegistrosUsuario", 0
        );

        int confirmacoes = preferences.getInt(
                "totalConfirmacoesUsuario", 0
        );

        int participacoes = registros + confirmacoes;

        txtPontos.setText(pontos + " pontos");

        StringBuilder conquistas = new StringBuilder();

        if (registros >= 1) {
            conquistas.append("🥉 Cidadão Participativo ✓");
        } else {
            conquistas.append("🔒 Cidadão Participativo");
        }

        conquistas.append("\n");

        if (confirmacoes >= 5) {
            conquistas.append("🥈 Olho Vivo ✓");
        } else {
            conquistas.append("🔒 Olho Vivo");
        }

        conquistas.append("\n");

        if (registros >= 5) {
            conquistas.append("🥇 Agente da Mudança ✓");
        } else {
            conquistas.append("🔒 Agente da Mudança");
        }

        conquistas.append("\n");

        if (participacoes >= 10) {
            conquistas.append("🏆 Guardião da Cidade ✓");
        } else {
            conquistas.append("🔒 Guardião da Cidade");
        }

        txtConquistas.setText(conquistas.toString());
    }
}
