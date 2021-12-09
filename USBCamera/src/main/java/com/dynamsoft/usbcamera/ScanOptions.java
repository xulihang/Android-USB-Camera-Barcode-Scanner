package com.dynamsoft.usbcamera;

import android.content.Context;
import android.content.Intent;

public class ScanOptions {
    private boolean continuous = false;
    private boolean manual = false;

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public void setManual(boolean manual){
        this.manual = manual;
    }

    public Intent createScanIntent(Context context) {
        Intent intentScan = new Intent(context, getCaptureActivity());
        intentScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentScan.putExtra(Intents.ScanOptions.CONTINUOUS, continuous);
        intentScan.putExtra(Intents.ScanOptions.MANUAL, manual);
        return intentScan;
    }

    private Class<?> captureActivity;

    protected Class<?> getDefaultCaptureActivity() {
        return CaptureActivity.class;
    }

    public Class<?> getCaptureActivity() {
        if (captureActivity == null) {
            captureActivity = getDefaultCaptureActivity();
        }
        return captureActivity;
    }

    /**
     * Set the Activity class to use. It can be any activity, but should handle the intent extras
     * as used here.
     *
     * @param captureActivity the class
     */
    public ScanOptions setCaptureActivity(Class<?> captureActivity) {
        this.captureActivity = captureActivity;
        return this;
    }
}
