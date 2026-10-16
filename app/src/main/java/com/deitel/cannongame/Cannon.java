// Cannon.java
// Representa o canhão e dispara a bala (Cannonball)
package com.deitel.cannongame;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

public class Cannon {
    private int baseRadius; // raio da base do canhão
    private int barrelLength; // comprimento do cano
    private Point barrelEnd = new Point(); // ponto final do cano
    private double barrelAngle; // ângulo do cano (radianos)
    private Cannonball cannonball; // a bala do canhão (null = não existe)
    private Paint paint = new Paint(); // Paint do cano e da base
    private Paint detailPaint = new Paint(); // Paint dos detalhes dourados
    private int inkColor; // cor da bala (acompanha o tema claro/escuro)
    private CannonView view; // view que contém o canhão

    // construtor
    public Cannon(CannonView view, int baseRadius, int barrelLength,
                  int barrelWidth) {
        this.view = view;
        this.baseRadius = baseRadius;
        this.barrelLength = barrelLength;

        inkColor = view.getResources().getColor(R.color.ink,
                view.getContext().getTheme());
        int warm = view.getResources().getColor(R.color.accent_warm,
                view.getContext().getTheme());

        paint.setAntiAlias(true);
        paint.setStrokeWidth(barrelWidth); // largura do cano
        paint.setStrokeCap(Paint.Cap.ROUND); // ponta do cano arredondada
        paint.setColor(inkColor);

        detailPaint.setAntiAlias(true);
        detailPaint.setStrokeWidth(barrelWidth * 0.28f);
        detailPaint.setStrokeCap(Paint.Cap.ROUND);
        detailPaint.setColor(warm);

        align(Math.PI / 2); // cano inicialmente apontando reto para a direita
    }

    // alinha o cano do canhão com o ângulo recebido
    public void align(double barrelAngle) {
        this.barrelAngle = barrelAngle;
        // trigonometria: seno -> componente x; cosseno -> componente y
        barrelEnd.x = (int) (barrelLength * Math.sin(barrelAngle));
        barrelEnd.y = (int) (-barrelLength * Math.cos(barrelAngle)) +
                view.getScreenHeight() / 2;
    }

    // cria e dispara a bala na direção para onde o canhão aponta
    public void fireCannonball() {
        // componente x da velocidade da bala
        int velocityX = (int) (CannonView.CANNONBALL_SPEED_PERCENT *
                view.getScreenWidth() * Math.sin(barrelAngle));

        // componente y da velocidade da bala
        int velocityY = (int) (CannonView.CANNONBALL_SPEED_PERCENT *
                view.getScreenWidth() * -Math.cos(barrelAngle));

        // raio da bala
        int radius = (int) (view.getScreenHeight() *
                CannonView.CANNONBALL_RADIUS_PERCENT);

        // "carrega o canhão": cria a bala e a posiciona dentro do canhão
        cannonball = new Cannonball(view, inkColor,
                CannonView.CANNON_SOUND_ID, -radius,
                view.getScreenHeight() / 2 - radius, radius, velocityX,
                velocityY);

        cannonball.playSound(); // toca o som do disparo
    }

    // desenha o canhão no Canvas
    public void draw(Canvas canvas) {
        float centerY = view.getScreenHeight() / 2f;

        // cano
        canvas.drawLine(0, centerY, barrelEnd.x, barrelEnd.y, paint);

        // faixa dourada ao longo do cano
        float dx = barrelEnd.x;
        float dy = barrelEnd.y - centerY;
        canvas.drawLine(dx * 0.35f, centerY + dy * 0.35f,
                dx * 0.88f, centerY + dy * 0.88f, detailPaint);

        // base (círculo centrado na borda esquerda: só metade aparece)
        canvas.drawCircle(0, centerY, baseRadius, paint);
        canvas.drawCircle(0, centerY, baseRadius * 0.62f, detailPaint);
        canvas.drawCircle(0, centerY, baseRadius * 0.30f, paint);
    }

    // retorna a bala que este canhão disparou
    public Cannonball getCannonball() {
        return cannonball;
    }

    // remove a bala do jogo
    public void removeCannonball() {
        cannonball = null;
    }
}
