// MenuArtView.java
// Ilustração do menu desenhada com Canvas: uma "carta de tiro" com o canhão,
// a linha de mira, o ângulo e os alvos. Não usa nenhuma imagem.
package com.deitel.cannongame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

public class MenuArtView extends View {
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tealPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint warmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dangerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint burstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    // posições dos alvos (frações da altura): topo de cada "pílula"
    private static final float[] TARGET_TOPS =
            {0.16f, 0.30f, 0.10f, 0.26f, 0.20f, 0.34f, 0.14f};
    private static final int HIT_INDEX = 3; // alvo atingido na ilustração

    public MenuArtView(Context context, AttributeSet attrs) {
        super(context, attrs);
        int ink = color(R.color.ink);
        gridPaint.setColor(color(R.color.stroke));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAlpha(150);
        aimPaint.setColor(color(R.color.ink_muted));
        aimPaint.setStyle(Paint.Style.STROKE);
        aimPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setColor(color(R.color.ink_muted));
        arcPaint.setStyle(Paint.Style.STROKE);
        inkPaint.setColor(ink);
        inkPaint.setStrokeCap(Paint.Cap.ROUND);
        tealPaint.setColor(color(R.color.accent));
        warmPaint.setColor(color(R.color.accent_warm));
        warmPaint.setStrokeCap(Paint.Cap.ROUND);
        dangerPaint.setColor(color(R.color.danger));
        burstPaint.setColor(color(R.color.accent_warm));
        burstPaint.setStrokeCap(Paint.Cap.ROUND);
        highlightPaint.setColor(0xFFFFFFFF);
        highlightPaint.setAlpha(110);
        textPaint.setColor(color(R.color.ink_muted));
        textPaint.setTypeface(android.graphics.Typeface.create(
                "sans-serif-condensed", android.graphics.Typeface.NORMAL));
    }

    private int color(int resId) {
        return getResources().getColor(resId, getContext().getTheme());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float dp = getResources().getDisplayMetrics().density;

        // 1) grade de "papel quadriculado"
        float step = h / 8f;
        gridPaint.setStrokeWidth(Math.max(1f, dp));
        for (float x = step; x < w; x += step)
            canvas.drawLine(x, 0, x, h, gridPaint);
        for (float y = step; y < h; y += step)
            canvas.drawLine(0, y, w, y, gridPaint);

        // 2) alvos (pílulas) em fileira, como no jogo
        float pillW = w * 0.042f;
        float pillH = h * 0.30f;
        float gap = w * 0.02f;
        float startX = w * 0.46f;
        float r = pillW / 2f;
        for (int i = 0; i < TARGET_TOPS.length; i++) {
            float left = startX + i * (pillW + gap);
            float top = h * TARGET_TOPS[i];
            rect.set(left, top, left + pillW, top + pillH);
            canvas.drawRoundRect(rect, r, r, (i % 2 == 0) ? tealPaint : warmPaint);
        }

        // bloqueador (vermelho) à frente dos alvos
        float blockerW = w * 0.03f;
        float blockerLeft = w * 0.30f;
        rect.set(blockerLeft, h * 0.08f, blockerLeft + blockerW, h * 0.38f);
        canvas.drawRoundRect(rect, blockerW / 2f, blockerW / 2f, dangerPaint);

        // 3) ponto de impacto no alvo atingido
        float hitLeft = startX + HIT_INDEX * (pillW + gap);
        float hitX = hitLeft + pillW / 2f;
        float hitY = h * TARGET_TOPS[HIT_INDEX] + pillH * 0.62f;

        // 4) canhão e linha de mira
        float cx = w * 0.07f;
        float cy = h * 0.74f;
        double angle = Math.atan2(cy - hitY, hitX - cx); // ângulo para cima (radianos)
        float baseR = h * 0.065f;

        aimPaint.setStrokeWidth(Math.max(2f, 2f * dp));
        aimPaint.setPathEffect(new DashPathEffect(new float[]{0.1f, 9f * dp}, 0));
        canvas.drawLine(cx, cy, hitX, hitY, aimPaint);

        // linha de referência horizontal
        canvas.drawLine(cx, cy, w * 0.40f, cy, aimPaint);
        aimPaint.setPathEffect(null);

        // arco do ângulo entre a horizontal e a mira
        float arcR = h * 0.22f;
        arcPaint.setStrokeWidth(Math.max(1.5f, 1.5f * dp));
        rect.set(cx - arcR, cy - arcR, cx + arcR, cy + arcR);
        float deg = (float) Math.toDegrees(angle);
        canvas.drawArc(rect, -deg, deg, false, arcPaint);
        textPaint.setTextSize(h * 0.032f);
        canvas.drawText(String.format(Locale.getDefault(), "%d°", Math.round(deg)),
                cx + arcR + 8f * dp, cy - h * 0.012f, textPaint);

        // cano apontando para o alvo
        float barrelLen = h * 0.16f;
        float bx = cx + (float) (Math.cos(angle) * barrelLen);
        float by = cy - (float) (Math.sin(angle) * barrelLen);
        inkPaint.setStrokeWidth(h * 0.05f);
        canvas.drawLine(cx, cy, bx, by, inkPaint);
        warmPaint.setStrokeWidth(h * 0.014f);
        canvas.drawLine(cx + (bx - cx) * 0.35f, cy + (by - cy) * 0.35f,
                cx + (bx - cx) * 0.88f, cy + (by - cy) * 0.88f, warmPaint);

        // base do canhão
        canvas.drawCircle(cx, cy, baseR, inkPaint);
        canvas.drawCircle(cx, cy, baseR * 0.62f, warmPaint);
        canvas.drawCircle(cx, cy, baseR * 0.30f, inkPaint);

        // 5) bala no meio do caminho
        float t = 0.58f;
        float ballX = cx + (hitX - cx) * t;
        float ballY = cy + (hitY - cy) * t;
        float ballR = h * 0.026f;
        canvas.drawCircle(ballX, ballY, ballR, inkPaint);
        canvas.drawCircle(ballX - ballR * 0.3f, ballY - ballR * 0.3f,
                ballR * 0.35f, highlightPaint);

        // 6) faíscas do impacto
        burstPaint.setStrokeWidth(Math.max(2f, 2.5f * dp));
        float inner = pillW * 0.9f;
        float outer = pillW * 1.7f;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + Math.PI / 8;
            canvas.drawLine(hitX + (float) (Math.cos(a) * inner),
                    hitY + (float) (Math.sin(a) * inner),
                    hitX + (float) (Math.cos(a) * outer),
                    hitY + (float) (Math.sin(a) * outer), burstPaint);
        }
    }
}
