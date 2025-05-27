package com.example.eduease;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class ConfettiView extends View {

    private final ArrayList<Confetti> confettiList = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final Handler handler = new Handler();

    public ConfettiView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Confetti confetti : confettiList) {
            paint.setColor(confetti.color);
            paint.setAlpha(confetti.alpha);
            switch (confetti.shape) {
                case 0: // rectangle (was circle)
                    canvas.drawRect(confetti.x, confetti.y, confetti.x + confetti.size * 2, confetti.y + confetti.size, paint);
                    break;
                case 1: // square
                    canvas.drawRect(confetti.x, confetti.y, confetti.x + confetti.size, confetti.y + confetti.size, paint);
                    break;
                case 2: // line
                    canvas.drawLine(confetti.x, confetti.y, confetti.x + confetti.size, confetti.y + confetti.size / 2f, paint);
                    break;
            }
        }
    }

    public void startConfetti() {
        post(() -> {
            confettiList.clear();
            for (int i = 0; i < 120; i++) {
                Confetti confetti = new Confetti();
                confetti.x = random.nextInt(getWidth());
                confetti.y = -random.nextInt(300);
                confetti.size = random.nextInt(10) + 8;
                confetti.speedY = random.nextInt(10) + 12; // faster
                confetti.speedX = random.nextFloat() * 4f - 2f;
                confetti.alpha = 180 + random.nextInt(75);
                confetti.color = randomColor();
                confetti.shape = random.nextInt(3); // 0 = rectangle, 1 = square, 2 = line
                confettiList.add(confetti);
            }
            animateConfetti();
        });
    }

    private void animateConfetti() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean stillFalling = false;
                for (Confetti confetti : confettiList) {
                    if (confetti.y <= getHeight()) {
                        confetti.y += confetti.speedY;
                        confetti.x += confetti.speedX;
                        stillFalling = true;
                    }
                }

                invalidate();

                if (stillFalling) {
                    handler.postDelayed(this, 30);
                }
            }
        }, 30);
    }

    private int randomColor() {
        int[] colors = {
                Color.RED, Color.GREEN, Color.BLUE,
                Color.YELLOW, Color.MAGENTA, Color.CYAN,
                Color.rgb(255, 165, 0), // orange
                Color.rgb(255, 105, 180) // hot pink
        };
        return colors[random.nextInt(colors.length)];
    }

    private static class Confetti {
        float x, y;
        float speedX, speedY;
        int size;
        int alpha;
        int color;
        int shape; // 0 = rectangle, 1 = square, 2 = line
    }
}
