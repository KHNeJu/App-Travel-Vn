package vn.travelvn.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

final class TravelIcon extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int icon;

    TravelIcon(Context context, int icon, int color) {
        super(context);
        this.icon = icon;
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.75f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(getWidth() / 24f, getHeight() / 24f);
        path.reset();
        switch (icon) {
            case 0 -> {
                path.moveTo(3, 10); path.lineTo(12, 3); path.lineTo(21, 10);
                path.lineTo(21, 21); path.lineTo(15, 21); path.lineTo(15, 15);
                path.lineTo(9, 15); path.lineTo(9, 21); path.lineTo(3, 21); path.close();
                canvas.drawPath(path, paint);
            }
            case 1 -> {
                canvas.drawRoundRect(4, 6, 20, 21, 3, 3, paint);
                canvas.drawRoundRect(8, 3, 16, 8, 2, 2, paint);
                canvas.drawLine(8, 12, 16, 12, paint);
                canvas.drawLine(8, 16, 14, 16, paint);
            }
            case 2 -> {
                canvas.drawCircle(12, 12, 9, paint);
                path.moveTo(16, 8); path.lineTo(14, 14); path.lineTo(8, 16);
                path.lineTo(10, 10); path.close();
                canvas.drawPath(path, paint);
            }
            case 3 -> {
                canvas.drawRoundRect(3, 5, 21, 21, 3, 3, paint);
                canvas.drawLine(3, 10, 21, 10, paint);
                canvas.drawLine(8, 3, 8, 7, paint);
                canvas.drawLine(16, 3, 16, 7, paint);
                canvas.drawCircle(12, 15, 1, paint);
            }
            case 4 -> {
                canvas.drawCircle(12, 7, 4, paint);
                path.moveTo(4, 21); path.cubicTo(4, 12, 20, 12, 20, 21);
                canvas.drawPath(path, paint);
            }
            case 5 -> {
                path.moveTo(5, 17); path.lineTo(7, 14); path.lineTo(7, 9);
                path.cubicTo(7, 2, 17, 2, 17, 9);
                path.lineTo(17, 14); path.lineTo(19, 17); path.close();
                canvas.drawPath(path, paint);
                canvas.drawLine(12, 2, 12, 4, paint);
                canvas.drawArc(9, 17, 15, 22, 15, 150, false, paint);
            }
            case 6 -> {
                path.moveTo(11, 22); path.cubicTo(7, 17, 3, 13, 3, 9);
                path.cubicTo(3, 3, 10, 1, 14, 4); path.moveTo(18, 11);
                path.cubicTo(17, 15, 13, 20, 11, 22); canvas.drawPath(path, paint);
                canvas.drawCircle(10, 9, 2, paint);
                canvas.drawLine(18, 2, 18, 8, paint); canvas.drawLine(15, 5, 21, 5, paint);
            }
            case 7 -> {
                path.moveTo(3, 8); path.lineTo(3, 5); path.lineTo(10, 5);
                path.lineTo(12, 8); path.lineTo(21, 8); path.lineTo(21, 20);
                path.lineTo(3, 20); path.close(); canvas.drawPath(path, paint);
            }
            case 8 -> {
                path.moveTo(12, 6); path.cubicTo(9, 3, 5, 3, 2, 5);
                path.lineTo(2, 19); path.cubicTo(5, 17, 9, 17, 12, 20);
                path.cubicTo(15, 17, 19, 17, 22, 19); path.lineTo(22, 5);
                path.cubicTo(19, 3, 15, 3, 12, 6); path.lineTo(12, 20);
                canvas.drawPath(path, paint);
            }
            case 9 -> {
                canvas.drawLine(4, 6, 20, 6, paint);
                canvas.drawLine(4, 12, 20, 12, paint);
                canvas.drawLine(4, 18, 20, 18, paint);
            }
            case 10 -> {
                canvas.drawRoundRect(5, 3, 19, 21, 2, 2, paint);
                canvas.drawLine(8, 8, 16, 8, paint);
                canvas.drawLine(8, 12, 16, 12, paint);
                canvas.drawLine(8, 16, 13, 16, paint);
            }
            case 11 -> {
                canvas.drawLine(5, 20, 5, 13, paint);
                canvas.drawLine(12, 20, 12, 4, paint);
                canvas.drawLine(19, 20, 19, 10, paint);
            }
            case 12 -> {
                canvas.drawRoundRect(2, 5, 22, 19, 3, 3, paint);
                canvas.drawCircle(12, 12, 3, paint);
            }
            case 13 -> {
                path.moveTo(3, 20); path.lineTo(3, 4); path.lineTo(21, 4);
                path.lineTo(21, 16); path.lineTo(7, 16); path.close();
                canvas.drawPath(path, paint);
                canvas.drawLine(8, 12, 13, 7, paint);
                canvas.drawLine(12, 12, 17, 12, paint);
            }
            case 14 -> {
                canvas.drawCircle(12, 12, 9, paint);
                canvas.drawOval(8, 3, 16, 21, paint);
                canvas.drawLine(3, 12, 21, 12, paint);
            }
            case 15 -> {
                path.moveTo(12, 3); path.cubicTo(2, 4, 0, 17, 10, 21);
                path.cubicTo(16, 23, 21, 19, 21, 14);
                path.cubicTo(13, 17, 8, 11, 12, 3); path.close();
                canvas.drawPath(path, paint);
            }
            case 16 -> {
                canvas.drawCircle(12, 12, 9, paint);
                canvas.drawPoint(12, 8, paint);
                canvas.drawLine(12, 11, 12, 16, paint);
            }
            case 17 -> {
                path.moveTo(12, 3); path.lineTo(20, 6); path.lineTo(20, 12);
                path.quadTo(19, 18, 12, 22); path.quadTo(5, 18, 4, 12);
                path.lineTo(4, 6); path.close(); canvas.drawPath(path, paint);
            }
        }
        canvas.restore();
    }
}
