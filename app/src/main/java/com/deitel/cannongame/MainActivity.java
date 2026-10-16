// MainActivity.java
// MainActivity exibe o MainActivityFragment
package com.deitel.cannongame;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    // chamado quando o app é iniciado pela primeira vez
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // o layout activity_main.xml contém apenas o MainActivityFragment
        setContentView(R.layout.activity_main);
    }
}
