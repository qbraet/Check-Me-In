/*
 * AddConfigActivity.java is part of Check Me In
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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingApi;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SeekBarProgressChange;
import org.androidannotations.annotations.ViewById;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import be.quentinbraet.checkmein.Config;
import be.quentinbraet.checkmein.R;
import be.quentinbraet.checkmein.models.FenceInfo;
import be.quentinbraet.checkmein.models.FoursquareAccount;
import be.quentinbraet.checkmein.ui.views.ConnectNetworksDialog;
import be.quentinbraet.checkmein.utils.GeofenceUtils;
import be.quentinbraet.checkmein.utils.StorageUtils;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.CompactVenue;
import fi.foyt.foursquare.api.entities.CompleteUser;
import fi.foyt.foursquare.api.entities.Location;

import static be.quentinbraet.checkmein.utils.LogUtils.LOGD;
import static be.quentinbraet.checkmein.utils.LogUtils.LOGE;
import static be.quentinbraet.checkmein.utils.LogUtils.makeLogTag;

/**
 * Created by quentin on 19/10/13.
 */
@EActivity(R.layout.activity_add_config)
@OptionsMenu(R.menu.venue_config)
public class AddConfigActivity extends BaseActivity implements OnMarkerDragListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SlidingUpPanelLayout.PanelSlideListener{

    public static final String TAG = makeLogTag(AddConfigActivity.class);

    private GoogleMap map;

    MapFragment mapFragment;

    private StorageUtils store = StorageUtils.getInstance();

    protected FoursquareApi api = new FoursquareApi(Config.CLIENT_ID, Config.CLIENT_SECRET, Config.CLIENT_CALLBACK);

    @ViewById
    LinearLayout noPlayServices;

    @ViewById
    Button updatePlay;

    @ViewById
    Button updateMaps;

    @ViewById
    SeekBar rangeSeek;

    @ViewById
    TextView range;

    @ViewById
    SeekBar timeSeek;

    @ViewById
    TextView time;

    @ViewById
    RelativeLayout overlay;

    @ViewById
    ImageView facebookIcon;

    @ViewById
    ImageView twitterIcon;

    @Extra
    CompactVenue venue;

    @Bean
    GeofenceUtils geofenceUtils;

    @ViewById
    ImageView more;

    @ViewById
    SlidingUpPanelLayout slidingLayout;

    private Circle rangeCircle;
    private Marker marker;
    private GoogleApiClient locationClient;

    private boolean facebook;
    private boolean twitter;

    private boolean hasFacebook;
    private boolean hasTwitter;
    private GeofencingApi geofencingapi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api.setoAuthToken(FoursquareAccount.getInstance().getToken(this));
    }

    @AfterViews
    protected void afterInject(){
        // try to load data from memory (otherwise location of private venues will be the fuzzed location)
        CompactVenue temp = StorageUtils.getInstance().loadCompactVenue(this, venue.getId());
        if(temp != null){
            venue = temp;
        }


        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();

        getActionBar().setTitle(venue.getName());

        if(map == null){
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.hide(mapFragment);
            ft.commit();

            overlay.setVisibility(View.GONE);

            noPlayServices.setVisibility(View.VISIBLE);
            updatePlay.setOnClickListener(new OnClickListener(){
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

            updateMaps.setOnClickListener(new OnClickListener(){
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

        rangeSeek.setMax(300 - 10);
        timeSeek.setMax(30 - 2);

        FenceInfo info = StorageUtils.getInstance().loadFenceInfo(this, venue.getId());
        if(info != null){
            rangeSeek.setProgress((info.getRadius() - 10));
            timeSeek.setProgress((info.getDelay() - 2));

            facebook = info.isFacebook();
            if(facebook){
                facebookIcon.setImageResource(R.drawable.facebook_enabled);
            }else{
                facebookIcon.setImageResource(R.drawable.facebook_disabled);
            }

            twitter = info.isTwitter();
            if(twitter){
                twitterIcon.setImageResource(R.drawable.twitter_enabled);
            }else{
                twitterIcon.setImageResource(R.drawable.twitter_disabled);
            }

        }else{
            rangeSeek.setProgress(50 - 10);
            timeSeek.setProgress(5 - 2);
        }

        range.setText((rangeSeek.getProgress() + 10) + " m");
        time.setText((timeSeek.getProgress() + 2) + " min");

        slidingLayout.setPanelSlideListener(this);

        if(venue.getLocation().isFuzzed()) Crouton.makeText(this, getResources().getString(R.string.fuzzed_location), Style.INFO).show();
    }

    public void onStart(){
        super.onStart();

        if(servicesConnected()){
            locationClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            locationClient.connect();
        }
    }

    public void onResume(){
        super.onResume();

        updateNetworks();
    }

    public void onStop(){
        if(servicesConnected()){
            locationClient.disconnect();
        }
        super.onStop();
    }

    public void setupMap(){
        marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(venue.getLocation().getLat(), venue.getLocation().getLng()))
                .draggable(true));
        map.setOnMarkerDragListener(this);
        map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(marker.getPosition().latitude + 0.0005, marker.getPosition().longitude), (float) 16, (float) 30, (float) 0.0)));
        //map.getUiSettings().setAllGesturesEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        List<FenceInfo> fenceinfo = store.loadAllFenceInfo(this);
        List<CompactVenue> venues = store.loadCompactVenues(this);

        for(int i = 0 ; i < venues.size() ; i++){

            FenceInfo fence = fenceinfo.get(i);
            CompactVenue venueItem = venues.get(i);

            if(fence.getId().equals(venue.getId())) continue;

            CircleOptions circleOptions = new CircleOptions()
                    .center(new LatLng(venueItem.getLocation().getLat(), venueItem.getLocation().getLng()))
                    .strokeColor(getResources().getColor(R.color.blue))
                    .fillColor(getResources().getColor(R.color.blue_transparent_3))
                    .strokeWidth(3)
                    .radius(fence.getRadius()); // In meters

            map.addCircle(circleOptions);
        }
    }

    @SeekBarProgressChange({R.id.rangeSeek, R.id.timeSeek})
    void onProgressChangeOnSeekBar(SeekBar seekBar, int progress, boolean fromUser) {
        if(seekBar == rangeSeek){
            range.setText((progress + 10) + " m");
            drawCircle();
        }else if(seekBar == timeSeek){
            time.setText((timeSeek.getProgress() + 2) + " min");
        }
    }

    /*
     * Map interactions
     */

    public void drawCircle(){
        if(map == null) return;

        // Instantiates a new CircleOptions object and defines the center and radius
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(venue.getLocation().getLat(), venue.getLocation().getLng()))
                .strokeColor(getResources().getColor(R.color.blue))
                .fillColor(getResources().getColor(R.color.blue_transparent_9))
                .strokeWidth(3)
                .radius((rangeSeek.getProgress() + 10)); // In meters

        if(rangeCircle != null) rangeCircle.remove();
        rangeCircle = map.addCircle(circleOptions);
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {
        try{
            Location location = venue.getLocation();


            Field lat = ((Object) location).getClass().getDeclaredField("lat");
            Field lng = ((Object) location).getClass().getDeclaredField("lng");

            lat.setAccessible(true);
            lng.setAccessible(true);

            lat.set(location, marker.getPosition().latitude);
            lng.set(location, marker.getPosition().longitude);

        }catch(NoSuchFieldException ex){
            LOGE(TAG, ex.getMessage(), ex);
        }catch(IllegalAccessException ex){
            LOGE(TAG, ex.getMessage(), ex);
        }

        drawCircle();
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }

    /*
     * Actionbar actions
     */

    @OptionsItem
    @Background
    public void save(){
        final FenceInfo info = new FenceInfo(venue.getId());
        info.setRadius((rangeSeek.getProgress() + 10));
        info.setTransition(Geofence.GEOFENCE_TRANSITION_DWELL);
        info.setExpiration(Geofence.NEVER_EXPIRE);
        info.setDelay((timeSeek.getProgress() + 2));
        info.setFacebook(facebook);
        info.setTwitter(twitter);

        List<CompactVenue> conflicts = geofenceUtils.getConflictingFences(info, venue);

        if(!conflicts.isEmpty()){
            String error = getResources().getString(R.string.conflict_with) + " ";
            for(CompactVenue other : conflicts){
                error += other.getName() + " ";
            }
            error(error);
            return;
        }

        List<Geofence> fences = new ArrayList<Geofence>();
        fences.add(info.toGeofence(venue));

        if(locationClient != null && locationClient.isConnected()){
            store.saveCompactVenue(this, venue);
            store.saveFenceInfo(this, info);
            GeofencingRequest request = new GeofencingRequest.Builder()
                    .addGeofences(fences)
                    .build();
            PendingResult<Status> status = geofencingapi.addGeofences(locationClient, request, geofenceUtils.getTransitionPendingIntent(this));
            status.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if(LocationStatusCodes.SUCCESS == status.getStatusCode()){
                        success(getResources().getString(R.string.info_saved_venue));
                    }else{
                        // Fence not added, correct app
                        store.removeCompactVenue(AddConfigActivity.this, venue);
                        store.removeFenceInfo(AddConfigActivity.this, info.getId());
                        if(status.getStatusCode() == LocationStatusCodes.GEOFENCE_NOT_AVAILABLE){
                            error(getResources().getString(R.string.location_services_not_available));
                        }else if(status.getStatusCode() == LocationStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES){
                            error(getResources().getString(R.string.too_many_geofences));
                        }else{
                            error(getResources().getString(R.string.could_not_add_geofences));
                        }
                    }
                }
            }, 10, TimeUnit.SECONDS);
        }


        EasyTracker.getInstance(this).send(MapBuilder
                .createEvent("Save fence info", venue.getId(), (timeSeek.getProgress() + 2) + " min" + (rangeSeek.getProgress() + 10) + " m", null)
                .build()
        );
    }

    @OptionsItem
    @Background
    public void remove(){

        List<String> fences = new ArrayList<String>();
        fences.add(venue.getId());

        if(locationClient != null && locationClient.isConnected()){
            PendingResult<Status> status = geofencingapi.removeGeofences(locationClient, fences);
            status.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if(LocationStatusCodes.SUCCESS == status.getStatusCode()){
                        store.removeCompactVenue(AddConfigActivity.this, venue);
                        store.removeFenceInfo(AddConfigActivity.this, venue.getId());
                        success(getResources().getString(R.string.info_removed_venue));
                    }else{
                        error(getResources().getString(R.string.could_not_remove_geofences));
                    }
                }
            }, 10, TimeUnit.SECONDS);
        }
    }

    /*
     * Geofences and google play services
     */

    @Override
    public void onConnected(Bundle bundle) {
        LOGD(TAG, "connected");
        geofencingapi = LocationServices.GeofencingApi;
    }

    @Override
    public void onConnectionSuspended(int i) {
        locationClient = null;
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

    /*
     * Social network button interactions
     */

    @Click
    public void facebookIcon(){

        if(!hasFacebook && facebook){
            // No need to show connect dialog
        }else if(!hasFacebook){
            facebook = false;
            connectNetwork("Facebook");
            return;
        }

        if(facebook){
            facebookIcon.setImageResource(R.drawable.facebook_disabled);
        }else{
            facebookIcon.setImageResource(R.drawable.facebook_enabled);
        }

        facebook = !facebook;
    }

    @Click
    public void twitterIcon(){

        if(!hasTwitter && twitter){
            // No need to show connect dialog
        }else if(!hasTwitter){
            twitter = false;
            connectNetwork("Twitter");
            return;
        }

        if(twitter){
            twitterIcon.setImageResource(R.drawable.twitter_disabled);
        }else{
            twitterIcon.setImageResource(R.drawable.twitter_enabled);
        }

        twitter = !twitter;
    }

    public void connectNetwork(String network){

        ConnectNetworksDialog dialog = new ConnectNetworksDialog();
        dialog.show(getFragmentManager(), "dialog");

    }

    @Background
    public void updateNetworks(){
        try {
            Result<CompleteUser> result = api.user("self");

            String error = result.getMeta().getErrorDetail();
            if(error != null && !error.equals("")){
                EasyTracker.getInstance(this).send(MapBuilder
                                .createEvent("Exception", "check twitter/fb", error, null)
                                .build()
                );
                return;
            }
            CompleteUser user = result.getResult();

            String fb = user.getContact().getFacebook();
            String twit = user.getContact().getTwitter();

            if(fb != null && !fb.equals("")){
                hasFacebook = true;
            }

            if(twit != null && !twit.equals("")){
                hasTwitter = true;
            }
        } catch (FoursquareApiException e) {
            e.printStackTrace();
        }
    }

    /*
    * Panel listeners
    * */

    @Override
    public void onPanelSlide(View panel, float slideOffset) {

    }

    @Override
    public void onPanelCollapsed(View panel) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                more.setImageResource(R.drawable.ic_action_collapse);
            }
        });
    }

    @Override
    public void onPanelExpanded(View panel) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                more.setImageResource(R.drawable.ic_action_expand);
            }
        });
    }

    @Override
    public void onPanelAnchored(View panel) {

    }

}
