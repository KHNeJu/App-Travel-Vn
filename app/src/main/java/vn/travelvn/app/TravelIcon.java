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
        }
        canvas.restore();
    }
}
