package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

public class Geofencing implements ResultCallback<Status> {
    private Context context;
    private GoogleApiClient googleApiClient;
    private PendingIntent mGeofencePendingIntent;
    private List<Geofence> list;
    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours
    private static final float GEOFENCE_RADIUS = 50; // 50 meters

    public Geofencing(Context context, GoogleApiClient googleApiClient) {
        this.context = context;
        this.googleApiClient = googleApiClient;
        list = new ArrayList<>();
    }

    // given a PlaceBuffer will create a Geofence object for each Place using Geofence.Builder
    // and add that Geofence to mGeofenceList
    public void updateGeofencesList(PlaceBuffer placeBuffer) {
        for (Place place: placeBuffer) {
            Geofence geofence = new Geofence.Builder().setRequestId(place.getId())
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(place.getLatLng().latitude, place.getLatLng().longitude, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();
            list.add(geofence);
        }
    }

    // uses GeofencingRequest.Builder to return a GeofencingRequest object from the Geofence list
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder request = new GeofencingRequest.Builder();
        request.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        request.addGeofences(list);
        return (request.build());
    }

    private PendingIntent getGeofencePendingIntent() {

        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);

        mGeofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    public void registerAllGeofences() {
        // Check that the API client is connected and that the list has Geofences in it
        if (googleApiClient == null || !googleApiClient.isConnected() ||
               list == null || list.size() == 0) {
            return;
        }
        try {
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            Log.e("Geofencing", securityException.getMessage());
        }
    }

    @Override
    public void onResult(@NonNull Status status) {
        Log.e("onResult", String.format("Error adding/removing geofence : %s",
                status.getStatus().toString()));
    }

    public void unRegisterAllGeofences() {
        if (googleApiClient == null || !googleApiClient.isConnected()) {
            return;
        }
        try {
            LocationServices.GeofencingApi.removeGeofences(
                    googleApiClient,
                    // This is the same pending intent that was used in registerGeofences
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            Log.e("unregister", securityException.getMessage());
        }
    }
}
