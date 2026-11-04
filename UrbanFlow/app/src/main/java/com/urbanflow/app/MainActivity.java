package com.urbanflow.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.net.Uri;
import android.widget.ImageButton;
import android.graphics.drawable.GradientDrawable;

public class MainActivity extends AppCompatActivity {

    private ViewGroup telaPrincipal;

    private boolean contrasteInicializado = false;
    private boolean ultimoAltoContraste = false;

    // LEITURA POR VOZ
    private TextToSpeech leitorVoz;
    private boolean vozDisponivel = false;
    private boolean lendo = false;
    private Button btnOuvirPagina;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ImageButton btnPerfil = findViewById(R.id.btnPerfil);

        btnPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PerfilActivity.class);
            startActivity(intent);
        });

        telaPrincipal = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        // ACESSIBILIDADE
        Button btnAcessibilidade = findViewById(R.id.btnAcessibilidade);
        btnAcessibilidade.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this,
                    AcessibilidadeActivity.class);
            startActivity(intent);
        });

        // ALERTAS
        Button btnAlertas = findViewById(R.id.btnAlertas);
        btnAlertas.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this,
                    AlertasActivity.class);
            startActivity(intent);
        });

        // TRAJETOS
        Button btnTrajetos = findViewById(R.id.btnTrajetos);
        btnTrajetos.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this,
                    TrajetosActivity.class);
            startActivity(intent);
        });

        // OCORRÊNCIAS
        Button btnOcorrencias = findViewById(R.id.btnOcorrencias);
        btnOcorrencias.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this,
                    OcorrenciasActivity.class);
            startActivity(intent);
        });

        // MAPA
        Button btnMapa = findViewById(R.id.btnMapa);
        btnMapa.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this,
                    MapComposeActivity.class);
            startActivity(intent);
        });

        // BOTÃO DE LEITURA DA PÁGINA
        btnOuvirPagina = findViewById(R.id.btnOuvirPagina);

        btnOuvirPagina.setOnClickListener(v -> {

            if (!vozDisponivel) {
                return;
            }

            if (lendo) {
                pararLeitura();
            } else {
                iniciarLeitura();
            }
        });

        // INICIALIZA A VOZ
        inicializarLeitorVoz();
    }

    // INICIALIZA O TEXT TO SPEECH
    private void inicializarLeitorVoz() {

        leitorVoz = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS) {

                int resultado = leitorVoz.setLanguage(
                        new Locale("pt", "BR")
                );

                vozDisponivel =
                        resultado != TextToSpeech.LANG_MISSING_DATA &&
                                resultado != TextToSpeech.LANG_NOT_SUPPORTED;

                btnOuvirPagina.setEnabled(vozDisponivel);

            } else {
                vozDisponivel = false;
                btnOuvirPagina.setEnabled(false);
            }
        });

        leitorVoz.setOnUtteranceProgressListener(
                new UtteranceProgressListener() {

                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> {
                            lendo = false;
                            btnOuvirPagina.setText(
                                    "🔊 Ouvir leitura da página"
                            );
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> {
                            lendo = false;
                            btnOuvirPagina.setText(
                                    "🔊 Ouvir leitura da página"
                            );
                        });
                    }
                }
        );
    }

    // INICIA A LEITURA DA PÁGINA INICIAL
    private void iniciarLeitura() {

        String conteudo = obterTextosDaTela(telaPrincipal);

        if (conteudo.trim().isEmpty()) {
            return;
        }

        int resultado = leitorVoz.speak(
                conteudo,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "leituraPaginaInicial"
        );

        if (resultado == TextToSpeech.SUCCESS) {
            lendo = true;
            btnOuvirPagina.setText("⏹ Parar leitura");
        }
    }

    // REÚNE OS TEXTOS DA TELA
    private String obterTextosDaTela(ViewGroup grupo) {

        List<String> textos = new ArrayList<>();

        coletarTextos(grupo, textos);

        StringBuilder resultado = new StringBuilder();

        for (String texto : textos) {
            resultado.append(texto).append(". ");
        }

        return resultado.toString();
    }

    private void coletarTextos(
            ViewGroup grupo,
            List<String> textos
    ) {

        for (int i = 0; i < grupo.getChildCount(); i++) {

            View view = grupo.getChildAt(i);

            // IGNORA O PRÓPRIO BOTÃO DE LEITURA
            if (view.getId() == R.id.btnOuvirPagina) {
                continue;
            }

            // CAPTURA OS TEXTOS DA PÁGINA
            if (view instanceof TextView) {

                String texto = ((TextView) view)
                        .getText().toString().trim();

                if (!texto.isEmpty()) {
                    textos.add(texto);
                }
            }

            // PERCORRE OS ELEMENTOS INTERNOS
            if (view instanceof ViewGroup) {
                coletarTextos((ViewGroup) view, textos);
            }
        }
    }

    // INTERROMPE A LEITURA
    private void pararLeitura() {

        if (leitorVoz != null) {
            leitorVoz.stop();
        }

        lendo = false;

        if (btnOuvirPagina != null) {
            btnOuvirPagina.setText(
                    "🔊 Ouvir leitura da página"
            );
        }
    }

    // APLICA AS CONFIGURAÇÕES AO ABRIR A PÁGINA INICIAL
    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences preferencias = getSharedPreferences(
                "UrbanFlowAcessibilidade",
                MODE_PRIVATE
        );

        boolean altoContraste = preferencias.getBoolean(
                "altoContraste", false
        );

        int tamanhoFonte = preferencias.getInt(
                "tamanhoFonte", R.id.fonteNormal
        );

        if (contrasteInicializado &&
                ultimoAltoContraste != altoContraste) {

            ultimoAltoContraste = altoContraste;
            recreate();
            return;
        }

        ultimoAltoContraste = altoContraste;
        contrasteInicializado = true;

        aplicarAcessibilidade(altoContraste, tamanhoFonte);

        atualizarFotoPerfil();
    }

    private void atualizarFotoPerfil() {

        ImageButton btnPerfil = findViewById(R.id.btnPerfil);

        SharedPreferences preferenciasPerfil = getSharedPreferences(
                "UrbanFlowPerfil",
                MODE_PRIVATE
        );

        String fotoSalva = preferenciasPerfil.getString("fotoUri", "");

        if (!fotoSalva.isEmpty()) {

            try {
                btnPerfil.setImageURI(Uri.parse(fotoSalva));

                btnPerfil.setScaleType(ImageButton.ScaleType.CENTER_CROP);
                btnPerfil.setPadding(0, 0, 0, 0);
                btnPerfil.setImageTintList(null);

                GradientDrawable formatoCircular = new GradientDrawable();
                formatoCircular.setShape(GradientDrawable.OVAL);
                formatoCircular.setColor(0xFF45D6B0);

                btnPerfil.setBackground(formatoCircular);
                btnPerfil.setClipToOutline(true);

            } catch (Exception e) {
                btnPerfil.setImageResource(
                        android.R.drawable.ic_menu_myplaces
                );
            }

        } else {

            btnPerfil.setImageResource(
                    android.R.drawable.ic_menu_myplaces
            );
        }
    }

    // APLICA CONTRASTE E TAMANHO DA FONTE
    private void aplicarAcessibilidade(
            boolean altoContraste,
            int tamanhoFonte
    ) {

        float fator = 1.0f;

        if (tamanhoFonte == R.id.fonteGrande) {
            fator = 1.20f;
        } else if (tamanhoFonte == R.id.fonteMuitoGrande) {
            fator = 1.40f;
        }

        aplicarConfiguracoesNaTela(
                telaPrincipal,
                altoContraste,
                fator
        );
    }

    // ALTERA OS ELEMENTOS DA PÁGINA INICIAL
    private void aplicarConfiguracoesNaTela(
            ViewGroup grupo,
            boolean altoContraste,
            float fator
    ) {

        for (int i = 0; i < grupo.getChildCount(); i++) {

            View view = grupo.getChildAt(i);

            if (altoContraste) {

                if (view instanceof ViewGroup) {
                    view.setBackgroundColor(0xFF000000);
                }

                if (view instanceof Button) {
                    Button botao = (Button) view;

                    botao.setBackgroundTintList(
                            ColorStateList.valueOf(0xFFFFFF00)
                    );

                    botao.setTextColor(0xFF000000);

                } else if (view instanceof TextView) {
                    TextView texto = (TextView) view;
                    texto.setTextColor(0xFFFFFFFF);
                }
            }

            // TAMANHO DA FONTE
            if (view instanceof TextView) {

                TextView texto = (TextView) view;

                Object tamanhoOriginal = texto.getTag();

                if (tamanhoOriginal == null) {
                    tamanhoOriginal = texto.getTextSize();
                    texto.setTag(tamanhoOriginal);
                }

                float tamanho = (float) tamanhoOriginal;

                texto.setTextSize(
                        android.util.TypedValue.COMPLEX_UNIT_PX,
                        tamanho * fator
                );
            }

            // APLICA RECURSIVAMENTE
            if (view instanceof ViewGroup) {
                aplicarConfiguracoesNaTela(
                        (ViewGroup) view,
                        altoContraste,
                        fator
                );
            }
        }

        // RESTAURA O FUNDO PRINCIPAL NO MODO NORMAL
        if (grupo == telaPrincipal && !altoContraste) {
            grupo.setBackgroundColor(0xFFF3F6F8);
        }
    }

    // LIBERA OS RECURSOS DE VOZ
    @Override
    protected void onDestroy() {

        if (leitorVoz != null) {
            leitorVoz.stop();
            leitorVoz.shutdown();
        }

        super.onDestroy();
    }
}
