package com.dynamsoft.usbcamera;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private CheckBox continuousCheckBox;
    private CheckBox manualCheckBox;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        continuousCheckBox = findViewById(R.id.continuousCheckBox);
        manualCheckBox = findViewById(R.id.manualCheckBox);
        Button scanButton = findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                ScanOptions options = new ScanOptions();
                options.setContinuous(continuousCheckBox.isChecked());
                options.setManual(manualCheckBox.isChecked());
                barcodeLauncher.launch(options);
            }});
    }

    // Register the launcher and result handler
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getBarcodeText() == null) {
                    Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Scanned: " + result.getBarcodeText(), Toast.LENGTH_LONG).show();
                }
            });

}