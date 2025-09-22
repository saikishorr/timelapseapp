package com.saikishor.vivocamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.TextureView;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class CameraHelper {

    private Context context;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Handler backgroundHandler;
    private Timer timer;
    private TimeLapseEncoder encoder;

    public CameraHelper(Context context, TextureView textureView) {
        this.context = context;
        this.textureView = textureView;
    }

    public void openCamera() {
        HandlerThread thread = new HandlerThread("CameraBackground");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());

        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    startPreview();
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                }
            }, backgroundHandler);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void startPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(1280, 720);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(new android.view.Surface(texture));
            cameraDevice.createCaptureSession(
                    java.util.Collections.singletonList(new android.view.Surface(texture)),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
                            } catch (CameraAccessException e) { e.printStackTrace(); }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {}
                    }, backgroundHandler
            );
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }

    public void startTimeLapse(File outputFile) {
        encoder = new TimeLapseEncoder(outputFile, 4f); // 4x speed
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(textureView.isAvailable()) {
                    Bitmap bitmap = textureView.getBitmap();
                    encoder.addFrame(bitmap);
                }
            }
        }, 0, 250); // capture every 250ms for 4x speed
    }

    public void stopTimeLapse() {
        if (timer != null) {
            timer.cancel();
        }
        if (encoder != null) {
            encoder.finishEncoding();
        }
    }
}
