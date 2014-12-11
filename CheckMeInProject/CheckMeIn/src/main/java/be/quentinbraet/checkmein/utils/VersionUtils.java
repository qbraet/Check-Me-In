/*
 * VersionUtils.java is part of Check Me In
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import java.util.List;

import be.quentinbraet.checkmein.models.FenceInfo;
import be.quentinbraet.checkmein.services.RefreshGeofencesService_;

/**
 * Created by quentin on 1/12/13.
 */
public class VersionUtils {

    public static final String VERSION = "VERSION";

    public static void checkForUpdate(Context context){

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        int oldVersion = sp.getInt(VERSION, Integer.MAX_VALUE);

        try {

            int newVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            sp.edit().putInt(VERSION, newVersion).commit();

            if(oldVersion == Integer.MAX_VALUE) return;

            Log.d("CMI_VERSIONING", "update to version " + newVersion + " from " + oldVersion);

            if(oldVersion < 5){

                StorageUtils store = StorageUtils.getInstance();
                List<FenceInfo> fences = store.loadAllFenceInfo(context);
                for(FenceInfo fence : fences){
                    fence.setTransition(Geofence.GEOFENCE_TRANSITION_DWELL);
                    store.saveFenceInfo(context, fence);
                }
            }

            if(oldVersion < 10){
                StorageUtils store = StorageUtils.getInstance();
                List<FenceInfo> fences = store.loadAllFenceInfo(context);
                for(FenceInfo fence : fences){
                    fence.setTransition(Geofence.GEOFENCE_TRANSITION_DWELL);
                    fence.setDelay(5);
                    store.saveFenceInfo(context, fence);
                }
            }

            // update geofences
            Intent i = new Intent(context, RefreshGeofencesService_.class);
            context.startService(i);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }
}
