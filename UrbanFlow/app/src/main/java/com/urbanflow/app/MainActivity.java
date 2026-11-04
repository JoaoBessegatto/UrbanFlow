package com.urbanflow.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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

        Button btnAlertas = findViewById(R.id.btnAlertas);

        btnAlertas.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AlertasActivity.class);
            startActivity(intent);
        });

        Button btnTrajetos = findViewById(R.id.btnTrajetos);

        btnTrajetos.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TrajetosActivity.class);
            startActivity(intent);
        });

        Button btnOcorrencias = findViewById(R.id.btnOcorrencias);

        btnOcorrencias.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OcorrenciasActivity.class);
            startActivity(intent);
        });

        Button btnMapa = findViewById(R.id.btnMapa);
        btnMapa.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapComposeActivity.class);
            startActivity(intent);
        });
    }
}
