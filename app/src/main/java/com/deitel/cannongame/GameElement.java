// GameElement.java
// Representa um elemento do jogo delimitado por um retângulo
package com.deitel.cannongame;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class GameElement {
    protected CannonView view; // a view que contém este GameElement
    protected Paint paint = new Paint(); // Paint usado para desenhar
    protected Rect shape; // os limites retangulares do elemento (usados nas colisões)

    private RectF drawRect = new RectF(); // mesmo retângulo, em float, para desenhar
    private float velocityY; // velocidade vertical (pixels por segundo)
    private int soundId; // som associado a este elemento

    // construtor público
    public GameElement(CannonView view, int color, int soundId, int x,
                       int y, int width, int length, float velocityY) {
        this.view = view;
        paint.setColor(color);
        paint.setAntiAlias(true); // bordas suaves
        shape = new Rect(x, y, x + width, y + length); // define os limites
        this.soundId = soundId;
        this.velocityY = velocityY;
    }

    // atualiza a posição e verifica colisão com as paredes (topo/base)
    public void update(double interval) {
        // desloca na vertical: velocidade (px/s) * tempo decorrido (s)
        shape.offset(0, (int) (velocityY * interval));

        // se colidir com o topo ou a base, inverte a direção
        if (shape.top < 0 && velocityY < 0 ||
                shape.bottom > view.getScreenHeight() && velocityY > 0)
            velocityY *= -1;
    }

    // desenha este GameElement no Canvas como uma "pílula" (cantos totalmente
    // arredondados). A colisão continua usando o retângulo (shape).
    public void draw(Canvas canvas) {
        drawRect.set(shape);
        float radius = Math.min(drawRect.width(), drawRect.height()) / 2f;
        canvas.drawRoundRect(drawRect, radius, radius, paint);
    }

    // toca o som correspondente a este tipo de GameElement
    public void playSound() {
        view.playSound(soundId);
    }
}
