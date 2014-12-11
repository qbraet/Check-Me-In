/*
 * TransitionService.java is part of Check Me In
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
import android.app.PendingIntent;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;

import java.util.List;

import be.quentinbraet.checkmein.Config;
import be.quentinbraet.checkmein.models.FenceInfo;
import be.quentinbraet.checkmein.utils.StorageUtils;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.Checkin;
import fi.foyt.foursquare.api.entities.CheckinGroup;
import fi.foyt.foursquare.api.entities.CompactVenue;

import static be.quentinbraet.checkmein.utils.LogUtils.LOGD;
import static be.quentinbraet.checkmein.utils.LogUtils.LOGE;
import static be.quentinbraet.checkmein.utils.LogUtils.makeLogTag;

/**
 * Created by quentin on 26/10/13.
 */
@EService
public class TransitionService extends BaseIntentService {

    public static final String TAG = makeLogTag(TransitionService.class);

    @SystemService
    AlarmManager alarmManager;

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        GeofencingEvent geoFenceEvent = GeofencingEvent.fromIntent(intent);

        // First check for errors
        if (geoFenceEvent.hasError()) {
            // Get the error code with a static method
            int errorCode = geoFenceEvent.getErrorCode();
            // Log the error
            LOGE(TAG,
                    "Location Services error: " +
                            Integer.toString(errorCode));
            /*
             * You can also send the error code to an Activity or
             * Fragment with a broadcast Intent
             */
        /*
         * If there's no error, get the transition type and the IDs
         * of the geofence or geofences that triggered the transition
         */
        } else {
            // Get the type of transition (entry or exit)
            int transitionType =
                    geoFenceEvent.getGeofenceTransition();
            // Test that a valid transition was reported
            if (
                    (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
                            ||
                            (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT)
                            || (transitionType == Geofence.GEOFENCE_TRANSITION_DWELL)
                    ) {
                List<Geofence> triggerList = geoFenceEvent.getTriggeringGeofences();

                String[] triggerIds = new String[triggerList.size()];

                for (int i = 0; i < triggerIds.length; i++) {
                    // Store the Id of each geofence
                    triggerIds[i] = triggerList.get(i).getRequestId();
                }
                /*
                 * At this point, you can store the IDs for further use
                 * display them, or display the details associated with
                 * them.
                 */

                CompactVenue venue = StorageUtils.getInstance().loadCompactVenue(this, triggerIds[0]);

                Intent checkinIntent = new Intent(this, CheckInService_.class);
                checkinIntent.putExtra("venue", venue);
                if(venue == null) {
                    LOGE(TAG, "Nullpointer prevented for venue in transitionservice!");
                    return;
                }
                PendingIntent pending = PendingIntent.getService(this, 0, checkinIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                if(transitionType == Geofence.GEOFENCE_TRANSITION_DWELL){
                    LOGD(TAG, "Transition DWELL " + venue.getName());
                }else{
                    LOGD(TAG, "Transition other " + venue.getName());
                }

                if(transitionType == Geofence.GEOFENCE_TRANSITION_DWELL){
                    try {
                        Result<CheckinGroup> result = api.usersCheckins("self", 1, 0, null, null);
                        String error = result.getMeta().getErrorDetail();
                        if(error != null && !error.equals("")){
                            toast(error);
                            LOGE(TAG, "FSQ API last checkin: error: " + error);
                        }else if(result.getResult() == null || result.getResult().getItems().length == 0){
                            // No last checkins => checkin here
                            LOGD(TAG, "FSQ API last checkin: list empty");
                            startService(checkinIntent);
                        }else{
                            Checkin checkin = result.getResult().getItems()[0];

                            LOGD(TAG, "FSQ API last checkin: " + checkin.getVenue().getName() + " at " + checkin.getCreatedAt());
                            if(!venue.getId().equals(checkin.getVenue().getId())){
                                // Last checkin was at another venue, checkin

                                // if we track the venue of the last checkin, we have to reactivate the geofence now
                                CompactVenue lastVenue = StorageUtils.getInstance().loadCompactVenue(this, checkin.getId());
                                FenceInfo lastFence = StorageUtils.getInstance().loadFenceInfo(this, checkin.getId());

                                if(lastVenue != null && lastFence != null){
                                    Intent reactivate = new Intent(this, AddGeofence_.class);
                                    reactivate.putExtra("venue", lastVenue);
                                    reactivate.putExtra("fence", lastFence);
                                    startService(reactivate);
                                }

                                // actual checkin
                                startService(checkinIntent);

                                LOGD(TAG, "CHECKIN AT " + venue.getName() + " time:  " +System.currentTimeMillis());
                            }else if(checkin.getCreatedAt() <= ((System.currentTimeMillis()/1000) - Config.DONT_REPEAT_HOURS *60*60) ){
                                // last checkin was here, must be older then 20h
                                startService(checkinIntent);
                                LOGD(TAG, "CHECKIN AT " + venue.getName() + " time:  " +System.currentTimeMillis());
                            }
                        }
                    } catch (FoursquareApiException e) {
                        toast(e.getMessage());
                    }
                }
            } else {
                // An invalid transition was reported
                LOGE(TAG, "Geofence transition error: " + transitionType);
            }
        }
    }
}
