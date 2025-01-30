package com.example.timetracker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Map;

public class DonutChartView extends View {

    private Map<String, Integer> data;
    private Paint paint;
    private Paint textPaint;
    private RectF chartBounds;
    private float totalValue;
    private float animationProgress = 0f;

    private int[] colors = {
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#9C27B0"), // Deep Purple
            Color.parseColor("#673AB7"), // Indigo
            Color.parseColor("#3F51B5"), // Blue
            Color.parseColor("#2196F3"), // Light Blue
            Color.parseColor("#03A9F4"), // Cyan
            Color.parseColor("#00BCD4"), // Teal
            Color.parseColor("#009688"), // Green
            Color.parseColor("#4CAF50"), // Light Green
            Color.parseColor("#8BC34A"), // Lime
            Color.parseColor("#CDDC39"), // Yellow
            Color.parseColor("#FFEB3B"), // Amber
            Color.parseColor("#FFC107"), // Orange
            Color.parseColor("#FF9800"), // Deep Orange
            Color.parseColor("#FF5722"), // Brown
            Color.parseColor("#795548"), // Grey
            Color.parseColor("#9E9E9E"), // Blue Grey
            Color.parseColor("#607D8B"), // Black
            Color.parseColor("#000000")  // Deep Black for contrast
    };

    private int innerCircleColor = Color.parseColor("#00B0FF");

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        chartBounds = new RectF();
    }

    public void setData(Map<String, Integer> data) {
        this.data = data;
        totalValue = 0f;

        for (Integer value : data.values()) {
            totalValue += value;
        }

        // Animate the chart when data is set
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000); // Animation duration in milliseconds
        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data == null || data.isEmpty()) {
            return; // No data to draw
        }

        // Calculate chart bounds
        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        chartBounds.set(
                (width - size) / 2f + 50,
                (height - size) / 2f + 50,
                (width + size) / 2f - 50,
                (height + size) / 2f - 50
        );

        float startAngle = 0f;
        int colorIndex = 0;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            float sweepAngle = (entry.getValue() / totalValue) * 360 * animationProgress;

            paint.setColor(colors[colorIndex % colors.length]);
            canvas.drawArc(chartBounds, startAngle, sweepAngle, true, paint);

            // Calculate label position
            float angle = (float) Math.toRadians(startAngle + sweepAngle / 2);
            float labelX = (float) (chartBounds.centerX() + Math.cos(angle) * chartBounds.width() / 2.5);
            float labelY = (float) (chartBounds.centerY() + Math.sin(angle) * chartBounds.height() / 2.5);

            canvas.drawText(entry.getKey(), labelX, labelY, textPaint);

            startAngle += sweepAngle;
            colorIndex++;
        }

        // Draw inner circle to create a donut effect
        paint.setColor(innerCircleColor);
        canvas.drawCircle(chartBounds.centerX(), chartBounds.centerY(), chartBounds.width() / 4, paint);
    }
}
