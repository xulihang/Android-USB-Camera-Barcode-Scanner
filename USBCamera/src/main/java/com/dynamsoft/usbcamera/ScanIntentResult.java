package com.dynamsoft.usbcamera;

import android.app.Activity;
import android.content.Intent;

public final class ScanIntentResult {
    private final String barcodeText;
    private final String barcodeFormat;
    private final int x1;
    private final int x2;
    private final int x3;
    private final int x4;
    private final int y1;
    private final int y2;
    private final int y3;
    private final int y4;
    private final Intent originalIntent;

    ScanIntentResult(Intent intent) {
        this(null, null, 0, 0, 0, 0, 0, 0, 0, 0, intent);
    }
    ScanIntentResult(String barcodeText,
                     String barcodeFormat,
                     int x1,
                     int x2,
                     int x3,
                     int x4,
                     int y1,
                     int y2,
                     int y3,
                     int y4,
                     Intent originalIntent) {
        this.barcodeText = barcodeText;
        this.barcodeFormat = barcodeFormat;
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
        this.x4 = x4;
        this.y1 = y1;
        this.y2 = y2;
        this.y3 = y3;
        this.y4 = y4;
        this.originalIntent = originalIntent;
    }

    public static ScanIntentResult parseActivityResult(int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            String barcodeText = intent.getStringExtra(Intents.ScanResult.BARCODETEXT);
            String barcodeFormat = intent.getStringExtra(Intents.ScanResult.BARCODEFORMAT);
            int x1 = intent.getIntExtra(Intents.ScanResult.X1, Integer.MIN_VALUE);
            int x2 = intent.getIntExtra(Intents.ScanResult.X2, Integer.MIN_VALUE);
            int x3 = intent.getIntExtra(Intents.ScanResult.X3, Integer.MIN_VALUE);
            int x4 = intent.getIntExtra(Intents.ScanResult.X4, Integer.MIN_VALUE);
            int y1 = intent.getIntExtra(Intents.ScanResult.Y1, Integer.MIN_VALUE);
            int y2 = intent.getIntExtra(Intents.ScanResult.Y2, Integer.MIN_VALUE);
            int y3 = intent.getIntExtra(Intents.ScanResult.Y3, Integer.MIN_VALUE);
            int y4 = intent.getIntExtra(Intents.ScanResult.Y4, Integer.MIN_VALUE);
            return new ScanIntentResult(barcodeText,
                    barcodeFormat,
                    x1,
                    x2,
                    x3,
                    x4,
                    y1,
                    y2,
                    y3,
                    y4,
                    intent);
        }
        return new ScanIntentResult(intent);
    }

    public String getBarcodeText(){
        return barcodeText;
    }

    public String getBarcodeFormat(){
        return barcodeFormat;
    }

    public int getX1(){
        return x1;
    }
    public int getX2(){
        return x2;
    }
    public int getX3(){
        return x3;
    }
    public int getX4(){
        return x4;
    }

    public int getY1(){
        return y1;
    }
    public int getY2(){
        return y2;
    }
    public int getY3(){
        return y3;
    }
    public int getY4(){
        return y4;
    }
}
