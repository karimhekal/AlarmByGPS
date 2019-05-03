package com.example.maptest;

import android.Manifest;
import android.app.IntentService;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
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
import java.util.Arrays;

public class MyService extends Service implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    final static String MY_ACTION = "MY_ACTION";
    Vibrator vibrator;
    String initData;
    private static final String TAG = "LocationService";
    boolean enough=false;
    private FusedLocationProviderClient mFusedLocationClient;
    private final static long UPDATE_INTERVAL = 4000;  /* 4 secs */
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
        //stopSelf();
        //startService(new Intent(MyService.this, MyService.class));

        Log.e("Service :","Stopped");

        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
        vibrator.cancel();
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) MyService.this.getSystemService(Context.VIBRATOR_SERVICE);
        Toast.makeText(MyService.this, "service started", Toast.LENGTH_SHORT).show();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        showNotification("We're Monitoring your location","You havent arrived yet");
        if (Build.VERSION.SDK_INT >= 26) { // this notificatinos to inform user that service is running .. this also prevents system from killing the service because of background limitations
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "My Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("wwwwwwwwwwwwww").setContentText("We're monitoring your location").build();
            startForeground(1, notification);
        }
    }

    double circleLat,circleLng,circleRadius; // to store circle position that came from intent
    String[] circleArray; // to split the incoming circle position from intent and store them in it
    String[] pointsArray; // to split "allpoints" array and store the points positions in the arrays below
    String[] firstPoint;
    String[] secondPoint;
    String[] thirdPoint;
    String[] fourthPoint;

    boolean workingOnPolygon=false; // these boolean's job is to check if we're working on poly or circle
    boolean workingOnCircle=false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            markersLatlng = new LatLng[4];
            Log.e(TAG, "onStartCommand: called.");
            initData = intent.getStringExtra("DATA");
            Log.e("SERVICE INTENT : ", initData);
            if (initData.contains("*")) {
                pointsArray = initData.split("\\*");
                firstPoint = pointsArray[0].split("\\$");
                secondPoint = pointsArray[1].split("\\$");
                thirdPoint = pointsArray[2].split("\\$");
                fourthPoint = pointsArray[3].split("\\$");
                double p1[]= convertToDouble(firstPoint);
                double p2[]=  convertToDouble(secondPoint);
                double p3[]=  convertToDouble(thirdPoint);
                double p4[]=   convertToDouble(fourthPoint);
                fillLatLng(p1,p2,p3,p4); // assigning all these points in one array to calculate that array in the "ray casting algorithm"
                workingOnPolygon = true; // assigned true so (onlocationresult) checks what shape to listen to
                Toast.makeText(this, "poly data", Toast.LENGTH_SHORT).show();
                Log.e("Working on : ","POLYGON");
            }
            if (initData.contains("#")) {
                Toast.makeText(this, "circle data passed", Toast.LENGTH_SHORT).show();
                circleArray = initData.split("\\#");
                circleLat= Double.parseDouble(circleArray[0]);
                circleLng= Double.parseDouble(circleArray[1]);
                circleRadius= Double.parseDouble(circleArray[2]);
                Log.e("circle lat : ","Working on circle");

                workingOnCircle=true;
            }
        }catch (Exception e)
        {
            Log.e("Error : onstartCommand ", e.toString());
        }




        getLocation();
        //   showEmployee();
        return START_STICKY;
    }
    private void openDialog() {



    }
    private double[] convertToDouble(String[] point) {
        double[] nums = new double[point.length];
        for (int i = 0; i < nums.length; i++) {
            nums[i] = Double.parseDouble(point[i]);
        }
        return nums;
    }

    LatLng markersLatlng[];

    private void fillLatLng(double[] firstPoint, double[] secondPoint, double[] thirdPoint, double[] fourthPoint) {
        markersLatlng[0]=new LatLng(firstPoint[0],firstPoint[1]);
        markersLatlng[1]=new LatLng(secondPoint[0],secondPoint[1]);
        markersLatlng[2]=new LatLng(thirdPoint[0],thirdPoint[1]);
        markersLatlng[3]=new LatLng(fourthPoint[0],fourthPoint[1]);

    }

    LocationCallback mLocationCallback = new LocationCallback() {

Boolean breaktheloop=false;
        @Override
        public void onLocationResult(LocationResult locationResult) {
               Log.e(TAG, "onLocationResult: got location result.");
                if (locationResult==null){
                    Toast.makeText(MyService.this, "can't get current location", Toast.LENGTH_SHORT).show();
                }
                else {
                    LatLng ll=new LatLng(locationResult.getLastLocation().getLatitude(),locationResult.getLastLocation().getLongitude());
                    float[] distance = new float[2]; // to calculate distance between user and circle
                    Location.distanceBetween(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude(), circleLat, circleLng, distance);

                    if (workingOnPolygon) // we're checking for Polygon
                    {
                        LatLng locLatlng = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                        if (isPointInPolygon(locLatlng, markersLatlng)) // inside the polygon
                        {
                            breaktheloop=true;
                            showNotification("You Arrived","Mabrook");
                            final MediaPlayer mp = MediaPlayer.create(MyService.this, R.raw.alarm);
                            mp.start();
                            final long[] pattern={250,350};
                            vibrator = (Vibrator) MyService.this.getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(pattern,0);
                            enough=true;
                            Toast.makeText(MyService.this, "Youve arrived", Toast.LENGTH_SHORT).show();
                            //  mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                            Toast.makeText(MyService.this, "click on notification to confirm", Toast.LENGTH_SHORT).show();
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //  mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                                    stopSelf();
                                }
                            }, 4000);

                        }
                        else { // user  outside the polygon
                            Toast.makeText(MyService.this, "LAAA2", Toast.LENGTH_SHORT).show();
                        }

                    }
                    if (workingOnCircle){ //this means we're working on circle

                        Toast.makeText(MyService.this, "Distance : "+distance, Toast.LENGTH_SHORT).show();
                        if (distance[0] <= circleRadius) {
                            breaktheloop=true;

                            showNotification("You Arrived","Mabrook");
                            final MediaPlayer mp = MediaPlayer.create(MyService.this, R.raw.alarm);
                            mp.start();
                            final long[] pattern={250,350};
                            vibrator = (Vibrator) MyService.this.getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(pattern, 1);
                            enough=true;

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    stopSelf();
                                }
                            }, 3000);
                            Toast.makeText(MyService.this, "Click on the notification to confirm", Toast.LENGTH_LONG).show();

                        }else{
                            //  Toast.makeText(MyService.this, "back : Outside", Toast.LENGTH_SHORT).show();
                            //outside the circle
                            Log.e("Still outside",".............................");
                            Toast.makeText(MyService.this, "OUTSIDEEE", Toast.LENGTH_SHORT).show();
                        }
                    }}
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
            // stopSelf();
            return;
        }

        Log.e(TAG, "getLocation: getting location information.");
        mFusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, mLocationCallback,Looper.myLooper()); // Looper.myLooper tells this to repeat forever until thread is destroyed
    }
//ss




    private boolean isPointInPolygon(LatLng tap, LatLng[] vertices) {

        int intersectCount = 0;
        int h=0; // for the while loop to increase
        while (h<vertices.length-1)
        {
            if ((rayCastIntersect(tap, vertices[h], vertices[h + 1]))==true) { //rayCastIntersect(tap, vertices[h], vertices[h + 1])
                intersectCount++;
            }
            h++; // increasing normal loop
        }

        return ((intersectCount % 2) == 1); // odd = inside, even = outside;

    }
    boolean rayCastIntersect(LatLng tap, LatLng vertA, LatLng vertB) {

        double aY = vertA.latitude;
        double bY = vertB.latitude;
        double aX = vertA.longitude;
        double bX = vertB.longitude;
        double pY = tap.latitude;
        double pX = tap.longitude;

        if ((aY > pY && bY > pY) || (aY < pY && bY < pY)
                || (aX < pX && bX < pX)) {
            return false; // a and b can't both be above or below pt.y, and a or
            // b must be east of pt.x
        }

        double m = (aY - bY) / (aX - bX); // Rise over run
        double bee = (-aX) * m + aY; // y = mx + b
        double x = (pY - bee) / m; // algebra is neat!

        return x > pX;
    }

    LocationRequest mLocationRequest;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // make accurace to high
        mLocationRequest.setInterval(1000); // update my location every 1 second
        if (ActivityCompat.checkSelfPermission(MyService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MyService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location==null){
            Toast.makeText(MyService.this, "can't get current location", Toast.LENGTH_SHORT).show();
        }
        else {
            LatLng ll=new LatLng(location.getLatitude(),location.getLongitude());
            float[] distance = new float[2]; // to calculate distance between user and circle
            Location.distanceBetween(location.getLatitude(), location.getLongitude(), circleLat, circleLng, distance);

            if (workingOnPolygon) // we're checking for Polygon
            {
                LatLng locLatlng = new LatLng(location.getLatitude(), location.getLongitude());
                if (isPointInPolygon(locLatlng, markersLatlng)) // inside the polygon
                {
                  //  breaktheloop=true;
                    showNotification("You Arrived","Mabrook");
                    final MediaPlayer mp = MediaPlayer.create(MyService.this, R.raw.alarm);
                    mp.start();
                    final long[] pattern={250,350};
                    vibrator = (Vibrator) MyService.this.getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(pattern,0);
                    enough=true;
                    Toast.makeText(MyService.this, "Youve arrived", Toast.LENGTH_SHORT).show();
                    //  mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                    Toast.makeText(MyService.this, "click on notification to confirm", Toast.LENGTH_SHORT).show();
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //  mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                            stopSelf();
                        }
                    }, 4000);

                }
                else { // user  outside the polygon
                    Toast.makeText(MyService.this, "LAAA2", Toast.LENGTH_SHORT).show();
                }

            }
            if (workingOnCircle){ //this means we're working on circle

                Toast.makeText(MyService.this, "Distance : "+distance, Toast.LENGTH_SHORT).show();
                if (distance[0] <= circleRadius) {
                   // breaktheloop=true;

                    showNotification("You Arrived","Mabrook");
                    final MediaPlayer mp = MediaPlayer.create(MyService.this, R.raw.alarm);
                    mp.start();
                    final long[] pattern={250,350};
                    vibrator = (Vibrator) MyService.this.getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(pattern, 1);
                    enough=true;

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            stopSelf();
                        }
                    }, 3000);
                    Toast.makeText(MyService.this, "Click on the notification to confirm", Toast.LENGTH_LONG).show();

                }else{
                    //  Toast.makeText(MyService.this, "back : Outside", Toast.LENGTH_SHORT).show();
                    //outside the circle
                    Toast.makeText(MyService.this, "OUTSIDEEE", Toast.LENGTH_SHORT).show();
                }
            }}
    }

    GoogleApiClient mGoogleApiClient;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(MyService.this);

        mGoogleApiClient = new GoogleApiClient.Builder(MyService.this).addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

    }
}
