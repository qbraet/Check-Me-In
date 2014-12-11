/*
 * BaseActivity.java is part of Check Me In
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

package be.quentinbraet.checkmein.ui.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;

import be.quentinbraet.checkmein.Config;
import be.quentinbraet.checkmein.R;
import be.quentinbraet.checkmein.models.FoursquareAccount;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import fi.foyt.foursquare.api.FoursquareApi;

import static be.quentinbraet.checkmein.utils.LogUtils.LOGD;
import static be.quentinbraet.checkmein.utils.LogUtils.makeLogTag;

/**
 * Created by quentin on 18/10/13.
 */
@EActivity
public class BaseActivity extends Activity {

    protected FoursquareApi api = new FoursquareApi(Config.CLIENT_ID, Config.CLIENT_SECRET, Config.CLIENT_CALLBACK);

    public static final String TAG = makeLogTag(BaseActivity.class);

    /**
     * Used to know wether the activity is still valid (started) or already stopped
     */
    private boolean valid;

    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    protected final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setIcon(R.drawable.logo_trans);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        String token = FoursquareAccount.getInstance().getToken(this);
        if(token == null || token.equals("")){
            startActivity(new Intent(this, LoginActivity_.class));
            finish();
        }else{
            api.setoAuthToken(token);
        }



        // create our manager instance after the content view is set
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);
        // set a custom tint color for all system bars
        tintManager.setTintColor(getResources().getColor(R.color.blue));
    }

    @Override
    protected void onDestroy(){
        Crouton.cancelAllCroutons();
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);  // Add this method.
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
    }

    @Override
    public void onResume() {
        super.onResume();
        valid = true;
    }

    @Override
    public void onPause(){
        valid = false;
        super.onPause();
    }

    @UiThread
    protected void error(String error){
        Crouton.makeText(this, error, Style.ALERT).show();
    }

    @UiThread
    protected void info(String info){
        Crouton.makeText(this, info, Style.INFO).show();
    }

    @UiThread
    protected void success(String success){
        Crouton.makeText(this, success, Style.CONFIRM).show();
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

            showErrorDialog(resultCode);
            return false;
        }
    }

    protected void showErrorDialog(int errorCode) {
        if(!valid) return;

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this,
                CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
            // Create a new DialogFragment for the error dialog
            ErrorDialogFragment errorFragment =
                    new ErrorDialogFragment();
            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);
            // Show the error dialog in the DialogFragment
            errorFragment.show(getFragmentManager(),
                    "Play Services");
        }
    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
}
