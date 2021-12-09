# Android-USB-Camera-Barcode-Scanner

An Android barcode scanner with can utilize usb cameras based on [saki4510t/UVCCamera](https://github.com/saki4510t/UVCCamera). [Dynamsoft Barcode Reader](https://www.dynamsoft.com/barcode-reader/overview/) is used as the barcode SDK.

It is possible to call it from another application using Intent:

1. Register the launcher and result handler

    ```java
    ActivityResultLauncher<Intent> barcodeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data.hasExtra("BarcodeText")){
                            Toast.makeText(MainActivity.this, "Scanned: " + data.getStringExtra("BarcodeText"), Toast.LENGTH_LONG).show();
                        }else{
                            Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
    ```

2. Start the activity:

    ```java
    Intent intent = new Intent();
    ComponentName cn=new ComponentName("com.dynamsoft.usbcamera",
            "com.dynamsoft.usbcamera.CaptureActivity");
    intent.setComponent(cn);
    barcodeLauncher.launch(intent);
    ```

## References

* [UVCcamera-Opencv
](https://github.com/o0olele/UVCcamera-Opencv)
* [Accessing a USB camera using Android-Camera2 API](https://stackoverflow.com/questions/57846505/accessing-a-usb-camera-using-android-camera2-api)