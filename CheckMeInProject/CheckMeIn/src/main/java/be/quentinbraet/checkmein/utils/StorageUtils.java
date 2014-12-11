/*
 * StorageUtils.java is part of Check Me In
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import be.quentinbraet.checkmein.models.FenceInfo;
import fi.foyt.foursquare.api.entities.CompactVenue;

/**
 * Created by quentin on 20/10/13.
 */
public class StorageUtils {

    private static StorageUtils instance;

    public static final String COMPACT_VENUE = "COMPACT_VENUE";
    public static final String COMPACT_VENUE_IDS = "COMPACT_VENUE_IDS";
    public static final String CHECKIN_RANGE = "CHECKIN_RANGE";
    public static final String FENCE_INFO = "FENCE_INFO";
    private final Gson gson;

    public static StorageUtils getInstance(){

        if(instance == null){
            instance = new StorageUtils();
        }

        return instance;
    }

    public StorageUtils(){
        gson = new Gson();
    }

    public CompactVenue loadCompactVenue(Context context, String id){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String venueString =  sp.getString(COMPACT_VENUE + "." + id, "");

        if(venueString.equals("")) return null;

        return gson.fromJson(venueString, CompactVenue.class);
    }

    public void saveCompactVenue(Context context, CompactVenue venue){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String json = gson.toJson(venue);
        sp.edit().putString(COMPACT_VENUE + "." + venue.getId(), json).commit();
        addCompactVenueId(context, venue.getId());
    }

    public String[] loadCompactVenueIds(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String idList = sp.getString(COMPACT_VENUE_IDS, "");

        if(idList.equals("")) return new String[0];

        return idList.split(",");
    }

    public void addCompactVenueId(Context context, String id){
        List<String> list = new LinkedList<String>(Arrays.asList(loadCompactVenueIds(context)));
        if(!list.contains(id)){
            list.add(id);
        }
        String[] ids = (String[]) list.toArray(new String[list.size()]);
        saveCompactVenueIds(context, ids);
    }

    public void removeCompactVenueId(Context context, String id){
        List<String> list = new LinkedList<String>(Arrays.asList(loadCompactVenueIds(context)));
        if(list.contains(id)){
            list.remove(id);
        }
        String[] ids = list.toArray(new String[list.size()]);
        saveCompactVenueIds(context, ids);
    }

    private void saveCompactVenueIds(Context context, String[] ids){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        String idString;

        if(ids == null || ids.length == 0){
            idString = "";
        }else{

            StringBuilder nameBuilder = new StringBuilder();

            for (String id : ids) {
                nameBuilder.append(id);
                nameBuilder.append(",");
            }

            nameBuilder.deleteCharAt(nameBuilder.length() - 1);

            idString = nameBuilder.toString();
        }

        sp.edit().putString(COMPACT_VENUE_IDS, idString).commit();
    }

    public List<CompactVenue> loadCompactVenues(Context context){
        ArrayList<CompactVenue> result = new ArrayList<CompactVenue>();

        String[]ids = loadCompactVenueIds(context);

        for(String id : ids){
            CompactVenue venue = loadCompactVenue(context, id);
            if(venue != null) result.add(venue);
        }

        return result;
    }

    public void removeCompactVenue(Context context, CompactVenue venue){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().remove(COMPACT_VENUE + "." + venue.getId()).commit();
        removeCompactVenueId(context, venue.getId());
        removeRange(context, venue.getId());
    }

    public void removeCompactVenue(Context context,  String id){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().remove(COMPACT_VENUE + "." + id).commit();
        removeCompactVenueId(context, id);
        removeRange(context, id);
    }

    public void saveRange(Context context, String id, int range){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(CHECKIN_RANGE + "." + id, range).commit();
    }

    public void removeRange(Context context, String id){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().remove(CHECKIN_RANGE + "." + id).commit();
    }

    public FenceInfo loadFenceInfo(Context context, String id){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String fenceInfoString =  sp.getString(FENCE_INFO + "." + id, "");

        if(fenceInfoString.equals("")) return null;

        return gson.fromJson(fenceInfoString, FenceInfo.class);
    }

    public void saveFenceInfo(Context context, FenceInfo info){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String json = gson.toJson(info);
        sp.edit().putString(FENCE_INFO + "." + info.getId(), json).commit();
    }

    public void removeFenceInfo(Context context, String id){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().remove(FENCE_INFO + "." + id).commit();
    }

    public List<FenceInfo> loadAllFenceInfo(Context context){
        ArrayList<FenceInfo> result = new ArrayList<FenceInfo>();

        String[]ids = loadCompactVenueIds(context);

        for(String id : ids){
            FenceInfo info = loadFenceInfo(context, id);
            if(info != null) result.add(info);
        }

        return result;
    }

}
