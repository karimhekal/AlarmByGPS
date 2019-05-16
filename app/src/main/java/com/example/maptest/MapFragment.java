package com.example.maptest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;

public class MapFragment extends Fragment implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    Circle circle;
    ArrayList<Marker> markers= new ArrayList<>();

    static final int POLYGON_POINTS=4;
    Polygon shape;
    GoogleMap mGoogleMap;
    boolean enough=false;
    boolean userChoosedCircle=false;
    GoogleMap m;
    CameraUpdate update;
    float lat = 30, lng = 0;
    View mView;

    LatLng[] markersLatLng;
    Vibrator vibrator;
    LatLng saveCircleLocation;
    Marker marker;
    MapView mMapView;
    GoogleApiClient mGoogleApiClient;
    //Button zoom;
    int radius=1000;
    EditText editText;
    RadioButton polygonRadio,circleRadio;
    RadioGroup rg;
    boolean c=false,p=false;
    LocationRequest mLocationRequest;
    Button setRadius,clearMarkers;




    @Override
    public void onResume() {
        //clicked(saveCircleLocation);
        userChoosedPoly=false;
        userChoosedCircle=false;
        Intent i= new Intent(getActivity().getApplicationContext(),MyService.class);
        getActivity().stopService(i);
        super.onResume();
      }
    boolean userChoosedPoly=false;

    MyReceiver myReceiver;
    String[] markersField;

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public void onPause() {
        super.onPause();
        vibrator.cancel();
        try {

            if (userChoosedPoly) { //start the service only if user checked polygon
                String point1 = String.valueOf(markersLatLng[0].latitude) + "$" + String.valueOf(markersLatLng[0].longitude); //putting $ between them so i can split the lat and lng in the service
                String point2 = String.valueOf(markersLatLng[1].latitude) + "$" + String.valueOf(markersLatLng[1].longitude);
                String point3 = String.valueOf(markersLatLng[2].latitude) + "$" + String.valueOf(markersLatLng[2].longitude);
                String point4 = String.valueOf(markersLatLng[3].latitude) + "$" + String.valueOf(markersLatLng[3].longitude);

                String allPoints = point1 + "*" + point2 + "*" + point3 + "*" + point4; // seperating them by * so i can split the points in the service
                // now "allPoints" contains all data of the 4 points , it will be sent as an intent to the service
                myReceiver = new MyReceiver();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(MyService.MY_ACTION);
                mContext.registerReceiver(myReceiver, intentFilter);
                Intent intent = new Intent(mContext, com.example.maptest.MyService.class);
                intent.putExtra("DATA", allPoints); //allpoints contains all data of 4 points seperated by * , and the latlng of every point is seperated by $
                getActivity().startForegroundService(intent);
                mContext.unregisterReceiver(myReceiver);
            }
            Intent i = new Intent(mContext,MyService.class);
            if (userChoosedCircle){ // start the service only if the user choosed a location

                String circleLat= String.valueOf(saveCircleLocation.latitude);
                String circleLng = String.valueOf(saveCircleLocation.longitude);
                String circleRadius= String.valueOf(radius);
                myReceiver = new MyReceiver();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(MyService.MY_ACTION);
                mContext.registerReceiver(myReceiver, intentFilter);
                Intent intent = new Intent(mContext, com.example.maptest.MyService.class);
                intent.putExtra("DATA", circleLat+"#"+circleLng+"#"+circleRadius); // putting $ between them so i can split them in the service and use them
                getActivity().startService(intent);
                mContext.unregisterReceiver(myReceiver);

            }
        }catch (Exception e)
        {
            Log.e("Fragment : ",e.getMessage());
        }
        removeAllMarkers();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        markersLatLng= new LatLng[4];
        mView = inflater.inflate(R.layout.map_fragment, container, false);
        clearMarkers=mView.findViewById(R.id.clear_markers);
        setRadius=mView.findViewById(R.id.set_radius);
        vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        editText = mView.findViewById(R.id.edit_text);
        polygonRadio=mView.findViewById(R.id.polygon);
        circleRadio=mView.findViewById(R.id.circle);
        circleRadio.setChecked(true);
        c=true;
        enough=false;
        clearMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeAllMarkers();
            }
        });
        polygonRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "Draw 4 points in clockwise ", Toast.LENGTH_LONG).show();
                p=true; // to indicate that we're working on a polygon
                c=false; // to make sure that we're not working on a circle
                userChoosedCircle=false; // to make sure it's false
            }
        });
        circleRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "Click on map to draw circle", Toast.LENGTH_LONG).show();
                c=true; // c for circle
                p=false; // p for polygon
                userChoosedPoly=false; // to make sure that this variable is false
            }
        });
        setRadius.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                try{
                    radius=Integer.valueOf(editText.getText().toString());
                }catch (Exception e){
                    Toast.makeText(mContext, "Configuring", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            mMapView = mView.findViewById(R.id.map);
            if (mMapView != null) {
                mMapView.onCreate(null);
                mMapView.onResume();
                mMapView.getMapAsync(this);
            }
        } catch (Exception e) {
            Log.e("OnViewCreated : ", e.toString());
        }
    }

    int count=0;
    private void goToLocationAndZoom(double lat1, double lng1, float zoom) {
        if (count<2){  //this variable counts how many times that "onLocationChanged" function is called , since it doesn't give you user location the first time it's called , usually the second time, So "count" only counts how many the "onLocationChanged" was called , and when it's called 2 times , this condition is applied
            LatLng ll = new LatLng(lat1, lng1);
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
            mGoogleMap.moveCamera(update);
            count++;}
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(getContext());

        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        goToLocationAndZoom(lat, lng, 5);
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                saveCircleLocation=latLng;
                enough=false;
                if (p) // "p" means if user checked polygon
                {
                    userChoosedPoly=true;

                    if (circleMarker||(markers.size()==POLYGON_POINTS)) {
                        removeAllMarkers();
                        circleMarker=false;
                    }
                    if (markers.size()!=POLYGON_POINTS) {
                        polyClicked(latLng);
                    }
                }
                else if (c) // "c" means if user checked circle
                {

                    userChoosedCircle=true;
                    removeAllMarkers();
                    clicked(latLng);
                }
                else {
                    Toast.makeText(mContext, "Select Circle or Polygon first", Toast.LENGTH_SHORT).show();
                }

            }
        });

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);

        mGoogleApiClient = new GoogleApiClient.Builder(mContext).addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        if (mMapView != null &&
                mMapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mMapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 60, 60);
        }}

    private void removeAllMarkers() {
        i=0;// to start polygons from points 1 again because i is the index of the array // check usage of i if you don't get it
        if (marker!=null){
            marker.remove();
            marker=null;
        }
        if (circle!=null)
        {circle.remove();
            circle=null;
        }
        if (markers!=null){
            for (Marker marker:markers)
            {



                marker.remove();
            }
            markers.clear();
            marker=null;
        }
        if (shape!=null)
        {
            shape.remove();
            shape=null;
        }
    }

    int i=0;
    private void polyClicked(LatLng latLng){ // this is called everytime the user clickes on map while drawing the polygon
        if (markers.size()==POLYGON_POINTS)
        {
            //marker.getPosition();
            Toast.makeText(mContext, "Polygon is finished", Toast.LENGTH_SHORT).show();
            removeAllMarkers();
        }
        MarkerOptions options=new MarkerOptions().position(latLng).title("point");


        markers.add(mGoogleMap.addMarker(options));


        if (markers.size() == POLYGON_POINTS) {
            drawPolygon();
            markersLatLng[i] = latLng; // stroing the last point poisition in the array that stores positions of points
            i=0; // to begin from start of the array again for the next polygon to draw
            en=false; // 3ashan ye5osh fel opendialog tany lama y3ml check 3al polygon fel onlocationchanged  //spaghetti awy hhh
        } else {
            markersLatLng[i] = latLng; // storing latlng of every click to an array that contains latlng of all points of the polygon
            i++; // increasing this every time to access next index of the array
        }



    }

    private void drawPolygon() {
        PolygonOptions options=new PolygonOptions()
                .fillColor(0x3000ffff)
                .strokeWidth(3)
                .strokeColor(Color.RED);

        for ( int i=0;i<POLYGON_POINTS;i++)
        {
            options.add(markers.get(i).getPosition()); //accessing the positions of markers and putting them in "options"
        }
        shape=mGoogleMap.addPolygon(options);

    }
    boolean circleMarker;
    private void clicked(LatLng latLng) {
        try {
            circleMarker = true;
            circle = drawCircle(latLng);

            if (marker != null) { // to remove the previous marker in case there's one
                marker.remove();
            }
            marker = mGoogleMap.addMarker(new MarkerOptions().position(latLng)); // adding the marker on map

        }catch (Exception e)
        {
            Log.e("Error 1: ",e.toString());
        }
    }



    private Circle drawCircle(LatLng latLng) {
        if (circle!=null) // to remove the previous circle if there's one
        {
            circle.remove();
        }
        CircleOptions options=new CircleOptions().center(latLng).radius(radius).fillColor(0x33FF0000).strokeColor(Color.BLUE).strokeWidth(3);
        return mGoogleMap.addCircle(options);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // make accurace to high
        mLocationRequest.setInterval(1000); // update my location every 1 second
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
    final long[] pattern={800,400};
    @Override
    public void onLocationChanged(Location location) {
        try{
            //Toast.makeText(mContext, "Running in back: "+location.getLatitude(), Toast.LENGTH_SHORT).show();
            if (location==null){
                Toast.makeText(mContext, "can't get current location", Toast.LENGTH_SHORT).show();
            }
            else {

                if (markers.size()==POLYGON_POINTS) { // this condition checks if we're using polygon
                    LatLng locLatlng = new LatLng(location.getLatitude(), location.getLongitude());
                    if (isPointInPolygon(locLatlng, markersLatLng)) // inside the polygon
                    {
                        if (!en) { // make the vibration work one time 3ashan lama bey update el location tany byla2y en el user lessa fel circle fa byshaghal el vibration kol shwya ma3 kol update law el condition da msh mawgod
                            openDialog();
                            en = true;
                        }

   ////                     Toast.makeText(mContext, "AAAYWA", Toast.LENGTH_SHORT).show();
                    }
                    else { // user outside the polygon
     ////                   Toast.makeText(mContext, "LAAA2", Toast.LENGTH_SHORT).show();
                    }

                    boolean s=isPointInPolygon(locLatlng, markersLatLng);
                    //Toast.makeText(mContext, "true", Toast.LENGTH_SHORT).show();
                }

                goToLocationAndZoom(location.getLatitude(),location.getLongitude(),12.5f);
                LatLng ll=new LatLng(location.getLatitude(),location.getLongitude());
                update=CameraUpdateFactory.newLatLngZoom(ll,15); // update my location every 1 second
                float[] distance = new float[2]; // to calculate distance between user and circle
                if (circleMarker&&circle!=null) {// to make sure that we're using the circle not polygon
                    Location.distanceBetween(location.getLatitude(), location.getLongitude(), circle.getCenter().latitude, circle.getCenter().longitude, distance);
                    if (distance[0] <= circle.getRadius()) {
                        if (!enough) { // make the vibration work one time 3ashan lama bey update el location tany byla2y en el user lessa fel circle fa byshaghal el vibration kol shwya ma3 kol update law el condition da msh mawgod
                            openDialog();
                            //vibrator.vibrate(pattern, 1);
       ////                      Toast.makeText(mContext, "Inside the circle", Toast.LENGTH_SHORT).show();
                            enough = true;
                        }
                    }else
                    {
                        enough=false;
        ////                  Toast.makeText(mContext, "Outside", Toast.LENGTH_SHORT).show();
                        //outside the circle
                    }


                }
                // Location.distanceBetween(location.getLatitude(), location.getLongitude(), shape.get().latitude, pol.gets().longitude, distance);
                // Toast.makeText(mContext, "Normal : "+ location.getLatitude(), Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e("Error 2: ",e.getMessage());
            //Toast.makeText(getActivity(), "ERRRRORRRRR: " +e.toString(), Toast.LENGTH_SHORT).show();
        }
    }


    // mGoogleMap.animateCamera(update);



    public Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext=context;
    }

    private void openDialog() {
        ExampleDialog exampleDialog=new ExampleDialog();
        exampleDialog.show(getFragmentManager(),"Example dialog");
        exampleDialog.setCancelable(true);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    boolean en=false;
    private boolean isPointInPolygon(LatLng tap, LatLng[] vertices) {

        int intersectCount = 0;
        int h=0;
        while (h<vertices.length-1)
        {
            if ((rayCastIntersect(tap, vertices[h], vertices[h + 1]))) { //rayCastIntersect(tap, vertices[h], vertices[h + 1])
                intersectCount++;
            }
            h++;
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




}
