// CannonApp.java
// Executa uma vez quando o app inicia: aplica o tema escolhido pelo jogador
package com.deitel.cannongame;

import android.app.Application;

public class CannonApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppSettings.applySaved(this);
    }
}
