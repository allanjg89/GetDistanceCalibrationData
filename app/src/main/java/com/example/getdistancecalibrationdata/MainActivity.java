package com.example.getdistancecalibrationdata;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.os.Bundle;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.io.FileWriter;

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static int SLEEP_TIME = 1000; //Sleep time in ms between AP measurements.
    private File LOG_PATH;
    private static String LOG_TAG = "DistanceCalibrationData";


    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private NetworkInfo networkInfo;
    private EditText currentDistanceInput;

    private double currentDistance;
    private Button startButton;
    private Button submitButton;
    private Button stopButton;
    private TextView inputMesage;
    private String filename;
    private File outFile;
    private FileWriter writer;

    private static int PERMISSION_ALL = 1;
    private static String[] PERMISSIONS = {
            android.Manifest.permission.CHANGE_WIFI_STATE,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }


        inputMesage = findViewById(R.id.textView3);
        currentDistanceInput = findViewById(R.id.CurrentDistance);

        startButton = findViewById(R.id.startButton);
        submitButton = findViewById(R.id.submitButton);
        stopButton = findViewById(R.id.stop);



        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });



        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopButton.setVisibility(View.VISIBLE);
                currentDistance = Float.valueOf(currentDistanceInput.getText().toString());
                currentDistanceInput.setText("");
                createLogEntry();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setVisibility(View.GONE);
                currentDistanceInput.setVisibility(View.VISIBLE);
                inputMesage.setVisibility(View.VISIBLE);
                setUpWIfiInfo();
                setUpFile();
                submitButton.setVisibility(View.VISIBLE);
            }
        });
    }


    public void setUpWIfiInfo(){

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            postToMessages("Wifi was disabled... we enabled it ;)");
            wifiManager.setWifiEnabled(true);
        }

        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {

        }

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        /*
         * The application waits for a wifi connection to be established. Ideally this would be
         * done with a broadcast receiver but I could not get that to work...
         * */
        while (!connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {

            }
        }


    }

    private void setUpFile(){
        Date currentTime;
        String externalStorageState;
        String header;

        externalStorageState = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
            Log.w(LOG_TAG, "External Storage not available... exiting");
            finish();
        }

        if (LOG_PATH == null) {
            LOG_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);// /storage/emulated/0/Download
        }

        currentTime = Calendar.getInstance().getTime();
        filename = "DISTANCE_CALIBRATION_" + currentTime.toString() + ".txt";
        filename = filename.replace(" ", "");

        outFile = new File(LOG_PATH, filename);
        postToMessages("Path to file: " + outFile.getAbsolutePath());
        Log.w(LOG_TAG, "Path to file: " + outFile.getAbsolutePath());

        try {
            writer = new FileWriter(outFile);
            header = "RSSI\tDistance\n";
            writer.append(header);
        } catch (IOException e) {
            e.printStackTrace();
            Log.w(LOG_TAG, "Could Not Create Header in file... exiting");
            exit();
        }

    }

    private void createLogEntry() {

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String data;

        data = String.format("%d\t%.3f\r\n", wifiInfo.getRssi(), currentDistance);

        try {
            writer.append(data);
        }catch (IOException e) {
            Log.w(LOG_TAG, "Could Not Write to file... exiting");
            exit();
        }

    }


    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if(ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    private void postToMessages(String message){
            TextView messages = findViewById(R.id.messages);
            String messageString = messages.getText().toString();
            messageString = messageString +"\n[*]" + message;
            messages.setText(messageString);
    }

    private void exit(){

        try {
            writer.flush();
            writer.close();
        }catch (IOException e){
            Log.w(LOG_TAG, "Unable to close file correctly... exiting");
        }

        finish();
    }
}


