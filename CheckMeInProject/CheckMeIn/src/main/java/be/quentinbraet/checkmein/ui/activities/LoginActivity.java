/*
 * LoginActivity.java is part of Check Me In
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
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;

import com.foursquare.android.nativeoauth.FoursquareCancelException;
import com.foursquare.android.nativeoauth.FoursquareDenyException;
import com.foursquare.android.nativeoauth.FoursquareInvalidRequestException;
import com.foursquare.android.nativeoauth.FoursquareOAuth;
import com.foursquare.android.nativeoauth.FoursquareOAuthException;
import com.foursquare.android.nativeoauth.FoursquareUnsupportedVersionException;
import com.foursquare.android.nativeoauth.model.AccessTokenResponse;
import com.foursquare.android.nativeoauth.model.AuthCodeResponse;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import be.quentinbraet.checkmein.Config;
import be.quentinbraet.checkmein.R;
import be.quentinbraet.checkmein.models.FoursquareAccount;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static be.quentinbraet.checkmein.utils.LogUtils.makeLogTag;

@EActivity(R.layout.activity_login)
public class LoginActivity extends Activity {

    public static final String TAG = makeLogTag(LoginActivity.class);

    private static final int REQUEST_CODE_FSQ_CONNECT = 200;
    private static final int REQUEST_CODE_FSQ_TOKEN_EXCHANGE = 201;

    @ViewById
    Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //VersionUtils.checkForUpdate(this);
        //checkLoggedIn();
    }

    @Override
    protected void onDestroy(){
        Crouton.cancelAllCroutons();
        super.onDestroy();
    }

    @Click
    public void loginButton(){
        // Start the native auth flow.
        Intent intent = FoursquareOAuth.getConnectIntent(LoginActivity.this, Config.CLIENT_ID);

        // If the device does not have the Foursquare app installed, we'd
        // get an intent back that would open the Play Store for download.
        // Otherwise we start the auth flow.
        if (FoursquareOAuth.isPlayStoreIntent(intent)) {
            Crouton.makeText(this, R.string.app_not_installed_message, Style.ALERT).show();
            startActivity(intent);
        } else {
            startActivityForResult(intent, REQUEST_CODE_FSQ_CONNECT);
        }
    }

    private void checkLoggedIn(){

        boolean isAuthorized = !TextUtils.isEmpty(FoursquareAccount.getInstance().getToken(this));

        if(isAuthorized){
            Intent i = new Intent(this, HomeActivity_.class);
            startActivity(i);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_FSQ_CONNECT:
                onCompleteConnect(resultCode, data);
                break;

            case REQUEST_CODE_FSQ_TOKEN_EXCHANGE:
                onCompleteTokenExchange(resultCode, data);
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onCompleteConnect(int resultCode, Intent data) {
        AuthCodeResponse codeResponse = FoursquareOAuth.getAuthCodeFromResult(resultCode, data);
        Exception exception = codeResponse.getException();

        if (exception == null) {
            // Success.
            String code = codeResponse.getCode();
            performTokenExchange(code);

        } else {
            if (exception instanceof FoursquareCancelException) {
                // Cancel.
                Crouton.makeText(this, R.string.canceled, Style.ALERT).show();

            } else if (exception instanceof FoursquareDenyException) {
                // Deny.
                Crouton.makeText(this, R.string.denied, Style.ALERT).show();

            } else if (exception instanceof FoursquareOAuthException) {
                // OAuth error.
                String errorMessage = exception.getMessage();
                String errorCode = ((FoursquareOAuthException) exception).getErrorCode();
                Crouton.makeText(this, errorMessage + " [" + errorCode + "]", Style.ALERT).show();

            } else if (exception instanceof FoursquareUnsupportedVersionException) {
                // Unsupported Fourquare app version on the device.
                Crouton.makeText(this, exception.getMessage(), Style.ALERT).show();

            } else if (exception instanceof FoursquareInvalidRequestException) {
                // Invalid request.
                Crouton.makeText(this, exception.getMessage(), Style.ALERT).show();

            } else {
                // Error.
                Crouton.makeText(this, exception.getMessage(), Style.ALERT).show();
            }
        }
    }

    /**
     * Exchange a code for an OAuth Token. Note that we do not recommend you
     * do this in your app, rather do the exchange on your server. Added here
     * for demo purposes.
     *
     * @param code
     *          The auth code returned from the native auth flow.
     */
    private void performTokenExchange(String code) {
        Intent intent = FoursquareOAuth.getTokenExchangeIntent(this, Config.CLIENT_ID, Config.CLIENT_SECRET, code);
        startActivityForResult(intent, REQUEST_CODE_FSQ_TOKEN_EXCHANGE);
    }

    private void onCompleteTokenExchange(int resultCode, Intent data) {
        AccessTokenResponse tokenResponse = FoursquareOAuth.getTokenFromResult(resultCode, data);
        Exception exception = tokenResponse.getException();

        if (exception == null) {
            String accessToken = tokenResponse.getAccessToken();
            // Success.
            Crouton.makeText(this, R.string.connected_to_foursquare, Style.CONFIRM).show();

            // Persist the token for later use. In this example, we save
            // it to shared prefs.
            FoursquareAccount.getInstance().setToken(this, accessToken);

            // Refresh UI.
            checkLoggedIn();

        } else {
            if (exception instanceof FoursquareOAuthException) {
                // OAuth error.
                String errorMessage = ((FoursquareOAuthException) exception).getMessage();
                String errorCode = ((FoursquareOAuthException) exception).getErrorCode();
                Crouton.makeText(this, errorMessage + " [" + errorCode + "]", Style.ALERT).show();

            } else {
                // Other exception type.
                Crouton.makeText(this, exception.getMessage(), Style.ALERT).show();
            }
        }
    }
}
