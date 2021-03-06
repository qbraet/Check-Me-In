/*
 * AddGeofence.java is part of Check Me In
 *
 * Copyright (c) 2014 Quentin Braet
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: quentinbraet@gmail.com
 */

package be.quentinbraet.checkmein.services;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingApi;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationStatusCodes;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import be.quentinbraet.checkmein.models.FenceInfo;
import be.quentinbraet.checkmein.utils.GeofenceUtils;
import fi.foyt.foursquare.api.entities.CompactVenue;

import static be.quentinbraet.checkmein.utils.LogUtils.LOGD;
import static be.quentinbraet.checkmein.utils.LogUtils.LOGE;
import static be.quentinbraet.checkmein.utils.LogUtils.makeLogTag;

/**
 * Created by quentin on 13/12/13.
 */
@EService
public class AddGeofence extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<Status> {

    public static final String TAG = makeLogTag(RefreshGeofencesService.class);

    private GoogleApiClient locationClient;

    @Bean
    GeofenceUtils geofenceUtils;

    private Intent intent;

    private CompactVenue venue;

    private FenceInfo fence;
    private GeofencingApi geofencingapi;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent,flags,startId);

        this.intent = intent;
        this.venue = (CompactVenue) intent.getSerializableExtra("venue");
        this.fence = (FenceInfo) intent.getSerializableExtra("fence");

        if(venue != null && fence != null){

            if(servicesConnected()){
                locationClient = new GoogleApiClient.Builder(this)
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
                locationClient.connect();
            }

        }
        return START_NOT_STICKY;
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Now we can do the actual logic

        List<Geofence> fences = new ArrayList<Geofence>();
        fences.add(fence.toGeofence(venue));

        geofencingapi = LocationServices.GeofencingApi;

        if(locationClient != null && locationClient.isConnected() && fences.size() > 0){
            GeofencingRequest request = new GeofencingRequest.Builder()
                    .addGeofences(fences)
                    .build();
            PendingResult<Status> status = geofencingapi.addGeofences(locationClient, request, geofenceUtils.getTransitionPendingIntent(this));
            status.setResultCallback(this, 10, TimeUnit.SECONDS);

            // disconnect will be done in resultlistener
        }else if(locationClient != null && locationClient.isConnected()){
            locationClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Shut down this service
        LOGE(TAG, "Geofence refresher shut down, connection with google play services failed");
        stopSelf();
    }

    protected boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            LOGD(TAG,
                    "Google Play services is available.");
            // Continue
            return true;
        } else {
            // Google Play services was not available for some reason
            return false;
        }
    }

    @Override
    public void onResult(Status status) {
        if(LocationStatusCodes.SUCCESS == status.getStatusCode()){
            LOGD(TAG, "Geofences refreshed :)");
        }else{
            LOGE(TAG, "Geofences not refreshed: errorcode " + status.getStatusCode());

            EasyTracker.getInstance(this).send(MapBuilder
                            .createEvent("exception", "AddGeofence", "Geofences not refreshed: errorcode " + status.getStatusCode(), null)
                            .build()
            );
        }
        // disconnect from services
        if(servicesConnected()){
            locationClient.disconnect();
        }
        stopSelf();
    }
}

