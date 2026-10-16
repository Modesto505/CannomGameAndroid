// CannonView.java
// Exibe e controla o Cannon Game
package com.deitel.cannongame;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

// SurfaceView permite que OUTRA thread (não a da interface) desenhe na tela.
// SurfaceHolder.Callback avisa quando a superfície é criada/alterada/destruída.
public class CannonView extends SurfaceView
        implements SurfaceHolder.Callback {

    private static final String TAG = "CannonView"; // para logs de erro

    // constantes do jogo
    public static final int MISS_PENALTY = 2; // segundos perdidos ao acertar o Blocker
    public static final int HIT_REWARD = 3; // segundos ganhos ao acertar um alvo

    // constantes do canhão (percentuais do tamanho da tela)
    public static final double CANNON_BASE_RADIUS_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_WIDTH_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_LENGTH_PERCENT = 1.0 / 10;

    // constantes da bala
    public static final double CANNONBALL_RADIUS_PERCENT = 3.0 / 80;
    public static final double CANNONBALL_SPEED_PERCENT = 3.0 / 2;

    // constantes dos alvos
    public static final double TARGET_WIDTH_PERCENT = 1.0 / 40;
    public static final double TARGET_LENGTH_PERCENT = 3.0 / 20;
    public static final double TARGET_FIRST_X_PERCENT = 3.0 / 5;
    public static final double TARGET_SPACING_PERCENT = 1.0 / 60;
    public static final double TARGET_PIECES = 9;
    public static final double TARGET_MIN_SPEED_PERCENT = 3.0 / 4;
    public static final double TARGET_MAX_SPEED_PERCENT = 6.0 / 4;

    // constantes do Blocker
    public static final double BLOCKER_WIDTH_PERCENT = 1.0 / 40;
    public static final double BLOCKER_LENGTH_PERCENT = 1.0 / 4;
    public static final double BLOCKER_X_PERCENT = 1.0 / 2;
    public static final double BLOCKER_SPEED_PERCENT = 1.0;

    // tamanho do texto: 1/18 da altura da tela
    public static final double TEXT_SIZE_PERCENT = 1.0 / 18;

    private CannonThread cannonThread; // controla o game loop
    private Activity activity; // para exibir o diálogo na thread da interface
    private boolean dialogIsDisplayed = false;

    // objetos do jogo
    private Cannon cannon;
    private Blocker blocker;
    private ArrayList<Target> targets;

    // dimensões da tela
    private int screenWidth;
    private int screenHeight;

    // variáveis do game loop e das estatísticas
    private boolean gameOver; // o jogo terminou?
    private double timeLeft; // tempo restante (segundos)
    private int shotsFired; // tiros disparados
    private double totalElapsedTime; // tempo total decorrido (segundos)

    // constantes e variáveis para gerenciar os sons
    public static final int TARGET_SOUND_ID = 0;
    public static final int CANNON_SOUND_ID = 1;
    public static final int BLOCKER_SOUND_ID = 2;
    private SoundPool soundPool; // toca os efeitos sonoros
    private SparseIntArray soundMap; // mapeia nossos IDs para os do SoundPool

    // cores do tema atual (claro ou escuro): lidas de res/values e res/values-night
    private int colInk;
    private int colDanger;
    private int colAccent;
    private int colStroke;

    // Paints usados para desenhar o cenário e o painel de informações (HUD)
    private Paint backgroundPaint; // fundo em degradê
    private Paint gridPaint; // linhas da grade
    private Paint guidePaint; // linha tracejada central
    private Paint cardPaint; // fundo dos cartões do HUD
    private Paint cardStrokePaint; // borda dos cartões
    private Paint labelPaint; // rótulos pequenos (TEMPO, ALVOS)
    private Paint valuePaint; // valores grandes
    private Paint barPaint; // barra de tempo no rodapé
    private RectF tmpRect = new RectF(); // retângulo reaproveitado ao desenhar

    // construtor
    public CannonView(Context context, AttributeSet attrs) {
        super(context, attrs); // chama o construtor da superclasse
        activity = findActivity(context); // guarda a referência à Activity

        // registra este objeto como o SurfaceHolder.Callback
        getHolder().addCallback(this);

        // configura os atributos de áudio para jogos
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setUsage(AudioAttributes.USAGE_GAME);

        // inicializa o SoundPool para tocar os três efeitos sonoros
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);
        builder.setAudioAttributes(attrBuilder.build());
        soundPool = builder.build();

        // cria o mapa de sons e pré-carrega os arquivos de res/raw
        soundMap = new SparseIntArray(3);
        soundMap.put(TARGET_SOUND_ID,
                soundPool.load(context, R.raw.target_hit, 1));
        soundMap.put(CANNON_SOUND_ID,
                soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID,
                soundPool.load(context, R.raw.blocker_hit, 1));

        // cores do tema
        colInk = color(R.color.ink);
        colDanger = color(R.color.danger);
        colAccent = color(R.color.accent);
        colStroke = color(R.color.stroke);

        // Paints do cenário
        backgroundPaint = new Paint();

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(colStroke);
        gridPaint.setAlpha(110);
        gridPaint.setStyle(Paint.Style.STROKE);

        guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        guidePaint.setColor(colStroke);
        guidePaint.setStyle(Paint.Style.STROKE);

        // Paints do HUD
        cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setColor(color(R.color.surface));

        cardStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardStrokePaint.setColor(colStroke);
        cardStrokePaint.setStyle(Paint.Style.STROKE);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(color(R.color.ink_muted));
        labelPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        labelPaint.setLetterSpacing(0.14f);

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    // lê uma cor do tema atual
    private int color(int resId) {
        return getResources().getColor(resId, getContext().getTheme());
    }

    // O livro faz (Activity) context. Aqui "desembrulhamos" o Context caso ele
    // venha dentro de um ContextWrapper, evitando ClassCastException.
    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity)
                return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        throw new IllegalStateException("CannonView precisa estar em uma Activity");
    }

    // chamado quando o tamanho da SurfaceView muda
    // (inclusive na primeira vez em que ela entra na hierarquia de views)
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        screenWidth = w; // guarda a largura da CannonView
        screenHeight = h; // guarda a altura da CannonView

        // fundo em degradê (de cima para baixo)
        backgroundPaint.setShader(new LinearGradient(0, 0, 0, h,
                color(R.color.background_alt), color(R.color.background),
                Shader.TileMode.CLAMP));

        // espessuras proporcionais ao tamanho da tela
        float line = Math.max(1f, h * 0.003f);
        gridPaint.setStrokeWidth(line);
        cardStrokePaint.setStrokeWidth(line);
        guidePaint.setStrokeWidth(line * 1.5f);
        guidePaint.setPathEffect(
                new DashPathEffect(new float[]{h * 0.02f, h * 0.02f}, 0));

        // tamanhos de texto
        labelPaint.setTextSize(h * 0.032f);
        valuePaint.setTextSize((float) (TEXT_SIZE_PERCENT * h * 1.15));
    }

    // obtém a largura da tela do jogo
    public int getScreenWidth() {
        return screenWidth;
    }

    // obtém a altura da tela do jogo
    public int getScreenHeight() {
        return screenHeight;
    }

    // toca o som com o soundId dado em soundMap
    public void playSound(int soundId) {
        // parâmetros: id, volume esq., volume dir., prioridade, loop (0 = não), velocidade
        soundPool.play(soundMap.get(soundId), 1, 1, 1, 0, 1f);
    }

    // reinicia todos os elementos da tela e começa um novo jogo
    public void newGame() {
        // cria um novo canhão
        cannon = new Cannon(this,
                (int) (CANNON_BASE_RADIUS_PERCENT * screenHeight),
                (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth),
                (int) (CANNON_BARREL_WIDTH_PERCENT * screenHeight));

        Random random = new Random(); // para velocidades aleatórias
        targets = new ArrayList<>(); // nova lista de alvos

        // posição x do primeiro alvo (da esquerda para a direita)
        int targetX = (int) (TARGET_FIRST_X_PERCENT * screenWidth);

        // coordenada y dos alvos (centralizados verticalmente)
        int targetY = (int) ((0.5 - TARGET_LENGTH_PERCENT / 2) *
                screenHeight);

        // adiciona TARGET_PIECES alvos à lista
        for (int n = 0; n < TARGET_PIECES; n++) {

            // velocidade aleatória entre o mínimo e o máximo para o alvo n
            double velocity = screenHeight * (random.nextDouble() *
                    (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT) +
                    TARGET_MIN_SPEED_PERCENT);

            // alterna as cores entre escuro e claro
            int color = (n % 2 == 0) ? color(R.color.dark) : color(R.color.light);

            velocity *= -1; // inverte a velocidade inicial para o próximo alvo

            // cria e adiciona um novo Target à lista
            targets.add(new Target(this, color, HIT_REWARD, targetX, targetY,
                    (int) (TARGET_WIDTH_PERCENT * screenWidth),
                    (int) (TARGET_LENGTH_PERCENT * screenHeight),
                    (int) velocity));

            // avança a coordenada x para posicionar o próximo alvo à direita
            targetX += (TARGET_WIDTH_PERCENT + TARGET_SPACING_PERCENT) *
                    screenWidth;
        }

        // cria um novo Blocker (cor de perigo: acertá-lo custa tempo)
        blocker = new Blocker(this, colDanger, MISS_PENALTY,
                (int) (BLOCKER_X_PERCENT * screenWidth),
                (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2) * screenHeight),
                (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
                (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
                (float) (BLOCKER_SPEED_PERCENT * screenHeight));

        timeLeft = 10; // começa a contagem regressiva em 10 segundos

        shotsFired = 0; // zera o número de tiros
        totalElapsedTime = 0.0; // zera o tempo decorrido

        if (gameOver) { // começa um novo jogo depois que o último terminou
            gameOver = false; // o jogo não está mais terminado
            cannonThread = new CannonThread(getHolder()); // nova thread
            cannonThread.start(); // inicia o game loop
        }

        hideSystemBars(); // entra no modo imersivo
    }

    // chamado repetidamente pela CannonThread para atualizar os elementos
    private void updatePositions(double elapsedTimeMS) {
        double interval = elapsedTimeMS / 1000.0; // converte para segundos

        // atualiza a posição da bala se ela está na tela
        if (cannon.getCannonball() != null)
            cannon.getCannonball().update(interval);

        blocker.update(interval); // atualiza a posição do blocker

        for (GameElement target : targets)
            target.update(interval); // atualiza a posição de cada alvo

        timeLeft -= interval; // subtrai do tempo restante

        // se o tempo acabou
        if (timeLeft <= 0) {
            timeLeft = 0.0;
            gameOver = true; // o jogo terminou
            cannonThread.setRunning(false); // termina a thread
            showGameOverDialog(R.string.lose); // diálogo de derrota
        }
        // se todos os alvos foram atingidos ("else": evita dois diálogos no mesmo quadro)
        else if (targets.isEmpty()) {
            cannonThread.setRunning(false); // termina a thread
            showGameOverDialog(R.string.win); // diálogo de vitória
            gameOver = true;
        }
    }

    // alinha o cano e dispara a bala se não houver outra na tela
    public void alignAndFireCannonball(MotionEvent event) {
        // posição do toque nesta view
        Point touchPoint = new Point((int) event.getX(),
                (int) event.getY());

        // distância do toque até o centro da tela no eixo y
        double centerMinusY = (screenHeight / 2 - touchPoint.y);

        double angle = 0; // inicializa o ângulo com 0

        // calcula o ângulo que o cano faz com a horizontal
        angle = Math.atan2(touchPoint.x, centerMinusY);

        // aponta o cano para onde a tela foi tocada
        cannon.align(angle);

        // dispara se ainda não existe bala na tela
        if (cannon.getCannonball() == null ||
                !cannon.getCannonball().isOnScreen()) {
            cannon.fireCannonball();
            ++shotsFired;
        }
    }

    // Diálogo de fim de jogo. Precisa ser uma classe "public static" própria:
    // o AndroidX não aceita DialogFragment anônimo (o livro usava a versão antiga).
    // Os dados são passados por argumentos (Bundle).
    public static class GameResultDialog extends DialogFragment {
        private Runnable onReset; // o que fazer ao tocar em "Jogar de novo"

        public static GameResultDialog newInstance(int messageId, int shotsFired,
                                                   double totalElapsedTime,
                                                   boolean newRecord, float bestTime,
                                                   Runnable onReset) {
            GameResultDialog dialog = new GameResultDialog();
            Bundle args = new Bundle();
            args.putInt("messageId", messageId);
            args.putInt("shotsFired", shotsFired);
            args.putDouble("totalElapsedTime", totalElapsedTime);
            args.putBoolean("newRecord", newRecord);
            args.putFloat("bestTime", bestTime);
            dialog.setArguments(args);
            dialog.onReset = onReset;
            return dialog;
        }

        // cria o diálogo a partir do layout dialog_game_result.xml
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            Bundle args = requireArguments();
            int messageId = args.getInt("messageId");
            boolean won = (messageId == R.string.win);

            View content = LayoutInflater.from(requireActivity())
                    .inflate(R.layout.dialog_game_result, null);

            // título: "Vitória!" (cor de destaque) ou "Tempo esgotado" (cor de perigo)
            TextView title = (TextView) content.findViewById(R.id.dialogTitle);
            title.setText(messageId);
            title.setTextColor(requireActivity().getResources().getColor(
                    won ? R.color.accent : R.color.danger,
                    requireActivity().getTheme()));

            // estatísticas da partida
            ((TextView) content.findViewById(R.id.dialogShots))
                    .setText(String.valueOf(args.getInt("shotsFired")));
            ((TextView) content.findViewById(R.id.dialogTime)).setText(
                    String.format(Locale.getDefault(), "%.1f s",
                            args.getDouble("totalElapsedTime")));

            // recorde: só aparece quando o jogador venceu
            TextView record = (TextView) content.findViewById(R.id.dialogRecord);
            if (won) {
                record.setVisibility(View.VISIBLE);
                if (args.getBoolean("newRecord")) {
                    record.setText(R.string.new_record);
                    record.setBackgroundResource(R.drawable.bg_chip_record);
                    record.setTextColor(requireActivity().getResources().getColor(
                            R.color.on_warm, requireActivity().getTheme()));
                } else {
                    record.setText(getString(R.string.record_format,
                            args.getFloat("bestTime")));
                    record.setTextColor(requireActivity().getResources().getColor(
                            R.color.ink_muted, requireActivity().getTheme()));
                }
            }

            // botão "Jogar de novo"
            Button again = (Button) content.findViewById(R.id.btnAgain);
            again.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    if (onReset != null)
                        onReset.run(); // prepara e inicia novo jogo
                    else
                        requireActivity().finish(); // (estado perdido) volta ao menu
                }
            });

            // botão "Menu": fecha o jogo e volta para a tela inicial
            Button menu = (Button) content.findViewById(R.id.btnMenu);
            menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    requireActivity().finish();
                }
            });

            return new AlertDialog.Builder(requireActivity(), R.style.GameDialogTheme)
                    .setView(content)
                    .create();
        }

        // define a largura do diálogo depois que ele aparece
        @Override
        public void onStart() {
            super.onStart();
            Dialog dialog = getDialog();
            if (dialog != null && dialog.getWindow() != null) {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                int width = (int) Math.min(dm.widthPixels * 0.9f, 420 * dm.density);
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    // exibe o diálogo quando o jogo termina (e registra o resultado)
    private void showGameOverDialog(final int messageId) {
        boolean won = (messageId == R.string.win);

        // salva partida/recorde; retorna true se bateu o recorde
        boolean newRecord = GameStats.recordResult(getContext(), won, totalElapsedTime);
        float bestTime = GameStats.getBestTime(getContext());

        final GameResultDialog gameResult = GameResultDialog.newInstance(
                messageId, shotsFired, totalElapsedTime, newRecord, bestTime,
                new Runnable() {
                    @Override
                    public void run() {
                        dialogIsDisplayed = false;
                        newGame(); // prepara e inicia um novo jogo
                    }
                });

        // na thread da interface, usa o FragmentManager para exibir o DialogFragment
        activity.runOnUiThread(
                new Runnable() {
                    public void run() {
                        showSystemBars(); // sai do modo imersivo
                        dialogIsDisplayed = true;
                        gameResult.setCancelable(false); // diálogo modal
                        gameResult.show(((FragmentActivity) activity)
                                .getSupportFragmentManager(), "results");
                    }
                }
        );
    }

    // desenha o jogo no Canvas recebido
    public void drawGameElements(Canvas canvas) {
        // fundo em degradê + grade
        canvas.drawRect(0, 0, screenWidth, screenHeight, backgroundPaint);
        drawGrid(canvas);

        // painel de informações (tempo, alvos restantes, barra de tempo)
        drawHud(canvas);

        cannon.draw(canvas); // desenha o canhão

        // desenha a bala, se existir e estiver na tela
        if (cannon.getCannonball() != null &&
                cannon.getCannonball().isOnScreen())
            cannon.getCannonball().draw(canvas);

        blocker.draw(canvas); // desenha o blocker

        // desenha todos os alvos
        for (GameElement target : targets)
            target.draw(canvas);
    }

    // grade de "papel quadriculado" + linha tracejada na altura do canhão
    private void drawGrid(Canvas canvas) {
        for (int i = 1; i < 10; i++) {
            float x = screenWidth * i / 10f;
            canvas.drawLine(x, 0, x, screenHeight, gridPaint);
        }
        for (int i = 1; i < 6; i++) {
            float y = screenHeight * i / 6f;
            canvas.drawLine(0, y, screenWidth, y, gridPaint);
        }
        canvas.drawLine(0, screenHeight / 2f, screenWidth, screenHeight / 2f,
                guidePaint);
    }

    // painel de informações: tempo à esquerda, alvos à direita, barra embaixo
    private void drawHud(Canvas canvas) {
        float margin = screenHeight * 0.045f;
        float cardW = screenHeight * 0.52f;
        float cardH = screenHeight * 0.17f;
        boolean lowTime = timeLeft < 3; // últimos 3 segundos: alerta em vermelho

        drawHudCard(canvas, margin, margin, cardW, cardH,
                getResources().getString(R.string.hud_time),
                String.format(Locale.getDefault(), "%.1f s", timeLeft),
                lowTime ? colDanger : colInk);

        int total = (int) TARGET_PIECES;
        int destroyed = total - targets.size();
        drawHudCard(canvas, screenWidth - margin - cardW, margin, cardW, cardH,
                getResources().getString(R.string.hud_targets),
                destroyed + "/" + total, colInk);

        // barra de tempo no rodapé (cheia = 10 s ou mais)
        float barH = screenHeight * 0.012f;
        barPaint.setColor(colStroke);
        tmpRect.set(0, screenHeight - barH, screenWidth, screenHeight);
        canvas.drawRect(tmpRect, barPaint);

        float fraction = (float) Math.min(1.0, Math.max(0.0, timeLeft / 10.0));
        barPaint.setColor(lowTime ? colDanger : colAccent);
        tmpRect.set(0, screenHeight - barH, screenWidth * fraction, screenHeight);
        canvas.drawRect(tmpRect, barPaint);
    }

    // desenha um cartão arredondado com um rótulo pequeno e um valor grande
    private void drawHudCard(Canvas canvas, float x, float y, float w, float h,
                             String label, String value, int valueColor) {
        tmpRect.set(x, y, x + w, y + h);
        float radius = h * 0.22f;
        canvas.drawRoundRect(tmpRect, radius, radius, cardPaint);
        canvas.drawRoundRect(tmpRect, radius, radius, cardStrokePaint);

        float pad = h * 0.2f;
        canvas.drawText(label, x + pad, y + pad + labelPaint.getTextSize() * 0.8f,
                labelPaint);
        valuePaint.setColor(valueColor);
        canvas.drawText(value, x + pad, y + h - pad, valuePaint);
    }

    // verifica se a bala colide com o Blocker ou com algum alvo
    public void testForCollisions() {
        // remove os alvos com que a bala colide
        if (cannon.getCannonball() != null &&
                cannon.getCannonball().isOnScreen()) {
            for (int n = 0; n < targets.size(); n++) {
                if (cannon.getCannonball().collidesWith(targets.get(n))) {
                    targets.get(n).playSound(); // som de alvo atingido

                    // soma a recompensa ao tempo restante
                    timeLeft += targets.get(n).getHitReward();

                    cannon.removeCannonball(); // remove a bala do jogo
                    targets.remove(n); // remove o alvo atingido
                    --n; // garante que o novo alvo n seja testado
                    break;
                }
            }
        }
        else { // remove a bala se ela não deve estar na tela
            cannon.removeCannonball();
        }

        // verifica se a bala colide com o blocker
        if (cannon.getCannonball() != null &&
                cannon.getCannonball().collidesWith(blocker)) {
            blocker.playSound(); // som de acerto no blocker

            // inverte a direção da bala
            cannon.getCannonball().reverseVelocityX();

            // desconta a penalidade do blocker do tempo restante
            timeLeft -= blocker.getMissPenalty();
        }
    }

    // para o jogo: chamado pelo onPause do MainActivityFragment
    public void stopGame() {
        if (cannonThread != null)
            cannonThread.setRunning(false); // avisa a thread para terminar
    }

    // libera recursos: chamado pelo onDestroy do MainActivityFragment
    public void releaseResources() {
        soundPool.release(); // libera todos os recursos do SoundPool
        soundPool = null;
    }

    // chamado quando a superfície muda de tamanho
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format,
                               int width, int height) { }

    // chamado quando a superfície é criada pela primeira vez
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!dialogIsDisplayed) {
            newGame(); // prepara e inicia um novo jogo
            cannonThread = new CannonThread(holder); // cria a thread
            cannonThread.setRunning(true); // marca como em execução
            cannonThread.start(); // inicia o game loop
        }
    }

    // chamado quando a superfície é destruída
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (cannonThread == null)
            return;

        // garante que a thread termine corretamente
        boolean retry = true;
        cannonThread.setRunning(false); // pede para a thread terminar

        while (retry) {
            try {
                cannonThread.join(); // espera a cannonThread terminar
                retry = false;
            }
            catch (InterruptedException e) {
                Log.e(TAG, "Thread interrupted", e);
            }
        }
    }

    // chamado quando o usuário toca a tela
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // tipo da ação que causou o evento
        int action = e.getAction();

        // o usuário tocou ou arrastou o dedo pela tela
        if (action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_MOVE) {
            // dispara a bala na direção do toque
            alignAndFireCannonball(e);
        }

        return true; // evento tratado
    }

    // Subclasse de Thread que controla o game loop
    private class CannonThread extends Thread {
        private SurfaceHolder surfaceHolder; // para manipular o Canvas
        // volatile: garante que a mudança feita por OUTRA thread (setRunning)
        // seja vista pelo loop imediatamente
        private volatile boolean threadIsRunning = true; // ativa por padrão

        // inicializa o surface holder
        public CannonThread(SurfaceHolder holder) {
            surfaceHolder = holder;
            setName("CannonThread");
        }

        // altera o estado de execução
        public void setRunning(boolean running) {
            threadIsRunning = running;
        }

        // controla o game loop
        @Override
        public void run() {
            Canvas canvas = null; // usado para desenhar
            long previousFrameTime = System.currentTimeMillis();

            while (threadIsRunning) {
                try {
                    // obtém o Canvas para desenho exclusivo desta thread
                    canvas = surfaceHolder.lockCanvas(null);

                    // trava o surfaceHolder para desenhar
                    synchronized (surfaceHolder) {
                        // canvas pode ser null se a superfície não está pronta
                        if (canvas != null) {
                            long currentTime = System.currentTimeMillis();
                            double elapsedTimeMS = currentTime - previousFrameTime;
                            totalElapsedTime += elapsedTimeMS / 1000.0;
                            updatePositions(elapsedTimeMS); // atualiza o estado
                            testForCollisions(); // testa colisões
                            drawGameElements(canvas); // desenha com o canvas
                            previousFrameTime = currentTime; // guarda o instante
                        }
                    }
                }
                finally {
                    // exibe o conteúdo do canvas na CannonView
                    // e libera o Canvas para outras threads
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    canvas = null;
                }
            }
        }
    }

    // esconde as barras do sistema e a app bar (modo imersivo)
    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    // mostra as barras do sistema e a app bar
    @SuppressWarnings("deprecation")
    private void showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
