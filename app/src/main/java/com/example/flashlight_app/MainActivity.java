package com.example.flashlight_app;

import static java.lang.Thread.sleep;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import java.nio.channels.Channel;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int RC_NOTIFICATION = 99;
    private ToggleButton toggleFlashLightOnOff;
    private ToggleButton sos_toggle;
    private ToggleButton strobe_toggle;
    private CameraManager cameraManager;
    private SeekBar strobe_seekbar;
    private String getCameraID;

    //protected ImageButton imgbutton;
    String channelID = "CHANNEL_ID_NOTIFICATION";
    public long shortDelay = 200;
    public long longDelay = 600;

    private SensorManager sensorManager;
    public float changedvalue;
    private Sensor lightsensor;

    //public boolean flash_active=false;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchView;

    private ImageView imageView ;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.flash_off);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        getWindow().setStatusBarColor(ContextCompat.getColor(MainActivity.this, R.color.black));

        // Register the ToggleButton with specific ID
        toggleFlashLightOnOff = findViewById(R.id.toggle_flashlight);

        // cameraManager to interact with camera devices
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        // toggle sos
        sos_toggle = findViewById(R.id.sos_toggleButton);

        //SOS image button
        //imgbutton = findViewById(R.id.sosButton);

        //switch button
        switchView = findViewById(R.id.switch1);

        //strobe toggle button
        strobe_toggle = findViewById(R.id.strobe_toggle);

        //seekBar strobe
        strobe_seekbar = findViewById(R.id.strobe_seekBar);


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Log.e("MainActivity", "Device doesn't have flashlight feature");
            // Handle error
            return;
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
            lightsensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        } else{
            Toast.makeText(this, "Light Sensor not present", Toast.LENGTH_SHORT).show();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID, "My_Notification", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }


        //switch
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    sensorManager.registerListener(MainActivity.this, lightsensor, SensorManager.SENSOR_DELAY_NORMAL);

                } else {

                    sensorManager.unregisterListener(MainActivity.this);

                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, RC_NOTIFICATION);
        }

        /*imgbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                flashSOS();
            }
        });

         */

        try {
            // O means back camera unit,
            // 1 means front camera unit
            getCameraID = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            Log.e("MainActivity", "Error accessing camera ID", e);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RC_NOTIFICATION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Not Allowed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void sos_toggleflashlight(View view) {
        if(sos_toggle.isChecked()){
            flashSOS();
        }
        else{
            flashOff();
        }
    }
    // RequiresApi is set because, the devices which are
    // below API level 10 don't have the flash unit with
    // camera.
    public void toggleFlashLight(View view) {

        if (toggleFlashLightOnOff.isChecked()) {
            // Exception handled, because to check
            // whether the camera resource is being used by
            // another service or not.

                flashOn_img();

                makeONnotification();

        } else {
            // Exception is handled, because to check
            // whether the camera resource is being used by
            // another service or not.
                flashOff_img();

                makeOFFnotification();
        }
    }

    public void strobe_toggle(View view) {

        if(strobe_toggle.isChecked()){
            strobe_seekbar.setVisibility(View.VISIBLE);
        }
        else{
            strobe_seekbar.setVisibility(View.GONE);
        }
    }

    // when you click on button and torch open and
    // you do not close the torch again this code
    // will off the torch automatically
    @Override
    public void finish() {
        super.finish();
        try {
            // true sets the torch in OFF mode
            cameraManager.setTorchMode(getCameraID, false);

        } catch (CameraAccessException e) {

            Log.e("MainActivity", "Error accessing camera ID", e);
        }
    }

    public void makeONnotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, channelID);
        builder.setSmallIcon(R.drawable.flashlight_screen);
        builder.setContentTitle("Flashlight Turned ON");
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, RC_NOTIFICATION);
            }
            return;
        }
        notificationManager.notify(0, builder.build());
    }

    public void makeOFFnotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, channelID);
        builder.setSmallIcon(R.drawable.flashlight_screen);
        builder.setContentTitle("Flashlight Turned OFF");
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);

        //check for permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, RC_NOTIFICATION);
            }
            return;
        }
        notificationManager.notify(0, builder.build());
    }

    public void flashSOS() {
        new Thread(() -> {
    try {
        // Flash "S" (3 short flashes)
        flashOn();
        sleep(shortDelay);
        flashOff();
        sleep(shortDelay);
        flashOn();
        sleep(shortDelay);
        flashOff();
        sleep(shortDelay);
        flashOn();
        sleep(shortDelay);
        flashOff();

        sleep(shortDelay);

        // Flash "O" (3 long flashes)
        flashOn();
        sleep(longDelay);
        flashOff();
        sleep(shortDelay);
        flashOn();
        sleep(longDelay);
        flashOff();
        sleep(shortDelay);
        flashOn();
        sleep(longDelay);
        flashOff();

        sleep(shortDelay);

        // Flash "S" (3 short flashes)
        flashOn();
        sleep(shortDelay);
        flashOff();
        sleep(shortDelay);
        flashOn();
        sleep(shortDelay);
        flashOff();
        sleep(shortDelay);
        flashOn();
        sleep(shortDelay);
        flashOff();
    }catch (InterruptedException e) {
        Log.e("MainActivity", "InterruptedException in flashSOS()", e);
        // Restore interrupted state..
        Thread.currentThread().interrupt();
    }
        }).start();
    }

    public void flashOn_img() {
        imageView.setImageResource(R.drawable.flash_on_removebg);
        try {
            cameraManager.setTorchMode(getCameraID, true);
        } catch (CameraAccessException e) {
            Log.e("MainActivity", "Error accessing camera ID", e);
        }
    }

    public void flashOff_img() {
        imageView.setImageResource(R.drawable.flashlight_logo_removedbg);
        try {
            cameraManager.setTorchMode(getCameraID, false);
        } catch (CameraAccessException e) {
            Log.e("MainActivity", "Error accessing camera ID", e);
        }
    }
    public void flashOn() {
        try {
            cameraManager.setTorchMode(getCameraID, true);
        } catch (CameraAccessException e) {
            Log.e("MainActivity", "Error accessing camera ID", e);
        }
    }

    public void flashOff() {
        try {
            cameraManager.setTorchMode(getCameraID, false);
        } catch (CameraAccessException e) {
            Log.e("MainActivity", "Error accessing camera ID", e);
        }
    }

    public void sleep(long duration) throws InterruptedException{
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Log.e("MainActivity", "Error accessing camera ID", e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        changedvalue = event.values[0];

        if(changedvalue<1){
            flashOn_img();
        }
        else{
            flashOff_img();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}