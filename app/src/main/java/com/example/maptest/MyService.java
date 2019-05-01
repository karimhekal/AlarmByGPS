package com.example.maptest;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;

import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.service.notification.NotificationListenerService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

public class MyService extends Service {
    Vibrator vibrator;

    private static final String TAG = "LocationService";
    boolean enough=false;
    private FusedLocationProviderClient mFusedLocationClient;
    private final static long UPDATE_INTERVAL = 4 * 1000;  /* 4 secs */
    private final static long FASTEST_INTERVAL = 2000; /* 2 sec */

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
boolean shown=false;

    private void showNotification(String title,String description) {
        String CHANNEL_ID = "my_channel_01";

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

// notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
        vibrator.cancel();
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        showEmployee();
        vibrator = (Vibrator) MyService.this.getSystemService(Context.VIBRATOR_SERVICE);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        showNotification("We're Monitoring your location","You havent arrived yet");
//        if (Build.VERSION.SDK_INT >= 26) {
//            String CHANNEL_ID = "my_channel_01";
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
//                    "My Channel",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
//            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
//                    .setContentTitle("").setContentText("").build();
//            startForeground(1, notification);
//        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand: called.");
        getLocation();
        showEmployee();
        return START_NOT_STICKY;
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Log.e(TAG, "onLocationResult: got location result.");
            if (locationResult==null){
                Toast.makeText(MyService.this, "can't get current location", Toast.LENGTH_SHORT).show();
            }
            else {
                LatLng ll=new LatLng(locationResult.getLastLocation().getLatitude(),locationResult.getLastLocation().getLongitude());
                float[] distance = new float[2]; // to calculate distance between user and circle
                Location.distanceBetween(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude(), dlat, dlong, distance);
                if (distance[0] <= dradius) {
                    showNotification("You Arrived","Mabrook");
                    final MediaPlayer mp = MediaPlayer.create(MyService.this, R.raw.alarm);
                    mp.start();
                    final long[] pattern={250,350};
                    vibrator = (Vibrator) MyService.this.getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(pattern, 1);
                    enough=true;
                    File file = new File(path+"circle.txt");
                    file.delete();
//                    final Handler handler = new Handler();
//                    handler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            System.exit(0);
//                        }
//                    }, 3000);
                    Toast.makeText(MyService.this, "Click on the notification to confirm", Toast.LENGTH_LONG).show();
                    stopSelf();
                }else{
                 //  Toast.makeText(MyService.this, "back : Outside", Toast.LENGTH_SHORT).show();
                    //outside the circle
                }
            }
        }
    };

    private void getLocation() {
        // Create the location request to start receiving updates
        LocationRequest mLocationRequestHighAccuracy = new LocationRequest();
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestHighAccuracy.setInterval(UPDATE_INTERVAL);
        mLocationRequestHighAccuracy.setFastestInterval(FASTEST_INTERVAL);
        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "getLocation: stopping the location service.");
            stopSelf();
            return;
        }
        Log.e(TAG, "getLocation: getting location information.");
        mFusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, mLocationCallback,Looper.myLooper()); // Looper.myLooper tells this to repeat forever until thread is destroyed
    }


    String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/";
    String line = "";
    String[] employeeField;
    double dradius;
    double dlat ;
    double dlong;
    private void showEmployee() {
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(path+"circle.txt"));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);


            while ((line = bufferedReader.readLine()) != null) {
                employeeField = line.split("\\$");

//                lines.add("ID : "+  employeeField[0] + " Name : " + employeeField[1] + " Department : "+employeeField[2]);
                // Toast.makeText(MyService.this, line, Toast.LENGTH_SHORT).show();

            }
            String sradius = employeeField[0];
            String slat=employeeField[1];
            String slong=employeeField[2];

            dradius = Double.parseDouble(sradius);
            dlat = Double.parseDouble(slat);
            dlong = Double.parseDouble(slong);
            Toast.makeText(this, "Sleep peacefully hhh", Toast.LENGTH_SHORT).show();

            fileInputStream.close();
            bufferedReader.close();
        } catch (Exception e) {
            Log.e("readFromFile", e.toString());
        }
    }
}
