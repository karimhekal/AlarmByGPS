package com.example.maptest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyReceiver extends BroadcastReceiver {
//d

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub

        int datapassed = intent.getIntExtra("DATAPASSED", 0);
        String orgData = intent.getStringExtra("DATA_BACK");

        Toast.makeText(context,
                "Triggered by Service!\n"
                        + "Data passed: " + String.valueOf(datapassed) + "\n"
                        + "original Data: " + orgData,
                Toast.LENGTH_LONG).show();

    }
}