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

public class MainActivity extends FragmentActivity {
    MapFragment mapFragment;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    Intent i;
    String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {



            File file = new File(path+"circle.txt");
            file.delete();

            setContentView(R.layout.activity_main);

            i= new Intent(getBaseContext(),MyService.class);



            mapFragment = new MapFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_place, mapFragment);
            stopService(i);

        }catch (Exception e)
        {
            Log.e(" : ", e.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        stopService(i);
    }

    @Override
    protected void onPause() {
//s
        super.onPause();
    }
//    public void geoLocate(View view) throws IOException {
//        editText=findViewById(R.id.edit_text);
//        String in=editText.getText().toString();
//        mapFragment.execute(in, this);
//
//    }


}
