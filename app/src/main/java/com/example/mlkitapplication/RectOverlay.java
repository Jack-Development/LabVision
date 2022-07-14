package com.example.mlkitapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.objects.DetectedObject;

import java.util.ArrayList;
import java.util.List;

public class RectOverlay extends View {
    private Paint paint;
    private Paint clickPaint;
    private Paint textPaint;
    private Paint clickTextPaint;

    private Boolean toInformation = false;
    private String targetObj = "";

    private Boolean isTouch = false;

    private List<DetectedObject> results = new ArrayList<>();

    private float[] clickPoint = new float[2];

    public RectOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);

        clickPaint = new Paint();
        clickPaint.setColor(Color.GREEN);
        clickPaint.setStrokeWidth(10);
        clickPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(32);
        textPaint.setStyle(Paint.Style.FILL);

        clickTextPaint = new Paint();
        clickTextPaint.setColor(Color.GREEN);
        clickTextPaint.setTextSize(32);
        clickTextPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (DetectedObject object : results) {

            Paint targetPaint = paint;
            Paint targetTextPaint = textPaint;
            Rect boundBox = object.getBoundingBox();
            if (boundBox.contains((int) clickPoint[0], (int) clickPoint[1])) {
                targetPaint = clickPaint;
                targetTextPaint = clickTextPaint;

                if(targetObj.equals("") && !toInformation) {
                    targetObj = object.getLabels().get(0).getText();
                    toInformation = true;
                }
            }
            canvas.drawRect(boundBox.left, boundBox.top, boundBox.right, boundBox.bottom, targetPaint);

            float boxWidth = object.getBoundingBox().width();
            float screenWidth = getWidth();
            float fontSize = (boxWidth / screenWidth) * 300;
            targetTextPaint.setTextSize(fontSize);
            canvas.drawText(object.getLabels().get(0).getText() + ", " + String.format(getResources().getConfiguration().locale, "%.2f", object.getLabels().get(0).getConfidence() * 100) + "%", object.getBoundingBox().left, object.getBoundingBox().top - (fontSize / 2), targetTextPaint);
        }
        if(!isTouch) {
            clickPoint[0] = -Float.MAX_VALUE;
            clickPoint[1] = -Float.MAX_VALUE;
        }
        results.clear();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        Log.d("motionEvent", event.toString());
        Log.d("motionEvent", event.getX() + ", " + event.getY());
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            isTouch = true;
            clickPoint[0] = event.getX();
            clickPoint[1] = event.getY();
        }
        else if (event.getAction() == MotionEvent.ACTION_UP){
            isTouch = false;
        }
        else {
            clickPoint[0] = event.getX();
            clickPoint[1] = event.getY();
        }
        return true;
    }

    public void drawOverlay(DetectedObject object, Canvas canvas) {
        String targetLabel = object.getLabels().get(0).getText();
        boolean found = false;
        boolean replace = false;
        DetectedObject toRemove = null;
        for (DetectedObject checkObject : results) {
            if (checkObject.getLabels().get(0).getText().equals(targetLabel)) {
                found = true;
                float confidenceToBeat = checkObject.getLabels().get(0).getConfidence();
                float confidenceChallenge = object.getLabels().get(0).getConfidence();
                if (confidenceChallenge > confidenceToBeat) {
                    replace = true;
                    toRemove = checkObject;
                }
                break;
            }
        }
        if (replace) {
            results.remove(toRemove);
            results.add(object);
            invalidate();
        } else if (!found) {
            results.add(object);
            invalidate();
        }
    }

    public void updateOverlay() {
        results.clear();
        invalidate();
    }

    public String getTargetObj(){
        return targetObj;
    }
    public void setTargetObj(String s){
        targetObj = s;
    }

    public Boolean getRedirect() {
        return toInformation;
    }
    public void setRedirect(Boolean newVal){
        if(!newVal) {
            isTouch = false;
        }
        toInformation = newVal;
    }
}
