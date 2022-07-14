// Project: Code from Github page https://github.com/Chathunka/Android-CameraX
// Code Author: https://github.com/Chathunka
package com.example.mlkitapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import org.json.simple.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    PreviewView previewView;
    static RectOverlay rectOverlay;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    private final int modelID = 0;

    private String modelName = "";

    private static ObjectDetector objectDetector;
    private static Executor analysisExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        Bundle b = getIntent().getExtras();
        modelName = b.getString("model");
        float threshold = Float.parseFloat(b.getString("threshold"));

        TextView debugText = findViewById(R.id.debugLabel);
        String fullDebug = modelName + "\n Threshold: " + String.format(getResources().getConfiguration().locale, "%.2f", threshold * 100) + "%";
        debugText.setText(fullDebug);

        previewView = findViewById(R.id.viewFinder);
        rectOverlay = findViewById(R.id.rectOverlay);
        if (allPermissionsGranted()) {
            LocalModel localModel = new LocalModel.Builder()
                    .setAssetFilePath(modelName + "/model.tflite")
                    .build();

            CustomObjectDetectorOptions customObjectDetectorOptions =
                    new CustomObjectDetectorOptions.Builder(localModel)
                            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                            .enableMultipleObjects()
                            .enableClassification()
                            .setClassificationConfidenceThreshold(threshold)
                            .setMaxPerObjectLabelCount(3)
                            .build();

            objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
            analysisExecutor = Executors.newSingleThreadExecutor();
            startCamera();
        } else {
            int REQUEST_CODE_PERMISSIONS = 101;
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) cameraProviderFuture.get();
                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();
                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                // Get screen size
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                //Images are processed by passing an executor in which the image analysis is run
                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                //set the resolution of the view
                                .setTargetResolution(new Size(size.x, size.y))
                                //the executor receives the last available frame from the camera at the time that the analyze() method is called
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();
                // Set analyzer to MachineAnalyser model
                imageAnalysis.setAnalyzer(analysisExecutor, new MachineAnalyzer());
                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle(
                        ((LifecycleOwner) this),
                        cameraSelector,
                        preview,
                        imageAnalysis);

            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get() should
                // not block since the listener is being called, so no need to

                // handle InterruptedException.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    class MachineAnalyzer implements ImageAnalysis.Analyzer {
        private final String TAG = MachineAnalyzer.class.getName();

        public void analyze(ImageProxy imageProxy) {

            @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                objectDetector
                        .process(inputImage)
                        .addOnFailureListener(e -> {
                            e.printStackTrace();
                            Log.i(TAG, "FAILURE");
                        })
                        .addOnSuccessListener(results -> {
                            if (results.size() != 0) {
                                Log.i(TAG, "SUCCESS");
                                rectOverlay.setAlpha(1f);
                                Canvas canvas = new Canvas();
                                int count = 0;

                                for (DetectedObject detectedObject : results) {
                                    Rect boundingBox = detectedObject.getBoundingBox();
                                    Integer trackingId = detectedObject.getTrackingId();
                                    for (DetectedObject.Label label : detectedObject.getLabels()) {
                                        String text = label.getText();
                                        int index = label.getIndex();
                                        float confidence = label.getConfidence();
                                        Log.i(TAG, text + ", " + index + ", " + confidence);
                                        Log.i(TAG, boundingBox.flattenToString());
                                    }

                                    if (detectedObject.getLabels().size() > 0) {
                                        count++;
                                        rectOverlay.drawOverlay(detectedObject, canvas);
                                    }
                                }
                                if (count == 0) {
                                    rectOverlay.updateOverlay();
                                }
                            } else {
                                rectOverlay.updateOverlay();
                            }

                            if(rectOverlay.getRedirect()) {
                                String targetObj = rectOverlay.getTargetObj();

                                // Add screen move here
                                Intent intent = new Intent(MainActivity.this, DisplayInformation.class);
                                intent.putExtra("type", targetObj);
                                intent.putExtra("model", modelName);
                                startActivity(intent);

                                rectOverlay.setRedirect(false);
                                rectOverlay.setTargetObj("");
                            }

                        })
                        .addOnCompleteListener(complete -> {
                            mediaImage.close();
                            imageProxy.close();
                        });
            }
        }
    }

}
