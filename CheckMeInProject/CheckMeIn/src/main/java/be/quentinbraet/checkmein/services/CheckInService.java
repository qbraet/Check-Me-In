/*
 * CheckInService.java is part of Check Me In
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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.GeofencingApi;
import com.google.android.gms.location.LocationServices;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import be.quentinbraet.checkmein.Config;
import be.quentinbraet.checkmein.R;
import be.quentinbraet.checkmein.models.FenceInfo;
import be.quentinbraet.checkmein.models.FoursquareAccount;
import be.quentinbraet.checkmein.ui.activities.HistoryActivity_;
import be.quentinbraet.checkmein.utils.StorageUtils;
import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.Checkin;
import fi.foyt.foursquare.api.entities.CompactVenue;

import static be.quentinbraet.checkmein.utils.LogUtils.LOGD;
import static be.quentinbraet.checkmein.utils.LogUtils.LOGE;
import static be.quentinbraet.checkmein.utils.LogUtils.makeLogTag;

/**
 * Created by quentin on 27/10/13.
 */
@EService
public class CheckInService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    protected FoursquareApi api = new FoursquareApi(Config.CLIENT_ID, Config.CLIENT_SECRET, Config.CLIENT_CALLBACK);

    @SystemService
    AlarmManager alarmManager;

    private Intent intent;

    public static final String TAG = makeLogTag(CheckInService.class);

    private GoogleApiClient locationClient;
    private FusedLocationProviderApi fusedLocationProviderApi;
    private GeofencingApi geofencingapi;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent,flags,startId);
        api.setoAuthToken(FoursquareAccount.getInstance().getToken(this));
        this.intent = intent;
        if(servicesConnected()){
            locationClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            locationClient.connect();
        }
        return START_NOT_STICKY;
    }

    @Override
    @Background
    public void onConnected(Bundle bundle) {
        if(intent != null && intent.getExtras() != null) {

            // Now we can do the actual logic
            CompactVenue venue = (CompactVenue) intent.getExtras().getSerializable("venue");

            // prevent crashes (but should not happen)
            if(venue == null) return;
            FenceInfo info = StorageUtils.getInstance().loadFenceInfo(this, venue.getId());
            String broadcast = null;
            if(info.isFacebook() && info.isTwitter()){
                broadcast = "public,facebook,twitter";
            }else if(info.isFacebook()){
                broadcast = "public,facebook";
            }else if(info.isTwitter()){
                broadcast = "public,twitter";
            }

            //String venueId, String venue, String shout, String broadcast, String ll, Double llAcc, Double alt, Double altAcc)
            try {

                fusedLocationProviderApi = LocationServices.FusedLocationApi;
                geofencingapi = LocationServices.GeofencingApi;


                Location location = fusedLocationProviderApi.getLastLocation(locationClient);

                // keep these objects, we want to know if it was a success
                Result<Checkin> res;

                // do actual checkin, with or without location
                if(location != null){
                    String ll = location.getLatitude() + "," + location.getLongitude();
                    Double llAcc = (double) location.getAccuracy();
                    Double alt = location.getAltitude();

                    res = api.checkinsAdd(venue.getId(), null, null, broadcast, ll, llAcc, alt, llAcc);
                }else{
                    res = api.checkinsAdd(venue.getId(), null, null, broadcast, null, null, null, null );
                }

                // check for errors
                FoursquareAccount.getInstance().checkAuth(res, this);
                String error = res.getMeta().getErrorDetail();
                if(error != null && !error.equals("")){
                    EasyTracker.getInstance(this).send(MapBuilder
                            .createEvent("Exception", "checkin call", error, null)
                            .build()
                    );
                    locationClient.disconnect();
                    return;
                }
                LOGD(TAG, res.getMeta().getErrorDetail());

                // Create pending intent for notification
                Intent toCheckins = new Intent(this, HistoryActivity_.class);
                PendingIntent toCheckinsPending = PendingIntent.getActivity(this, 0, toCheckins, PendingIntent.FLAG_ONE_SHOT);

                // Build actual notification
                Notification.Builder builder =
                        new Notification.Builder(this)
                                .setSmallIcon(R.drawable.ic_action_accept)
                                .setContentTitle(venue.getName())
                                .setContentIntent(toCheckinsPending)
                                .setContentText(getResources().getString(R.string.checked_in_at) + " " + venue.getName());

                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                // Add action to notification (only for > Jelly Bean)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    notificationManager.notify(venue.getId().hashCode(), builder.build());
                }else{
                    notificationManager.notify(venue.getId().hashCode(), builder.getNotification());
                }

                EasyTracker.getInstance(this).send(MapBuilder
                        .createEvent("control", "checkin", "", null)
                        .build()
                );

                // Ok, now remove the geofence, we're about to save power
                List<String> toRemove = new ArrayList<String>();
                toRemove.add(venue.getId());
                PendingResult<Status> status = geofencingapi.removeGeofences(locationClient, toRemove);
                status.setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // Ok fine, but we have to reactivate it after x hours
                        PendingIntent intent = PendingIntent.getService(CheckInService.this, 0, new Intent(CheckInService.this, RefreshGeofencesService_.class), PendingIntent.FLAG_UPDATE_CURRENT);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + Config.DONT_REPEAT_HOURS * 60 * 60 * 1000 , intent);

                        // and finally, disconnect
                        locationClient.disconnect();
                    }
                }, 10, TimeUnit.SECONDS);


            } catch (FoursquareApiException e) {
                toast(e.getMessage());
                LOGE(TAG,e.getMessage(), e);
                locationClient.disconnect();
            }
        }else{
            locationClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Shut down this service
        stopSelf();
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

    @UiThread
    public void toast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
