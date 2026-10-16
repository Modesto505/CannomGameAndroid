// MenuActivity.java
// Tela inicial: recorde, botão Jogar e escolha de tema
package com.deitel.cannongame;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MenuActivity extends AppCompatActivity {
    private TextView tvRecord;
    private TextView tvWins;
    private TextView tvGames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        tvRecord = (TextView) findViewById(R.id.tvRecord);
        tvWins = (TextView) findViewById(R.id.tvWins);
        tvGames = (TextView) findViewById(R.id.tvGames);

        // botão Jogar: abre a tela do jogo (MainActivity)
        findViewById(R.id.btnPlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, MainActivity.class));
            }
        });

        // seletor de tema: marca a opção salva ANTES de ligar o listener,
        // para não disparar a troca de tema sem o usuário tocar
        RadioGroup themeGroup = (RadioGroup) findViewById(R.id.themeGroup);
        switch (AppSettings.getThemeMode(this)) {
            case AppSettings.THEME_LIGHT:
                themeGroup.check(R.id.themeLight);
                break;
            case AppSettings.THEME_DARK:
                themeGroup.check(R.id.themeDark);
                break;
            default:
                themeGroup.check(R.id.themeSystem);
                break;
        }
        themeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int mode = AppSettings.THEME_SYSTEM;
                if (checkedId == R.id.themeLight)
                    mode = AppSettings.THEME_LIGHT;
                else if (checkedId == R.id.themeDark)
                    mode = AppSettings.THEME_DARK;
                // salva e aplica; esta tela é recriada com as novas cores
                AppSettings.setThemeMode(MenuActivity.this, mode);
            }
        });
    }

    // ao voltar do jogo, atualiza recorde e estatísticas
    @Override
    protected void onResume() {
        super.onResume();
        float best = GameStats.getBestTime(this);
        if (best < 0)
            tvRecord.setText(R.string.stat_none);
        else
            tvRecord.setText(String.format(Locale.getDefault(), "%.1f s", best));
        tvWins.setText(String.valueOf(GameStats.getWins(this)));
        tvGames.setText(String.valueOf(GameStats.getGames(this)));
    }
}
