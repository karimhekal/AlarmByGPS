package com.example.maptest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    Circle circle;
    ArrayList<Marker> markers= new ArrayList<Marker>();

    static final int POLYGON_POINTS=4;
    Polygon shape;
    GoogleMap mGoogleMap;
    boolean enough=false;
    boolean userChoosedLocation=false;
    GoogleMap m;
    CameraUpdate update;
    float lat = 30, lng = 0;
    View mView;
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


    String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/";
    String[] employeeField;
    String line = "";
    double dradius;
    double dlat;
    double dlong;
    private void showEmployee() {
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(path+"circlelocation.txt"));
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
            LatLng l=new LatLng(dlat,dlong);

            Toast.makeText(mContext, "RESULTTT : "+dradius, Toast.LENGTH_SHORT).show();
            drawCircle(l);
            fileInputStream.close();
            bufferedReader.close();
        } catch (Exception e) {
            Log.e("readFromFile", e.toString());
        }
    }

    private void appendFile(LatLng latLng,String fileName) {
        File file = new File(path+fileName);
        try {
            if (!file.exists())
                file.createNewFile();
            // file.delete();
            file.createNewFile();
            FileOutputStream fileOutputStream =new FileOutputStream(file);
            if (latLng !=null) {
                try {
                    Log.e("radius : ", String.valueOf(circle.getRadius()));
                    fileOutputStream.write((circle.getRadius() + "$" + latLng.latitude + "$" + latLng.longitude + System.getProperty("line.separator")).getBytes());

                }catch (Exception e )
                {
                    e.printStackTrace();
                }
            }} catch (IOException e) {
            Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onResume() {
        clicked(saveCircleLocation);
        showEmployee();
        userChoosedLocation=false;
        Intent i= new Intent(getActivity().getBaseContext(),MyService.class);
        getActivity().stopService(i);
        super.onResume();
        //getActivity().stopService(new Intent(getActivity(), MyService.class));
    }

    @Override
    public void onPause() {
        super.onPause();

        try {
            Intent i = new Intent(mContext,MyService.class);
            if (userChoosedLocation==true){ // start the service only if the user choosed a location
                getActivity().startService(i);}
        }catch (Exception e)
        {
            Log.e("Fragment : ",e.getMessage());
        }
        removeEveryThing();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.map_fragment, container, false);
        clearMarkers=mView.findViewById(R.id.clear_markers);
        setRadius=mView.findViewById(R.id.set_radius);
        vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        editText = mView.findViewById(R.id.edit_text);
        polygonRadio=mView.findViewById(R.id.polygon);
        circleRadio=mView.findViewById(R.id.circle);
        enough=false;

        clearMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeEveryThing();
            }
        });
        polygonRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "Draw a polygon of 4 lines on map", Toast.LENGTH_LONG).show();
                p=true;
                c=false;
            }
        });
        circleRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "Click on map to draw circle", Toast.LENGTH_LONG).show();
                c=true;
                p=false;
            }
        });
        setRadius.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                try{
                    radius=Integer.valueOf(editText.getText().toString());
                    magicButton();
                    mGoogleMap.animateCamera(update);
                }catch (Exception e){
                    Toast.makeText(mContext, "Configuring", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return mView;
    }

    void addMarker(float lat, float lng) {
        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title("sss").snippet("i hope to"));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            //  getActivity().stopService(new Intent(getActivity(), MyService.class));

            mMapView = (MapView) mView.findViewById(R.id.map);
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
    private void goToLocationZoom(double lat1, double lng1, float zoom) {
    if (count<2){  //because this function is called one time before i start the onLocationChanged
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
        goToLocationZoom(lat, lng, 5);
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                saveCircleLocation=latLng;
                appendFile(latLng,"circleLocation.txt");
                enough=false;
                if (p==true)
                {
                    userChoosedLocation=true;

                    if (circleMarker==true) {
                        removeEveryThing();
                        circleMarker=false;
                    }
                    polyClicked(latLng);

                }
                else if (c==true)
                {
                    userChoosedLocation=true;
                    removeEveryThing();
                    clicked(latLng);


                }
                else {
                    Toast.makeText(mContext, "Select Circle or Polygon first", Toast.LENGTH_SHORT).show();
                }
                appendFile(latLng,"circle.txt"); // eb2a ghayyr esmaha

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

    }

    private void removeEveryThing() {

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

    private void polyClicked(LatLng latLng){
        if (markers.size()==POLYGON_POINTS)
        {
            //marker.getPosition();
            Toast.makeText(mContext, "Polygon is finished", Toast.LENGTH_SHORT).show();
            removeEveryThing();
        }
        MarkerOptions options=new MarkerOptions().position(latLng).title("point");
        markers.add(mGoogleMap.addMarker(options));
        if (markers.size()==POLYGON_POINTS)
        {
            drawPolygon();
        }
    }

    private void drawPolygon() {
        PolygonOptions options=new PolygonOptions()
                .fillColor(0x3000ffff)
                .strokeWidth(3)
                .strokeColor(Color.RED);

        for ( int i=0;i<POLYGON_POINTS;i++)
        {
            options.add(markers.get(i).getPosition());

        }
        shape=mGoogleMap.addPolygon(options);

    }
    boolean circleMarker;
    private void clicked(LatLng latLng) {
        try {
            circleMarker = true;
            circle = drawCircle(latLng);
            Geocoder gc = new Geocoder(mContext);
            List<Address> list = null;
            try {
                list = gc.getFromLocation(latLng.latitude, latLng.longitude, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (marker != null) {
                marker.remove();
            }
            marker = mGoogleMap.addMarker(new MarkerOptions().position(latLng));

        }catch (Exception e)
        {
            Log.e("Error : ",e.toString());
        }
    }



    private Circle drawCircle(LatLng latLng) {
        if (circle!=null)
        {
            circle.remove();
        }
        CircleOptions options=new CircleOptions().center(latLng).radius(radius).fillColor(0x33FF0000).strokeColor(Color.BLUE).strokeWidth(3);
        return mGoogleMap.addCircle(options);
    }


    private void magicButton() {
        String location = editText.getText().toString();
        Geocoder gc = new Geocoder(getContext());
        List<Address> list = null;
        try {
            list = gc.getFromLocationName(location, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Address address = list.get(0);
        String locality = address.getLocality();
        //Toast.makeText(mContext, locality, Toast.LENGTH_SHORT).show();
        double lat = address.getLatitude();
        double lng = address.getLongitude();
        goToLocationZoom(lat, lng, 12.5f);
        //MapsInitializer.initialize(mContext);
//        if (marker != null) {
//            marker.remove();
//        }
//        marker = mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title(locality).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher)));

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


                goToLocationZoom(location.getLatitude(),location.getLongitude(),12.5f);
                LatLng ll=new LatLng(location.getLatitude(),location.getLongitude());

                update=CameraUpdateFactory.newLatLngZoom(ll,15); // update my location every 1 second
                float[] distance = new float[2]; // to calculate distance between user and circle
                if (circleMarker==true) {// to make sure that we're using the circle not polygon

                    Location.distanceBetween(location.getLatitude(), location.getLongitude(), circle.getCenter().latitude, circle.getCenter().longitude, distance);

                    if (distance[0] <= circle.getRadius()) {
                        if (enough==false) { // make the vibration work one time 3ashan lama bey update el location tany byla2y en el user lessa fel circle fa byshaghal el vibration kol shwya ma3 kol update law el condition da msh mawgod
                            openDialog();
                            //vibrator.vibrate(pattern, 1);
                            //  Toast.makeText(mContext, "Inside the circle", Toast.LENGTH_SHORT).show();
                            enough = true;

                        }}else {
                        enough=false;
                        //   Toast.makeText(mContext, "Outside", Toast.LENGTH_SHORT).show();
                        //outside the circle
                    }


                }
                // Location.distanceBetween(location.getLatitude(), location.getLongitude(), shape.get().latitude, pol.gets().longitude, distance);
                // Toast.makeText(mContext, "Normal : "+ location.getLatitude(), Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.e("Error : ",e.getMessage());
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

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


}
