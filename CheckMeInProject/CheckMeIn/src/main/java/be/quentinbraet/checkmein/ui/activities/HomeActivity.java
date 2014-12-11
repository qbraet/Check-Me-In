/*
 * HomeActivity.java is part of Check Me In
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

import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.List;

import be.quentinbraet.checkmein.R;
import be.quentinbraet.checkmein.models.FenceInfo;
import be.quentinbraet.checkmein.ui.adapters.HomeVenueAdapter;
import be.quentinbraet.checkmein.utils.StorageUtils;
import fi.foyt.foursquare.api.entities.CompactVenue;

import static be.quentinbraet.checkmein.utils.LogUtils.LOGD;

/**
 * Created by quentin on 18/10/13.
 */
@EActivity(R.layout.activity_home)
@OptionsMenu(R.menu.home)
public class HomeActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap map;

    MapFragment mapFragment;

    @ViewById
    ListView list;

    @ViewById
    LinearLayout mapview;

    @ViewById
    LinearLayout noPlayServices;

    @ViewById
    Button updatePlay;

    @ViewById
    Button updateMaps;

    @ViewById
    LinearLayout placeholder;

    private HomeVenueAdapter adapter;

    private GoogleApiClient locationClient;

    private StorageUtils store = StorageUtils.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new HomeVenueAdapter(this);

        getActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void onResume(){
        super.onResume();
        List<CompactVenue> venues = StorageUtils.getInstance().loadCompactVenues(this);
        if(venues.size() == 0){
            placeholder.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
        }else{
            placeholder.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
        }
        adapter.setVenues(venues);

        setMarkers();

        if(servicesConnected()){
            locationClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            locationClient.connect();
        }

    }

    public void onPause(){
        if(servicesConnected()){
            locationClient.disconnect();
        }
        super.onPause();
    }

    @AfterViews
    protected void afterInject(){
        list.setAdapter(adapter);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();

        if(map == null){
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.hide(mapFragment);
            ft.commit();

            mapview.setVisibility(View.GONE);

            noPlayServices.setVisibility(View.VISIBLE);
            updatePlay.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    String appName = "com.google.android.gms";
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName)));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id="+appName)));
                    }
                }
            });

            updateMaps.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    String appName = "com.google.android.apps.maps";
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appName)));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id="+appName)));
                    }
                }
            });
        }else{
            noPlayServices.setVisibility(View.GONE);

            setupMap();
        }

    }

    @OptionsItem
    @Background
    public void add(){

        Intent i = new Intent(this, AddSearchActivity_.class);
        startActivity(i);

    }

    @OptionsItem
    @Background
    public void history(){

        Intent i = new Intent(this, HistoryActivity_.class);
        startActivity(i);

    }

    @UiThread
    public void setMarkers(){
        if(map == null) return;
        map.clear();

        List<FenceInfo> fenceinfo = store.loadAllFenceInfo(this);
        List<CompactVenue> venues = store.loadCompactVenues(this);

        for(int i = 0 ; i < venues.size() ; i++){
            FenceInfo fence = fenceinfo.get(i);
            CompactVenue venue = venues.get(i);

            CircleOptions circleOptions = new CircleOptions()
                    .center(new LatLng(venue.getLocation().getLat(), venue.getLocation().getLng()))
                    .strokeColor(getResources().getColor(R.color.blue))
                    .fillColor(getResources().getColor(R.color.blue_transparent_9))
                    .strokeWidth(3)
                    .radius(fence.getRadius()); // In meters

            map.addMarker(new MarkerOptions()
                    .position(new LatLng(venue.getLocation().getLat(), venue.getLocation().getLng()))
                    .draggable(false));

            map.addCircle(circleOptions);
        }
    }

    @UiThread
    public void setupMap(){
        map.setMyLocationEnabled(true);
        //map.getUiSettings().setAllGesturesEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * If the error has a resolution, start a Google Play services
         * activity to resolve it.
         */
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
            // If no resolution is available, display an error dialog
        } else {
            // Get the error code
            int errorCode = connectionResult.getErrorCode();
            // Get the error dialog from Google Play services
            showErrorDialog(errorCode);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        LOGD(TAG, "connected");
        Location location = LocationServices.FusedLocationApi.getLastLocation(locationClient);

        if(location != null){
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to Mountain View
                    .zoom(10)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        locationClient = null;
        LOGD(TAG, "disconnected");
    }
}
