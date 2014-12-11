/*
 * SplashActivity.java is part of Check Me In
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

import com.crashlytics.android.Crashlytics;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;

import be.quentinbraet.checkmein.R;
import be.quentinbraet.checkmein.models.FoursquareAccount;
import be.quentinbraet.checkmein.utils.VersionUtils;

/**
 * Created by quentin on 31/12/13.
 */
@EActivity(R.layout.activity_splash)
public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);
    }

    protected void onResume(){
        super.onResume();
        checkLoggedIn();

    }

    @Background
    protected void checkLoggedIn(){

        VersionUtils.checkForUpdate(this);

        boolean isAuthorized = !TextUtils.isEmpty(FoursquareAccount.getInstance().getToken(this));

        if(isAuthorized){
            Intent i = new Intent(this, HomeActivity_.class);
            startActivity(i);
            finish();
        }else{
            Intent i = new Intent(this, LoginActivity_.class);
            startActivity(i);
            finish();
        }
    }

}
