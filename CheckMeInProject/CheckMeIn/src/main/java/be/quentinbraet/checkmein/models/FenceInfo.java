/*
 * FenceInfo.java is part of Check Me In
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

package be.quentinbraet.checkmein.models;

import com.google.android.gms.location.Geofence;

import java.io.Serializable;

import fi.foyt.foursquare.api.entities.CompactVenue;

/**
 * Created by quentin on 26/10/13.
 */
public class FenceInfo implements Serializable {

    private String id;
    private int radius;
    private long expiration;
    private int transition;
    private int delay = 5; // in minutes
    private boolean facebook;
    private boolean twitter;

    public FenceInfo(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getRadius() {
        if(radius < 10) radius = 10;
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public int getTransition() {
        return transition;
    }

    public void setTransition(int transition) {
        this.transition = transition;
    }

    public Geofence toGeofence(CompactVenue venue) {
        // Build a new Geofence object
        return new Geofence.Builder()
                .setRequestId(id)
                .setTransitionTypes(transition)
                .setCircularRegion(
                        venue.getLocation().getLat(),
                        venue.getLocation().getLng(),
                        radius)
                .setLoiteringDelay(delay*60*1000)
                .setExpirationDuration(expiration)
                .build();
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public boolean isFacebook() {
        return facebook;
    }

    public void setFacebook(boolean facebook) {
        this.facebook = facebook;
    }

    public boolean isTwitter() {
        return twitter;
    }

    public void setTwitter(boolean twitter) {
        this.twitter = twitter;
    }
}
