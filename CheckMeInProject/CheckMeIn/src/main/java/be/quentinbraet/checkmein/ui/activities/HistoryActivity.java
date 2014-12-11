/*
 * HistoryActivity.java is part of Check Me In
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

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import be.quentinbraet.checkmein.R;
import be.quentinbraet.checkmein.ui.adapters.HistoryAdapter;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.Checkin;
import fi.foyt.foursquare.api.entities.CheckinGroup;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by quentin on 16/12/13.
 */
@EActivity(R.layout.activity_list)
public class HistoryActivity extends BaseActivity {

    @ViewById
    StickyListHeadersListView list;

    @ViewById
    RelativeLayout loading;

    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new HistoryAdapter(this);

        loadHistory();
    }

    @AfterViews
    public void afterViews(){
        list.setAdapter(adapter);
        getActionBar().setTitle(getResources().getString(R.string.checkin_history));
    }

    public void onResume(){
        super.onResume();
        adapter.updateTracked();
    }

    @Background
    protected void loadHistory() {
        loading();
        try {
            Result<CheckinGroup> userResult = api.usersCheckins("self", 20, 0, null, null);
            String error = userResult.getMeta().getErrorDetail();
            if(error != null && !error.equals("")){
                error(error);
                return;
            }
            setCheckins(userResult.getResult().getItems());
        } catch (FoursquareApiException e) {
            Crouton.makeText(this, e.getMessage(), Style.ALERT);
        }
        loadingDone();
    }

    @UiThread
    protected void setCheckins(Checkin[] checkins){
        adapter.setCheckins(checkins);
    }

    @UiThread
    protected void loading(){
        loading.setVisibility(View.VISIBLE);
    }

    @UiThread
    protected void loadingDone(){
        loading.setVisibility(View.GONE);
    }
}
