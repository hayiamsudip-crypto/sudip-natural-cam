package org.sudip.naturalcam;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector =
            CameraSelector.DEFAULT_BACK_CAMERA;
    private ExecutorService cameraExecutor;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraExecutor = Executors.newSingleThreadExecutor();
        buildInterface();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF080B10);

        TextView title = new TextView(this);
        title.setText("SUDIP NATURAL CAM");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setPadding(8, 18, 8, 14);
        root.addView(title, new LinearLayout.LayoutParams(
                -1, -2));

        previewView = new PreviewView(this);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        root.addView(previewView, new LinearLayout.LayoutParams(
                -1, 0, 1f));

        statusText = new TextView(this);
        statusText.setText("Natural camera • Ready");
        statusText.setTextColor(0xFFB0BEC5);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(8, 10, 8, 10);
        root.addView(statusText);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(8, 8, 8, 20);

        Button switchButton = new Button(this);
        switchButton.setText("Flip");
        switchButton.setOnClickListener(v -> {
            cameraSelector = cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA
                    ? CameraSelector.DEFAULT_FRONT_CAMERA
                    : CameraSelector.DEFAULT_BACK_CAMERA;
            startCamera();
        });

        Button captureButton = new Button(this);
        captureButton.setText("TAKE PHOTO");
        captureButton.setOnClickListener(v -> takePhoto());

        controls.addView(switchButton);
        controls.addView(captureButton);
        root.addView(controls);
        setContentView(root);
    }

    private void startCamera() {
        var future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

                statusText.setText("Camera ready • Quality priority");
            } catch (Exception e) {
                statusText.setText("Camera error: " + e.getClass().getSimpleName());
                Toast.makeText(this, "Camera চালু হয়নি", Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera প্রস্তুত নয়", Toast.LENGTH_SHORT).show();
            return;
        }

        String filename = "SUDIP_CAM_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(System.currentTimeMillis());

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SudipNaturalCam");

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values).build();

        imageCapture.takePicture(options, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(
                            ImageCapture.OutputFileResults result) {
                        runOnUiThread(() -> {
                            statusText.setText("Photo saved to Gallery");
                            Toast.makeText(MainActivity.this,
                                    "ছবি গ্যালারিতে সংরক্ষিত হয়েছে",
                                    Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onError(ImageCaptureException error) {
                        runOnUiThread(() -> Toast.makeText(
                                MainActivity.this,
                                "ছবি সংরক্ষণ হয়নি: " + error.getMessage(),
                                Toast.LENGTH_LONG).show());
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "ক্যামেরা অনুমতি প্রয়োজন",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}
