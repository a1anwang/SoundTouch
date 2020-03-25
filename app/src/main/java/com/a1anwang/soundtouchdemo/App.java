package com.a1anwang.soundtouchdemo;

import android.app.Application;

import androidx.multidex.MultiDex;

import com.blankj.utilcode.util.Utils;


public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        MultiDex.install(this);
    }
}
