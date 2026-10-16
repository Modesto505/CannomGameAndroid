// GameStats.java
// Recorde (menor tempo para vencer), vitórias e partidas, salvos em SharedPreferences
package com.deitel.cannongame;

import android.content.Context;
import android.content.SharedPreferences;

public final class GameStats {
    private static final String PREFS = "cannon_prefs";
    private static final String KEY_BEST_TIME = "best_time"; // segundos; negativo = sem recorde
    private static final String KEY_WINS = "wins";
    private static final String KEY_GAMES = "games";

    private GameStats() { } // classe utilitária

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // menor tempo já feito para destruir os 9 alvos (negativo se ainda não há)
    public static float getBestTime(Context context) {
        return prefs(context).getFloat(KEY_BEST_TIME, -1f);
    }

    public static int getWins(Context context) {
        return prefs(context).getInt(KEY_WINS, 0);
    }

    public static int getGames(Context context) {
        return prefs(context).getInt(KEY_GAMES, 0);
    }

    // registra o fim de uma partida; retorna true se bateu o recorde
    public static synchronized boolean recordResult(Context context, boolean won,
                                                    double seconds) {
        SharedPreferences prefs = prefs(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_GAMES, prefs.getInt(KEY_GAMES, 0) + 1);

        boolean newRecord = false;
        if (won) {
            editor.putInt(KEY_WINS, prefs.getInt(KEY_WINS, 0) + 1);
            float best = prefs.getFloat(KEY_BEST_TIME, -1f);
            if (best < 0 || seconds < best) { // tempo menor = melhor
                editor.putFloat(KEY_BEST_TIME, (float) seconds);
                newRecord = true;
            }
        }
        editor.apply();
        return newRecord;
    }
}
