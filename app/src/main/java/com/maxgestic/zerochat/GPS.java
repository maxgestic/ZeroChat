package com.maxgestic.zerochat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.concurrent.Executor;

public class GPS extends Service {

    FusedLocationProviderClient locationProviderClient;

    DocumentReference docRef;

    LocationRequest locationRequest;

    String userID;

    LocationCallback locationCallback;

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        userID = intent.getStringExtra("userID");

        Log.d("TEST", userID);

        docRef = FirebaseFirestore.getInstance().collection("users").document(userID);

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationProviderClient.getLastLocation()
                .addOnSuccessListener(getApplicationContext().getMainExecutor(), location -> {
                    if (location != null){

                        GeoPoint geo = new GeoPoint(location.getLatitude(), location.getLongitude());

                        docRef.update("location", geo);

                    }
                });

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    GeoPoint geo = new GeoPoint(location.getLatitude(), location.getLongitude());
                    docRef.update("location", geo);
                }
                super.onLocationResult(locationResult);
            }
        };

        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
        locationProviderClient.removeLocationUpdates(locationCallback);
        // stopping the process
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
