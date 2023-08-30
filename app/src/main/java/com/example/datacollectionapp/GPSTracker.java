package com.example.datacollectionapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class GPSTracker extends Service implements LocationListener {
    LocationManager LM = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    public static String mLat; //= "-9999";
    public static String mLong; //= "-9999";
    public static String mSpeed; //= "-9999";
    public static String mAltitude;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        LM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        String result = "";
        // TODO Auto-generated method stub
        mLat = String.valueOf(location.getLatitude());
        mLong = String.valueOf(location.getLongitude());
        mSpeed = String.valueOf(location.getSpeed());
        mAltitude = String.valueOf(location.getAltitude());
        result = "Location (success): " +
                mLat +
                mLong;
        Log.d("LOCATION TASK: ", "(0) getCurrentLocation() result: " + result);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager =
                    (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, this);
        }

        return Service.START_STICKY;
    }
}
