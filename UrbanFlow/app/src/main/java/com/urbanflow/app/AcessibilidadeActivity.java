package com.urbanflow.app;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AcessibilidadeActivity extends AppCompatActivity {

    private TextToSpeech leitorVoz;
    private boolean vozDisponivel = false;
    private boolean lendo = false;

    private SharedPreferences preferencias;

    private ScrollView tela;
    private Switch switchAltoContraste;
    private RadioGroup grupoFonte;
    private Button btnOuvir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acessibilidade);

        preferencias = getSharedPreferences(
                "UrbanFlowAcessibilidade",
                MODE_PRIVATE
        );

        tela = findViewById(R.id.main);

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        switchAltoContraste = findViewById(R.id.switchAltoContraste);
        grupoFonte = findViewById(R.id.grupoFonte);
        btnOuvir = findViewById(R.id.btnOuvir);

        // VOLTAR
        btnVoltar.setOnClickListener(v -> {
            pararLeitura();
            finish();
        });

        // ALTO CONTRASTE
        boolean contrasteSalvo = preferencias.getBoolean(
                "altoContraste", false
        );

        switchAltoContraste.setChecked(contrasteSalvo);
        aplicarAltoContraste(contrasteSalvo);

        switchAltoContraste.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    preferencias.edit()
                            .putBoolean("altoContraste", isChecked)
                            .apply();

                    aplicarAltoContraste(isChecked);
                }
        );

        // TAMANHO DA FONTE
        int fonteSalva = preferencias.getInt(
                "tamanhoFonte", R.id.fonteNormal
        );

        grupoFonte.check(fonteSalva);
        aplicarTamanhoFonte(fonteSalva);

        grupoFonte.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {

                preferencias.edit()
                        .putInt("tamanhoFonte", checkedId)
                        .apply();

                aplicarTamanhoFonte(checkedId);
            }
        });

        // LEITURA POR VOZ
        leitorVoz = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS) {

                int resultado = leitorVoz.setLanguage(
                        new Locale("pt", "BR")
                );

                vozDisponivel =
                        resultado != TextToSpeech.LANG_MISSING_DATA &&
                                resultado != TextToSpeech.LANG_NOT_SUPPORTED;

                btnOuvir.setEnabled(vozDisponivel);

            } else {
                btnOuvir.setEnabled(false);
            }
        });

        btnOuvir.setOnClickListener(v -> {

            if (!vozDisponivel) {
                return;
            }

            if (lendo) {
                pararLeitura();
            } else {
                iniciarLeitura();
            }
        });
    }

    // INICIA A LEITURA DA TELA
    private void iniciarLeitura() {

        String conteudo = obterTextosDaTela(tela);

        if (conteudo.trim().isEmpty()) {
            conteudo = "Bem-vindo ao UrbanFlow. "
                    + "Mobilidade inteligente para todos.";
        }

        leitorVoz.speak(
                conteudo,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "leituraUrbanFlow"
        );

        lendo = true;
        btnOuvir.setText("Parar leitura");

        leitorVoz.setOnUtteranceProgressListener(
                new android.speech.tts.UtteranceProgressListener() {

                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> {
                            lendo = false;
                            btnOuvir.setText("Ouvir");
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> {
                            lendo = false;
                            btnOuvir.setText("Ouvir");
                        });
                    }
                }
        );
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

            if (view instanceof TextView) {

                String texto = ((TextView) view)
                        .getText().toString().trim();

                if (!texto.isEmpty()
                        && !(view instanceof Button)
                        && !(view instanceof RadioButton)) {
                    textos.add(texto);
                }
            }

            if (view instanceof RadioButton) {
                textos.add(((RadioButton) view)
                        .getText().toString());
            }

            if (view instanceof Switch) {
                textos.add(((Switch) view).getText().toString());
            }

            if (view instanceof ViewGroup) {
                coletarTextos((ViewGroup) view, textos);
            }
        }
    }

    // PARA A LEITURA
    private void pararLeitura() {

        if (leitorVoz != null) {
            leitorVoz.stop();
        }

        lendo = false;

        if (btnOuvir != null) {
            btnOuvir.setText("Ouvir");
        }
    }

    // MODO DE ALTO CONTRASTE
    private void aplicarAltoContraste(boolean ativar) {

        if (ativar) {
            tela.setBackgroundColor(0xFF000000);
            alterarElementosContraste(tela, true);
        } else {
            tela.setBackgroundColor(0xFFF3F6F8);
            alterarElementosContraste(tela, false);
        }
    }

    private void alterarElementosContraste(
            ViewGroup grupo,
            boolean ativar
    ) {

        for (int i = 0; i < grupo.getChildCount(); i++) {

            View view = grupo.getChildAt(i);

            if (view instanceof LinearLayout) {
                view.setBackgroundColor(
                        ativar ? 0xFF000000 : 0x00000000
                );
            }

            if (view instanceof Button) {
                Button botao = (Button) view;

                botao.setBackgroundTintList(
                        ColorStateList.valueOf(
                                ativar ? 0xFFFFFF00 : 0xFF167D72
                        )
                );

                botao.setTextColor(
                        ativar ? 0xFF000000 : 0xFFFFFFFF
                );

            } else if (view instanceof TextView) {
                TextView texto = (TextView) view;

                texto.setTextColor(
                        ativar ? 0xFFFFFFFF : 0xFF102B46
                );
            }

            if (view instanceof Switch) {
                ((Switch) view).setThumbTintList(
                        ColorStateList.valueOf(
                                ativar ? 0xFFFFFF00 : 0xFF167D72
                        )
                );
            }

            if (view instanceof RadioButton) {
                RadioButton radio = (RadioButton) view;

                radio.setTextColor(
                        ativar ? 0xFFFFFFFF : 0xFF000000
                );

                radio.setButtonTintList(
                        ColorStateList.valueOf(
                                ativar ? 0xFFFFFF00 : 0xFF167D72
                        )
                );
            }

            if (view instanceof ViewGroup) {
                alterarElementosContraste(
                        (ViewGroup) view,
                        ativar
                );
            }
        }
    }

    // TAMANHO DA FONTE
    private void aplicarTamanhoFonte(int idFonte) {

        float fator = 1.0f;

        if (idFonte == R.id.fonteGrande) {
            fator = 1.20f;
        } else if (idFonte == R.id.fonteMuitoGrande) {
            fator = 1.40f;
        }

        ajustarTextos(tela, fator);
    }

    private void ajustarTextos(
            ViewGroup grupo,
            float fator
    ) {

        for (int i = 0; i < grupo.getChildCount(); i++) {

            View view = grupo.getChildAt(i);

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

            if (view instanceof ViewGroup) {
                ajustarTextos(
                        (ViewGroup) view,
                        fator
                );
            }
        }
    }

    @Override
    protected void onDestroy() {

        if (leitorVoz != null) {
            leitorVoz.stop();
            leitorVoz.shutdown();
        }

        super.onDestroy();
    }
}
