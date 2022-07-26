package com.example.mlkitapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.objects.DetectedObject;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RectOverlay extends View {
    private Boolean debugMode;

    private final Paint clickPaint;
    private final Paint clickTextPaint;

    private Boolean toInformation = false;
    private String targetObj = "";

    private Boolean isTouch = false;

    private List<DetectedObject> results = new ArrayList<>();

    private float[] clickPoint = new float[2];
    ArrayList<ArrayList<String> > paintInfo = new ArrayList<>();

    public RectOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        clickPaint = makePaint("#00FF00", "OUTLINE");
        clickTextPaint = makePaint("#00FF00", "TEXT");
    }

    protected Paint makePaint(String hexCode, String type){
        Paint newPaint = new Paint();
        newPaint.setColor(Color.parseColor(hexCode));

        if(type.toUpperCase().equals("OUTLINE")){
            newPaint.setStrokeWidth(10);
            newPaint.setStyle(Paint.Style.STROKE);
        }
        else if(type.toUpperCase().equals("TEXT")){
            newPaint.setTextSize(32);
            newPaint.setStyle(Paint.Style.FILL);
        }

        return newPaint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (DetectedObject object : results) {

            String hexCode = "#FF0000";
            String targetName = object.getLabels().get(0).getText();

            for(int i = 0; i < paintInfo.size(); i++){
                if(paintInfo.get(i).get(0).equals(targetName)){
                    hexCode = paintInfo.get(i).get(1);
                    break;
                }
            }

            Paint targetPaint = makePaint(hexCode, "OUTLINE");
            Paint targetTextPaint = makePaint(hexCode, "TEXT");

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
            if(debugMode)
                canvas.drawText(object.getLabels().get(0).getText() + ", " + String.format(getResources().getConfiguration().locale, "%.2f", object.getLabels().get(0).getConfidence() * 100) + "%", object.getBoundingBox().left, object.getBoundingBox().top - (fontSize / 2), targetTextPaint);
            else
                canvas.drawText(object.getLabels().get(0).getText(), object.getBoundingBox().left, object.getBoundingBox().top - (fontSize / 2), targetTextPaint);
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

    public void setDebugMode(Boolean b){
        debugMode = b;
    }

    public void setModel(String modelName){
        String inputJSON = "";
        try {
            InputStream inputStream = this.getContext().getAssets().open(modelName + "/labels.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            inputJSON = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Object file = JSONValue.parse(inputJSON);

        JSONArray labels = (JSONArray) file;
        for(int i = 0; i < labels.size(); i++){
            JSONObject label = (JSONObject) labels.get(i);
            String shortName = (String) label.get("shortName");
            String hexCode = (String) label.get("colour");

            paintInfo.add(new ArrayList<>());
            paintInfo.get(i).add(0, shortName);
            paintInfo.get(i).add(1, hexCode);
        }
    }
}
