package com.example.maptest;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import java.io.File;
import java.io.IOException;
import java.util.List;
public class MainActivity extends FragmentActivity { //extend fragment activity so i can hide action bar
    MapFragment mapFragment;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            mapFragment = new MapFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_place, mapFragment);
        }catch (Exception e)
        {
            Log.e("MainActivityOnCreate: ", e.toString());
        }
    }

}
