package com.example.maptest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * Created by Karim Hekal on 2018-06-30.
 */

public class Splash extends AppCompatActivity {
    ImageView image;
    Animation animation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        image = findViewById(R.id.foodyLogo);

        getSupportActionBar().hide();
        animation = AnimationUtils.loadAnimation(this, R.anim.fade);
        image.setAnimation(animation);

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(1500);
                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(i);
                    finish();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();


    }
}