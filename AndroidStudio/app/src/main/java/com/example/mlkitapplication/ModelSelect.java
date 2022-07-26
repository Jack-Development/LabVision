package com.example.mlkitapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ModelSelect extends AppCompatActivity {

    private final Boolean debugMode = true;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_select);

        LinearLayout scrollMenu = (LinearLayout) findViewById(R.id.scrollMenu);

        String inputJSON = "";
        try {
            InputStream inputStream = getAssets().open("setup.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            inputJSON = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Object file = JSONValue.parse(inputJSON);
        JSONArray models = (JSONArray) file;

        for (int i = 0; i < models.size(); i++) {
            JSONObject targetModel = (JSONObject) models.get(i);
            scrollMenu.addView(createButton(targetModel));
        }
    }

    @SuppressLint("SetTextI18n")
    protected Button createButton(JSONObject currentModel) {
        String modelName = (String) currentModel.get("modelName");
        Object thresholdTemp = currentModel.get("threshold");
        float threshold = -100f;
        if (thresholdTemp == null) {
            threshold = 0.5f;
        } else {
            threshold = Float.parseFloat((String) thresholdTemp);
        }

        Button button = new Button(this);
        if (debugMode)
            button.setText("Model: " + modelName + "\nThreshold: " + String.valueOf(threshold * 100) + "%");
        else
            button.setText("Model: " + modelName);
        button.setTextSize(22);
        String finalThreshold = String.valueOf(threshold);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ModelSelect.this, MainActivity.class);
                intent.putExtra("model", modelName);
                intent.putExtra("threshold", finalThreshold);
                intent.putExtra("debugMode", debugMode);
                startActivity(intent);
            }
        });

        return button;
    }
}