/*
 * BaseIntentService.java is part of Check Me In
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

import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.UiThread;

import be.quentinbraet.checkmein.Config;
import be.quentinbraet.checkmein.models.FoursquareAccount;
import fi.foyt.foursquare.api.FoursquareApi;

import static be.quentinbraet.checkmein.utils.LogUtils.LOGD;
import static be.quentinbraet.checkmein.utils.LogUtils.makeLogTag;

/**
 * Created by quentin on 27/10/13.
 */
@EService
public class BaseIntentService extends IntentService {

    protected FoursquareApi api = new FoursquareApi(Config.CLIENT_ID, Config.CLIENT_SECRET, Config.CLIENT_CALLBACK);

    public static final String TAG = makeLogTag(BaseIntentService.class);

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public BaseIntentService() {
        super("");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        api.setoAuthToken(FoursquareAccount.getInstance().getToken(this));
    }

    @UiThread
    public void toast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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


}
