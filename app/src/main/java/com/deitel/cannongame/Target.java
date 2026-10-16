// Target.java
// Subclasse de GameElement customizada para o Target (alvo)
package com.deitel.cannongame;

public class Target extends GameElement {
    private int hitReward; // recompensa (segundos) quando este alvo é atingido

    // construtor
    public Target(CannonView view, int color, int hitReward, int x, int y,
                  int width, int length, float velocityY) {
        super(view, color, CannonView.TARGET_SOUND_ID, x, y, width, length,
                velocityY);
        this.hitReward = hitReward;
    }

    // retorna a recompensa deste Target
    public int getHitReward() {
        return hitReward;
    }
}
