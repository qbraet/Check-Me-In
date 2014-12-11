/*
 * GeofenceUtils.java is part of Check Me In
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

package be.quentinbraet.checkmein.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.util.ArrayList;
import java.util.List;

import be.quentinbraet.checkmein.models.FenceInfo;
import be.quentinbraet.checkmein.services.RefreshGeofencesService_;
import be.quentinbraet.checkmein.services.TransitionService_;
import fi.foyt.foursquare.api.entities.CompactVenue;

/**
 * Created by quentin on 27/10/13.
 */
@EBean(scope = EBean.Scope.Singleton)
public class GeofenceUtils {

    @RootContext
    Context context;

    public List<CompactVenue> getConflictingFences(FenceInfo info, CompactVenue venue){
        List<CompactVenue> conflicts = new ArrayList<CompactVenue>();

        Location current = toLocation(venue);

        List<FenceInfo> fences = StorageUtils.getInstance().loadAllFenceInfo(context);
        List<CompactVenue> venues = StorageUtils.getInstance().loadCompactVenues(context);

        for(int i = 0; i < fences.size(); i++){
            CompactVenue otherVenue = venues.get(i);

            // no conflict with current ;)
            if(otherVenue.getId().equals(venue.getId())) continue;

            Location other = toLocation(otherVenue);

            float distance = current.distanceTo(other); // meters

            if(distance <= (fences.get(i).getRadius() + info.getRadius())){
                conflicts.add(otherVenue);
            }
        }

        return conflicts;
    }

    public Location toLocation(CompactVenue venue){
        Location location = new Location("app");
        location.setLongitude(venue.getLocation().getLng());
        location.setLatitude(venue.getLocation().getLat());
        return location;
    }

    public PendingIntent getTransitionPendingIntent(Context context) {
        // Create an explicit Intent
        Intent intent = new Intent(context,
                TransitionService_.class);
        /*
         * Return the PendingIntent
         */
        return PendingIntent.getService(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void updateGeofences(Context context){
        Intent i = new Intent(context, RefreshGeofencesService_.class);
        context.startService(i);
    }
}
