package com.example.mlkitapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class DisplayInformation extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_information);

        Bundle b = getIntent().getExtras();
        String target = b.getString("type");
        String modelName = b.getString("model");

        String inputJSON = "";
        try {
            InputStream inputStream = getAssets().open(modelName + "/labels.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            inputJSON = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Object file = JSONValue.parse(inputJSON);
        JSONObject targetObject = null;
        JSONArray labels = (JSONArray) file;
        for(int i = 0; i < labels.size(); i++){
            JSONObject label = (JSONObject) labels.get(i);
            if(Objects.equals((String) label.get("shortName"), target)) {
                targetObject = label;
                break;
            }
        }

        TextView titleText = findViewById(R.id.componentTitle);
        TextView descText = findViewById(R.id.componentInfo);
        ImageView compImage = findViewById(R.id.componentImage);

        if(targetObject != null) {
            titleText.setText((String) targetObject.get("name"));
            descText.setText((String) targetObject.get("description"));

            try {
                InputStream is = getAssets().open(modelName + "/images/" + (String) targetObject.get("shortName") + ".jpg");
                Bitmap bmp = BitmapFactory.decodeStream(is);
                is.close();
                compImage.setImageBitmap(bmp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            titleText.setText("Null object");
            descText.setText("Invalid description");
        }
    }
}