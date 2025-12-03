package com.redrak.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class FaceDetectionView extends AppCompatImageView {

    private final Paint paint;
    private final List<RectF> facesRects = new ArrayList<>();

    public FaceDetectionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
    }

    public void setFaces(JSONArray faces) {
        facesRects.clear();
        if (getDrawable() == null) return;
        
        Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        float scaleX = (float) viewWidth / bitmapWidth;
        float scaleY = (float) viewHeight / bitmapHeight;

        for (int i = 0; i < faces.length(); i++) {
            JSONObject face = faces.optJSONObject(i);
            if (face != null) {
                JSONArray boundingBox = face.optJSONArray("bounding_box");
                if (boundingBox != null && boundingBox.length() == 4) {
                    float left = (float) boundingBox.optDouble(0, 0.0) * bitmapWidth * scaleX;
                    float top = (float) boundingBox.optDouble(1, 0.0) * bitmapHeight * scaleY;
                    float right = (float) boundingBox.optDouble(2, 0.0) * bitmapWidth * scaleX;
                    float bottom = (float) boundingBox.optDouble(3, 0.0) * bitmapHeight * scaleY;
                    facesRects.add(new RectF(left, top, right, bottom));
                }
            }
        }
        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (RectF rect : facesRects) {
            canvas.drawRect(rect, paint);
        }
    }
}
