package com.urbanflow.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PerfilActivity extends AppCompatActivity {

    private ImageView imgFotoPerfil;
    private EditText edtNomePerfil;
    private TextView txtPontosPerfil;
    private TextView txtDataInicioPerfil;

    private SharedPreferences preferenciasPerfil;
    private SharedPreferences preferenciasUsuarios;

    private final ActivityResultLauncher<String[]> selecionarFoto =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) {
                            try {
                                getContentResolver().takePersistableUriPermission(
                                        uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                );

                                preferenciasPerfil.edit()
                                        .putString("fotoUri", uri.toString())
                                        .apply();

                                imgFotoPerfil.setImageURI(uri);

                            } catch (Exception e) {
                                Toast.makeText(
                                        this,
                                        "Não foi possível carregar a foto.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        imgFotoPerfil = findViewById(R.id.imgFotoPerfil);
        edtNomePerfil = findViewById(R.id.edtNomePerfil);
        txtPontosPerfil = findViewById(R.id.txtPontosPerfil);
        txtDataInicioPerfil = findViewById(R.id.txtDataInicioPerfil);
        Button btnSairConta = findViewById(R.id.btnSairConta);
        Button btnAlterarFoto = findViewById(R.id.btnAlterarFoto);
        Button btnSalvarPerfil = findViewById(R.id.btnSalvarPerfil);
        ImageButton btnVoltarPerfil = findViewById(R.id.btnVoltarPerfil);

        btnSairConta.setOnClickListener(v -> {

            new AlertDialog.Builder(PerfilActivity.this)
                    .setTitle("Sair da conta")
                    .setMessage("Deseja realmente sair da sua conta?")
                    .setNegativeButton("Cancelar", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setPositiveButton("Sair", (dialog, which) -> {

                        // Encerra somente a sessão atual
                        preferenciasUsuarios.edit()
                                .remove("usuario_logado")
                                .apply();

                        // Volta para a tela de login
                        Intent intent = new Intent(
                                PerfilActivity.this,
                                LoginActivity.class
                        );

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();
                    })
                    .show();
        });

        preferenciasPerfil = getSharedPreferences(
                "UrbanFlowPerfil",
                MODE_PRIVATE
        );

        preferenciasUsuarios = getSharedPreferences(
                "UrbanFlowUsers",
                MODE_PRIVATE
        );

        // NOME DO USUÁRIO CADASTRADO OU SALVO NO PERFIL
        String nomeLogin = preferenciasUsuarios.getString(
                "usuario_logado",
                ""
        );

        String nomeSalvo = preferenciasPerfil.getString(
                "nomeUsuario",
                ""
        );

        if (!nomeSalvo.isEmpty()) {
            edtNomePerfil.setText(nomeSalvo);
        } else {
            edtNomePerfil.setText(nomeLogin);
        }

        // REGISTRA A DATA DO PRIMEIRO ACESSO AO PERFIL
        if (!preferenciasPerfil.contains("dataInicio")) {
            preferenciasPerfil.edit()
                    .putLong("dataInicio", System.currentTimeMillis())
                    .apply();
        }

        // BOTÃO VOLTAR
        btnVoltarPerfil.setOnClickListener(v -> finish());

        // TAMBÉM PERMITE VOLTAR PELO BOTÃO DO CELULAR
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finish();
                    }
                }
        );

        // ABRIR GALERIA
        btnAlterarFoto.setOnClickListener(v ->
                selecionarFoto.launch(new String[]{"image/*"})
        );

        // SALVAR NOME DO PERFIL
        btnSalvarPerfil.setOnClickListener(v -> {

            String novoNome = edtNomePerfil.getText()
                    .toString()
                    .trim();

            if (novoNome.isEmpty()) {
                edtNomePerfil.setError("Digite seu nome");
                return;
            }

            preferenciasPerfil.edit()
                    .putString("nomeUsuario", novoNome)
                    .apply();

            Toast.makeText(
                    this,
                    "Perfil salvo com sucesso!",
                    Toast.LENGTH_SHORT
            ).show();
        });

        carregarFoto();
        carregarDataInicio();
        atualizarPontos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarPontos();
    }

    // CARREGAR FOTO SALVA
    private void carregarFoto() {

        String fotoSalva = preferenciasPerfil.getString(
                "fotoUri",
                ""
        );

        if (!fotoSalva.isEmpty()) {
            try {
                imgFotoPerfil.setImageURI(Uri.parse(fotoSalva));
            } catch (Exception e) {
                imgFotoPerfil.setImageResource(
                        android.R.drawable.ic_menu_myplaces
                );
            }
        }
    }

    // MOSTRAR DATA DE INÍCIO
    private void carregarDataInicio() {

        long data = preferenciasPerfil.getLong(
                "dataInicio",
                System.currentTimeMillis()
        );

        SimpleDateFormat formato = new SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
        );

        txtDataInicioPerfil.setText(
                formato.format(new Date(data))
        );
    }

    // ATUALIZAR PONTUAÇÃO
    private void atualizarPontos() {

        SharedPreferences preferenciasOcorrencias =
                getSharedPreferences(
                        "UrbanFlowOcorrencias",
                        MODE_PRIVATE
                );

        int pontos = preferenciasOcorrencias.getInt(
                "pontos",
                0
        );

        txtPontosPerfil.setText(pontos + " pontos");
    }
}
