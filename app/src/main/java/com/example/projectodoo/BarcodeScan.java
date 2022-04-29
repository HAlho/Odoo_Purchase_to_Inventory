package com.example.projectodoo;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.media.AudioManager;
import android.media.Image;
import android.media.ToneGenerator;
import android.util.Log;
import android.util.Size;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BarcodeScan {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private CameraSelector cameraSelector;
    private Preview preview;
    private ProcessCameraProvider cameraProvider;
    private final Context context;
    private final View view;
    private int previewViewId;

    private BarcodeScanListener listener;


    public interface BarcodeScanListener {
        // These methods are the different events and
        // need to pass relevant arguments related to the event triggered
        public void onBarcodeReady(String Barcode);

    }

    // Assign the listener implementing events interface that will receive the events
    public void setCustomObjectListener(BarcodeScanListener listener) {
        this.listener = listener;
    }


    public BarcodeScan(Context context, View view, int previewViewId ){
        this.context=context;
        this.view=view;
        this.previewViewId=previewViewId;
        this.listener=null;

    }
    public void destroyScanner(){
        cameraProvider.unbindAll();
    }

    public void startScan(){
        //request a camera provider
        cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        //check for camera provider availability
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview();
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
            Log.e("listener", "here");
            imageAnalyzer();
        }, ContextCompat.getMainExecutor(this.context));
    }


    private void imageAnalyzer(){
        //build image analyzer
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        // enable the following line if RGBA output is needed.
                        //.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        //.setTargetResolution(new Size(240, 240)) //720 1280
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {//this function is run periodically/for every image
            @Override
            @androidx.camera.core.ExperimentalGetImage
            public void analyze(@NonNull ImageProxy imageProxy) {

                    Log.e("image analyzer", "..............");
                try {
                    Thread.sleep(1000);//sleep for 3sec

                } catch (Exception e) {
                    e.printStackTrace();
                }

                    // barcode scan
                    Image mediaImage = imageProxy.getImage();

                    if (mediaImage != null) {
                        InputImage image =
                                InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                        // Pass image to an ML Kit Vision API
                        //BarcodeScanner scanner = BarcodeScanning.getClient();
                        // Or, to specify the formats to recognize:
                        BarcodeScannerOptions options =
                                new BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(Barcode.FORMAT_CODE_128)
                                        .build();
                        BarcodeScanner scanner = BarcodeScanning.getClient(options);

                        Task<List<Barcode>> result = scanner.process(image)
                                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                                    @Override
                                    public void onSuccess(List<Barcode> barcodes) {
                                        // Task completed successfully
                                        for (Barcode barcode : barcodes) {

                                            String rawValue = barcode.getRawValue();
                                            Log.e("barcode value", rawValue);
                                            Log.e("barcode read", String.valueOf(barcode.getFormat()));


                                            if (listener != null)
                                                listener.onBarcodeReady(rawValue);

                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        Log.e("Barcode Scan Failed", e.getMessage());
                                        // ...
                                    }
                                })
                                .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                                    @Override
                                    public void onComplete(@NonNull Task<List<Barcode>> task) {
                                        imageProxy.close();
                                    }
                                });


                    }

                }

        });



        try {
            Log.e("binding camera", "SUCCESS");
            cameraProvider
                    .bindToLifecycle((LifecycleOwner) this.context, cameraSelector, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Error when bind analysis", e);
        }
    }


    //select a a camera and bind the lifecycle and use cases
    private void bindPreview() {// was @nonnull ProcessCameraProvider cameraProvider
        preview = new Preview.Builder()
                .build();

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        previewView=this.view.findViewById(previewViewId);
        Log.e("previewView", previewView.toString()+ " "+ previewView.getTransitionName());
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this.context, cameraSelector, preview);


    }


}
