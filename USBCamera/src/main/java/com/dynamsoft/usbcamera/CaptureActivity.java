package com.dynamsoft.usbcamera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dynamsoft.dbr.BarcodeReader;
import com.dynamsoft.dbr.BarcodeReaderException;
import com.dynamsoft.dbr.DBRDLSLicenseVerificationListener;
import com.dynamsoft.dbr.DMDLSConnectionParameters;
import com.dynamsoft.dbr.EnumResultCoordinateType;
import com.dynamsoft.dbr.Point;
import com.dynamsoft.dbr.PublicRuntimeSettings;
import com.dynamsoft.dbr.TextResult;
import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CaptureActivity extends BaseActivity implements CameraDialog.CameraDialogParent {

    private static final boolean DEBUG = true;	// TODO set false on release
    private static final String TAG = "CaptureActivity";

    /**
     * lock
     */
    private final Object mSync = new Object();

    /**
     * set true if you want to record movie using MediaSurfaceEncoder
     * (writing frame data into Surface camera from MediaCodec
     *  by almost same way as USBCameratest2)
     * set false if you want to record movie using MediaVideoEncoder
     */
    private static final boolean USE_SURFACE_ENCODER = false;

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_WIDTH = 640; // 640
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_HEIGHT = 480; //480
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 0; // YUV

    /**
     * for accessing USB
     */
    private USBMonitor mUSBMonitor;
    /**
     * Handler to execute camera related methods sequentially on private thread
     */
    private UVCCameraHandler mCameraHandler;
    /**
     * for camera preview display
     */
    private CameraViewInterface mUVCCameraView;
    /**
     * for open&start / stop&close camera preview
     */
    private ImageButton mCameraButton;
    private TextView resultTextView;
    private BarcodeReader barcodeReader;
    private ImageView canvasImageView;
    private Timer timer = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate:");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_capture);
        try {
            initDBR();
        } catch (BarcodeReaderException e) {
            e.printStackTrace();
        }
        mCameraButton = findViewById(R.id.imageButton);
        canvasImageView = findViewById(R.id.canvasImageView);
        canvasImageView.setAdjustViewBounds(true);
        canvasImageView.setScaleType(ImageView.ScaleType.CENTER);
        resultTextView = findViewById(R.id.resultTextView);
        mCameraButton.setOnClickListener(mOnClickListener);

        final View view = findViewById(R.id.camera_view);
        mUVCCameraView = (CameraViewInterface)view;
        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (double)PREVIEW_HEIGHT);

        synchronized (mSync) {
            mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
            mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
                    USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
        }

    }

    private void initDBR() throws BarcodeReaderException {
        barcodeReader = new BarcodeReader();
        DMDLSConnectionParameters dbrParameters = new DMDLSConnectionParameters();
        dbrParameters.organizationID = "200001";
        barcodeReader.initLicenseFromDLS(dbrParameters, new DBRDLSLicenseVerificationListener() {
            @Override
            public void DLSLicenseVerificationCallback(boolean isSuccessful, Exception e) {
                if (!isSuccessful) {
                    e.printStackTrace();
                }
            }
        });
        PublicRuntimeSettings settings = barcodeReader.getRuntimeSettings();
        settings.resultCoordinateType = EnumResultCoordinateType.RCT_PIXEL;
        barcodeReader.updateRuntimeSettings(settings);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart:");
        synchronized (mSync) {
            mUSBMonitor.register();
        }
        if (mUVCCameraView != null) {
            mUVCCameraView.onResume();
        }
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop:");
        synchronized (mSync) {
            mCameraHandler.close();	// #close include #stopRecording and #stopPreview
            mUSBMonitor.unregister();
        }
        if (mUVCCameraView != null)
            mUVCCameraView.onPause();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy:");
        synchronized (mSync) {
            if (mCameraHandler != null) {
                mCameraHandler.setPreviewCallback(null); //zhf
                mCameraHandler.release();
                mCameraHandler = null;
            }
            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }
        super.onDestroy();
    }

    /**
     * event handler when click camera / capture button
     */
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            synchronized (mSync) {
                if ((mCameraHandler != null) && !mCameraHandler.isOpened()) {
                    CameraDialog.showDialog(CaptureActivity.this);
                } else {
                    mCameraHandler.close();
                }
            }
        }
    };

    private void startConnectedCamera(){
        try{
            mUSBMonitor.requestPermission(mUSBMonitor.getDeviceList().get(0));
        } catch(Exception e){
            e.printStackTrace();
        }

    }

    private void startPreview() {
        synchronized (mSync) {
            if (mCameraHandler != null) {
                final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                mCameraHandler.setPreviewCallback(mIFrameCallback);
                mCameraHandler.startPreview(new Surface(st));
                stopDecoding();
                startDecoding();
            }
        }
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(CaptureActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
            if (getIntent().getBooleanExtra(Intents.ScanOptions.MANUAL,false) == false){
                startConnectedCamera();
            }
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            if (DEBUG) Log.v(TAG, "onConnect:");
            synchronized (mSync) {
                if (mCameraHandler != null) {
                    mCameraHandler.open(ctrlBlock);
                    startPreview();
                }
            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect:");
            stopDecoding();
            synchronized (mSync) {
                if (mCameraHandler != null) {
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                // maybe throw java.lang.IllegalStateException: already released
                                mCameraHandler.setPreviewCallback(null); //zhf
                            }
                            catch(Exception e){
                                e.printStackTrace();
                            }
                            mCameraHandler.close();
                        }
                    }, 0);
                }
            }
        }
        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(CaptureActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    /**
     * to access from CameraDialog
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        synchronized (mSync) {
            return mUSBMonitor;
        }
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (DEBUG) Log.v(TAG, "onDialogResult:canceled=" + canceled);
    }

    //================================================================================
    private boolean isActive() {
        return mCameraHandler != null && mCameraHandler.isOpened();
    }

    private boolean checkSupportFlag(final int flag) {
        return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
    }

    private int getValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
    }

    private int setValue(final int flag, final int value) {
        return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
    }

    private int resetValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
    }


    // if you need frame data as byte array on Java side, you can use this callback method with UVCCamera#setFrameCallback
    // if you need to create Bitmap in IFrameCallback, please refer following snippet.
    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {

        }
    };

    private void startDecoding(){
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Bitmap bmp = mUVCCameraView.captureStillImage();
                decode(bmp);
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(task, 1000, 100);
    }
    private void stopDecoding(){
        if (timer != null){
            timer.cancel();
            timer = null;
        }
    }

    private void decode(final Bitmap bitmap){
        try {
            final TextResult[] results = barcodeReader.decodeBufferedImage(bitmap,"");
            Log.d("DBR",String.valueOf(results.length));
            runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Found ");
                        sb.append(results.length);
                        sb.append(" barcodes:");
                        sb.append("\n");
                        for (TextResult tr : results){
                            sb.append(tr.barcodeText);
                            sb.append("\n");
                        }
                        resultTextView.setText(sb.toString());
                        showOverlay(bitmap,results);
                        if (results.length>0){
                            if (getIntent().getBooleanExtra(Intents.ScanOptions.CONTINUOUS,false) == false){
                                onScanned(results[0]);
                            }
                        }
                        //canvasImageView.setImageBitmap(bitmap);
                    }
                }
            );
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void showOverlay(Bitmap bitmap, TextResult[] results){
        if (results.length>0){
            Canvas canvas = new Canvas(bitmap);
            for (TextResult tr:results){
                for (int i=0;i<4;i++){
                    Point[] points = tr.localizationResult.resultPoints;
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    Log.d(TAG,"X: "+points[0].x);

                    canvas.drawLine(points[0].x, points[0].y, points[1].x, points[1].y,paint);
                    canvas.drawLine(points[1].x, points[1].y, points[2].x, points[2].y,paint);
                    canvas.drawLine(points[2].x, points[2].y, points[3].x, points[3].y,paint);
                    canvas.drawLine(points[3].x, points[3].y, points[0].x, points[0].y,paint);
                }
            }
            canvasImageView.setImageBitmap(bitmap);
        }else{
            Bitmap bm = Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888);
            canvasImageView.setImageBitmap(bm);
        }

    }

    private void onScanned(TextResult result){
        mCameraHandler.close();
        Intent intent = new Intent();
        intent.putExtra(Intents.ScanResult.BARCODETEXT,result.barcodeText);
        intent.putExtra(Intents.ScanResult.BARCODEFORMAT,result.barcodeFormatString);
        intent.putExtra(Intents.ScanResult.X1,result.localizationResult.resultPoints[0].x);
        intent.putExtra(Intents.ScanResult.X2,result.localizationResult.resultPoints[1].x);
        intent.putExtra(Intents.ScanResult.X3,result.localizationResult.resultPoints[2].x);
        intent.putExtra(Intents.ScanResult.X4,result.localizationResult.resultPoints[3].x);
        intent.putExtra(Intents.ScanResult.Y1,result.localizationResult.resultPoints[0].y);
        intent.putExtra(Intents.ScanResult.Y2,result.localizationResult.resultPoints[1].y);
        intent.putExtra(Intents.ScanResult.Y3,result.localizationResult.resultPoints[2].y);
        intent.putExtra(Intents.ScanResult.Y4,result.localizationResult.resultPoints[3].y);
        CaptureActivity.this.setResult(RESULT_OK, intent);
        CaptureActivity.this.finish();
    }

}