
package com.urbanflow.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsuario, edtSenha;
    private Button btnEntrar, btnCadastrar;

    private SharedPreferences preferences;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        preferences = getSharedPreferences(
                "UrbanFlowUsers", MODE_PRIVATE);

        String usuarioLogado = preferences.getString(
                "usuario_logado", null);

        if (usuarioLogado != null) {
            abrirTelaPrincipal();
            return;
        }

        edtUsuario = findViewById(R.id.edtUsuario);
        edtSenha = findViewById(R.id.edtSenha);

        btnEntrar = findViewById(R.id.btnEntrar);
        btnCadastrar = findViewById(R.id.btnCadastrar);

        btnCadastrar.setOnClickListener(v -> cadastrarUsuario());

        btnEntrar.setOnClickListener(v -> fazerLogin());
    }

    private void cadastrarUsuario() {

        String usuario = edtUsuario.getText().toString().trim();
        String senha = edtSenha.getText().toString();

        if (usuario.length() < 3 || senha.length() < 4) {
            Toast.makeText(this,
                    "Use pelo menos 3 caracteres no usuário e 4 na senha.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String chave = usuario.toLowerCase();

        String saltKey = "salt_" + chave;
        String hashKey = "hash_" + chave;

        if (preferences.contains(hashKey)) {
            Toast.makeText(this,
                    "Este usuário já está cadastrado.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            String saltBase64 = Base64.encodeToString(
                    salt, Base64.NO_WRAP);

            String senhaHash = gerarHash(senha, salt);

            preferences.edit()
                    .putString(saltKey, saltBase64)
                    .putString(hashKey, senhaHash)
                    .putString("usuario_logado", usuario)
                    .apply();

            Toast.makeText(this,
                    "Cadastro realizado com sucesso!",
                    Toast.LENGTH_SHORT).show();

            abrirTelaPrincipal();

        } catch (Exception e) {
            Toast.makeText(this,
                    "Erro ao realizar cadastro.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void fazerLogin() {

        String usuario = edtUsuario.getText().toString().trim();
        String senha = edtSenha.getText().toString();

        String chave = usuario.toLowerCase();

        String saltBase64 = preferences.getString(
                "salt_" + chave, null);

        String hashSalvo = preferences.getString(
                "hash_" + chave, null);

        if (saltBase64 == null || hashSalvo == null) {
            Toast.makeText(this,
                    "Usuário não cadastrado.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            byte[] salt = Base64.decode(
                    saltBase64, Base64.NO_WRAP);

            String senhaDigitada = gerarHash(senha, salt);

            if (MessageDigest.isEqual(
                    senhaDigitada.getBytes(StandardCharsets.UTF_8),
                    hashSalvo.getBytes(StandardCharsets.UTF_8))) {

                preferences.edit()
                        .putString("usuario_logado", usuario)
                        .apply();

                abrirTelaPrincipal();

            } else {
                Toast.makeText(this,
                        "Senha incorreta.",
                        Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this,
                    "Erro ao verificar senha.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String gerarHash(String senha, byte[] salt)
            throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        digest.update(salt);

        byte[] resultado = digest.digest(
                senha.getBytes(StandardCharsets.UTF_8));

        return Base64.encodeToString(
                resultado, Base64.NO_WRAP);
    }

    private void abrirTelaPrincipal() {

        Intent intent = new Intent(
                LoginActivity.this, MainActivity.class);

        startActivity(intent);
        finish();
    }
}
