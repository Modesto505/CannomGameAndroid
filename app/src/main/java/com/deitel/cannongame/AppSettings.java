// AppSettings.java
// Guarda e aplica a escolha de tema (claro, escuro ou do sistema)
package com.deitel.cannongame;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class AppSettings {
    // valores salvos nas preferências
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;

    private static final String PREFS = "cannon_prefs";
    private static final String KEY_THEME = "theme_mode";

    private AppSettings() { } // classe utilitária: não se cria objeto

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // lê o tema salvo (padrão: seguir o sistema)
    public static int getThemeMode(Context context) {
        return prefs(context).getInt(KEY_THEME, THEME_SYSTEM);
    }

    // salva o tema e já aplica; o AppCompat recria as telas abertas
    public static void setThemeMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_THEME, mode).apply();
        apply(mode);
    }

    // aplica o tema salvo (chamado quando o app inicia, em CannonApp)
    public static void applySaved(Context context) {
        apply(getThemeMode(context));
    }

    private static void apply(int mode) {
        switch (mode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
