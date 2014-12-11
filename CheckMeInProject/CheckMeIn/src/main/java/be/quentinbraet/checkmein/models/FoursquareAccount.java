/*
 * FoursquareAccount.java is part of Check Me In
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import fi.foyt.foursquare.api.Result;

import static be.quentinbraet.checkmein.utils.LogUtils.LOGD;
import static be.quentinbraet.checkmein.utils.LogUtils.makeLogTag;

/**
 * Created by quentin on 18/10/13.
 */
public class FoursquareAccount {

    public static final String TAG = makeLogTag(FoursquareAccount.class);

    public static final String FS_AUTH_TOKEN = "FS_AUTH_TOKEN";

    private static FoursquareAccount instance;

    public static FoursquareAccount getInstance(){

        if(instance == null){
            instance = new FoursquareAccount();
        }

        return instance;
    }

    public String getToken(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(FS_AUTH_TOKEN, null);
    }

    public void setToken(Context context, String token){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(FS_AUTH_TOKEN, token).commit();
    }

    public void checkAuth(Result<?> result, Context context){
        if(result.getMeta().getErrorType().equals("invalid_auth")){
            LOGD(TAG, "Invalid oauth, relogin");
            setToken(context, null);
        }
    }
}
