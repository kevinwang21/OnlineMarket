package com.example.onlinemarket;

import com.facebook.drawee.backends.pipeline.Fresco;

import android.app.Application;

public class MyApp extends Application{
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
    }
}
