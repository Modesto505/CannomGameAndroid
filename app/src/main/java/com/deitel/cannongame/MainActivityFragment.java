// MainActivityFragment.java
// MainActivityFragment cria e gerencia a CannonView
package com.deitel.cannongame;

import android.media.AudioManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainActivityFragment extends Fragment {
    private CannonView cannonView; // view customizada que exibe o jogo

    // chamado quando a view do Fragment precisa ser criada
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // infla o layout fragment_main.xml
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // obtém a referência para a CannonView
        cannonView = (CannonView) view.findViewById(R.id.cannonView);
        return view;
    }

    // configura o controle de volume depois que a view existe.
    // (O livro usa onActivityCreated, hoje depreciado; onViewCreated faz o mesmo.)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // permite que os botões de volume do aparelho controlem o volume do jogo
        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    // quando a MainActivity é pausada, termina o jogo
    @Override
    public void onPause() {
        super.onPause();
        cannonView.stopGame(); // encerra o game loop
    }

    // quando a MainActivity é destruída, libera os recursos (SoundPool)
    @Override
    public void onDestroy() {
        super.onDestroy();
        cannonView.releaseResources();
    }
}
