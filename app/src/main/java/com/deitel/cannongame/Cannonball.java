// Cannonball.java
// Representa a bala disparada pelo canhão
package com.deitel.cannongame;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class Cannonball extends GameElement {
    private float velocityX; // velocidade horizontal (a vertical está em GameElement)
    private boolean onScreen; // a bala ainda está na tela?
    private Paint highlight = new Paint(); // brilho para dar volume à bala

    // construtor: recebe o RAIO em vez de largura e altura
    public Cannonball(CannonView view, int color, int soundId, int x,
                      int y, int radius, float velocityX, float velocityY) {
        super(view, color, soundId, x, y,
                2 * radius, 2 * radius, velocityY);
        this.velocityX = velocityX;
        onScreen = true;
        highlight.setColor(0xFFFFFFFF);
        highlight.setAlpha(110);
        highlight.setAntiAlias(true);
    }

    // obtém o raio da bala
    private int getRadius() {
        return (shape.right - shape.left) / 2;
    }

    // testa se a bala colide com o GameElement recebido
    public boolean collidesWith(GameElement element) {
        // 1) os retângulos se cruzam  2) a bala está indo da esquerda p/ direita
        return (Rect.intersects(shape, element.shape) && velocityX > 0);
    }

    // retorna true se a bala está na tela
    public boolean isOnScreen() {
        return onScreen;
    }

    // inverte a velocidade horizontal (a bala "quica" no Blocker)
    public void reverseVelocityX() {
        velocityX *= -1;
    }

    // atualiza a posição da bala
    @Override
    public void update(double interval) {
        super.update(interval); // atualiza a posição vertical

        // atualiza a posição horizontal
        shape.offset((int) (velocityX * interval), 0);

        // se a bala sair da tela
        if (shape.top < 0 || shape.left < 0 ||
                shape.bottom > view.getScreenHeight() ||
                shape.right > view.getScreenWidth())
            onScreen = false; // marca para ser removida
    }

    // desenha a bala como um círculo, com um pequeno brilho
    @Override
    public void draw(Canvas canvas) {
        int radius = getRadius();
        float cx = shape.left + radius;
        float cy = shape.top + radius;
        canvas.drawCircle(cx, cy, radius, paint);
        canvas.drawCircle(cx - radius * 0.3f, cy - radius * 0.3f,
                radius * 0.35f, highlight);
    }
}
