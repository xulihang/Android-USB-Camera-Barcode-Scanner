package com.dynamsoft.usbcamera;

public final class Intents {
    private Intents() {
    }
    public static final class ScanResult {
        private ScanResult() {

        }
        public static final String BARCODETEXT = "BarcodeText";
        public static final String BARCODEFORMAT = "BarcodeFormat";
        public static final String X1 = "x1";
        public static final String X2 = "x2";
        public static final String X3 = "x3";
        public static final String X4 = "x4";
        public static final String Y1 = "y1";
        public static final String Y2 = "y2";
        public static final String Y3 = "y3";
        public static final String Y4 = "y4";
    }

    public static final class ScanOptions {
        private ScanOptions() {

        }
        public static final String CONTINUOUS = "continuous";
        public static final String MANUAL = "manual";

    }


}
