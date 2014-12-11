/*
 * AddSearchActivity.java is part of Check Me In
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

/**
 * Created by quentin on 19/10/13.
 */

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import be.quentinbraet.checkmein.R;
import be.quentinbraet.checkmein.ui.adapters.AddSearchAdapter;
import be.quentinbraet.checkmein.utils.StorageUtils;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.CompactVenue;
import fi.foyt.foursquare.api.entities.VenueHistory;
import fi.foyt.foursquare.api.entities.VenueHistoryGroup;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

@EActivity(R.layout.activity_list)
public class AddSearchActivity extends BaseActivity {

    @ViewById
    StickyListHeadersListView list;

    private AddSearchAdapter adapter;

    @ViewById
    RelativeLayout loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new AddSearchAdapter(this);

        loadMayors();
        loadVenueHistory(60);
    }

    @AfterViews
    public void afterViews(){
        list.setAdapter(adapter);
    }

    public void onResume(){
        super.onResume();
        adapter.setTracked(StorageUtils.getInstance().loadCompactVenues(this));
    }

    @Background
    protected void loadMayors(){
        loading();
        try {
            Result<VenueHistoryGroup> userResult = api.mayorships("self");
            String error = userResult.getMeta().getErrorDetail();
            if(error != null && !error.equals("")){
                error(error);
                return;
            }
            setMayors(userResult.getResult().getItems());
        } catch (FoursquareApiException e) {
            Crouton.makeText(this, e.getMessage(), Style.ALERT);
        }
        loadingDone();
    }

    @UiThread
    protected void setMayors(VenueHistory[] mayors){
        CompactVenue[] venues = new CompactVenue[mayors.length];
        for(int i = 0; i < mayors.length; i++){
            venues[i] = mayors[i].getVenue();
        }
        adapter.setMayors(venues);
    }

    @Background
    protected void loadVenueHistory(int days){
        loading();
        long now = System.currentTimeMillis();
        long before = now / 1000;
        long after = (now / 1000) - (60*60*24*days);
        try {
            Result<VenueHistoryGroup> result = api.usersVenueHistory("self", before, after);
            String error = result.getMeta().getErrorDetail();
            if(error != null && !error.equals("")){
                error(error);
                return;
            }
            setVenueHistory(result.getResult().getItems());
        } catch (FoursquareApiException e) {
            Crouton.makeText(this, e.getMessage(), Style.ALERT);
        }
        loadingDone();
    }

    @UiThread
    protected void setVenueHistory(VenueHistory[] history){
        CompactVenue[] venues = new CompactVenue[history.length];
        for(int i = 0; i < history.length; i++){
            venues[i] = history[i].getVenue();
        }
        adapter.setHistory(venues);
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
