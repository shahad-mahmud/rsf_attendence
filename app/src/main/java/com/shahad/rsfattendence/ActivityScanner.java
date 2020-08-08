package com.shahad.rsfattendence;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.shahad.rsfattendence.helperClasses.IconDialog;

import java.io.IOException;

/*
 * Created by SHAHAD MAHMUD on 7/25/20
 */
public class ActivityScanner extends AppCompatActivity {
    private static final int REQ_CAMERA_PER = 539;

    private SurfaceView scannerSurface;
    private CameraSource cameraSource;
    private ToneGenerator toneGenerator;

    IconDialog iconDialog;

    private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // check Camera permission
            if (ActivityCompat.checkSelfPermission(ActivityScanner.this,
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // permission was granted.
                try {
                    cameraSource.start(scannerSurface.getHolder());
                } catch (IOException e) {
                    iconDialog.startIconDialog(
                            "Can not start camera. Please try again!",
                            R.drawable.ic_cross
                    );
                    e.printStackTrace();
                }
            } else {
                // ask for permission
                ActivityCompat.requestPermissions(
                        ActivityScanner.this,
                        new String[]{Manifest.permission.CAMERA},
                        REQ_CAMERA_PER
                );
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            cameraSource.stop();
        }
    };
    private Detector.Processor<Barcode> barcodeProcessor = new Detector.Processor<Barcode>() {
        @Override
        public void release() {

        }

        @Override
        public void receiveDetections(Detector.Detections<Barcode> detections) {
            final SparseArray<Barcode> barcodeSparseArray = detections.getDetectedItems();

            if (barcodeSparseArray.size() != 0) {
                Log.d("SCANNER", barcodeSparseArray.valueAt(0).rawValue);
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150);

                Intent intent = new Intent();
                intent.putExtra("SCAN_RES", barcodeSparseArray.valueAt(0).rawValue);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        iconDialog = new IconDialog(ActivityScanner.this);

        findElements();
        initializeBarcodeEssentials();

        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
    }

    void findElements() {
        scannerSurface = findViewById(R.id.scanner_surface);
    }

    void initializeBarcodeEssentials() {
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(ActivityScanner.this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        cameraSource = new CameraSource.Builder(ActivityScanner.this, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true)
                .build();

        scannerSurface.getHolder().addCallback(surfaceHolderCallback);
        barcodeDetector.setProcessor(barcodeProcessor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeBarcodeEssentials();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraSource != null)
            cameraSource.release();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CAMERA_PER) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeBarcodeEssentials();
            } else {
                Toast.makeText(this, "Permission was not granted." +
                        " Can scan.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
