package com.palash.uberdriver.ui.home;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.palash.uberdriver.Common;
import com.palash.uberdriver.Model.EventBus.DriverRequestReceived;
import com.palash.uberdriver.Model.EventBus.NotifyToRiderEvent;
import com.palash.uberdriver.Model.RiderModel;
import com.palash.uberdriver.Model.TripPlanModel;
import com.palash.uberdriver.R;
import com.palash.uberdriver.Remote.IGoogleAPI;
import com.palash.uberdriver.Remote.RetrofitClient;
import com.palash.uberdriver.Utils.LocationUtils;
import com.palash.uberdriver.Utils.UserUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    @BindView(R.id.chip_decline)
    Chip chip_decline;
    @BindView(R.id.layout_accept)
    CardView layout_accept;
    @BindView(R.id.circularProgressBar)
    CircularProgressBar circularProgressBar;
    @BindView(R.id.txt_estimate_time)
    TextView txt_estimate_time;
    @BindView(R.id.txt_estimate_distance)
    TextView txt_estimate_distance;
    @BindView(R.id.root_layout)
    FrameLayout root_layout;

    @BindView(R.id.txt_rating)
    TextView txt_rating;

    @BindView(R.id.txt_type_uber)
    TextView txt_type_uber;

    @BindView(R.id.img_round)
    ImageView img_round;

    @BindView(R.id.layout_start_uber)
    CardView layout_start_uber;

    @BindView(R.id.txt_rider_name)
    TextView txt_rider_name;

    @BindView(R.id.txt_start_uber_estimate_distance)
    TextView txt_start_uber_estimate_distance;

    @BindView(R.id.txt_start_uber_estimate_time)
    TextView txt_start_uber_estimate_time;

    @BindView(R.id.img_phone_call)
    ImageView img_phone_call;

    @BindView(R.id.btn_start_uber)
    Button btn_start_uber;

    @BindView(R.id.btn_complete_trip)
    Button btn_complete_trip;

    @BindView(R.id.layout_notify_rider)
    LinearLayout layout_notify_rider;

    @BindView(R.id.txt_notify_rider)
    TextView txt_notify_rider;

    @BindView(R.id.progress_notify)
    ProgressBar progress_notify;

    private GeoFire pickupGeoFire, destinationGeoFire;
    private GeoQuery pickupGeoQuery, destinationGeoQuery;

    private boolean isTripStart = false, onlineSystemAlreadyRegister = false;

    private GeoQueryEventListener pickupGeoQueryListener = new GeoQueryEventListener() {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            btn_start_uber.setEnabled(true);
            UserUtils.sendNotifyToRider(getContext(), root_layout, key);
            if (pickupGeoQuery != null) {
                //Remove geoFire
                pickupGeoFire.removeLocation(key);
                pickupGeoFire = null;
                pickupGeoQuery.removeAllListeners();
            }
        }

        @Override
        public void onKeyExited(String key) {
            btn_start_uber.setEnabled(false);

        }

        @Override
        public void onKeyMoved(String key, GeoLocation location) {

        }

        @Override
        public void onGeoQueryReady() {

        }

        @Override
        public void onGeoQueryError(DatabaseError error) {

        }
    };

    private GeoQueryEventListener destinationGeoQueryListener = new GeoQueryEventListener() {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            Toast.makeText(getContext(), "Destination Entered", Toast.LENGTH_SHORT).show();
            btn_complete_trip.setEnabled(true);
            if (destinationGeoQuery != null) {
                destinationGeoFire.removeLocation(key);
                destinationGeoFire = null;
                destinationGeoQuery.removeAllListeners();
            }
        }

        @Override
        public void onKeyExited(String key) {

        }

        @Override
        public void onKeyMoved(String key, GeoLocation location) {

        }

        @Override
        public void onGeoQueryReady() {

        }

        @Override
        public void onGeoQueryError(DatabaseError error) {

        }
    };

    private CountDownTimer waiting_timer;
    private String cityName = "";


    @OnClick(R.id.chip_decline)
    void onDeclineClick() {
        if (driverRequestReceived != null) {
            if (TextUtils.isEmpty(tripNumberId)) {
                if (countDownEvent != null) {
                    countDownEvent.dispose();
                }
                chip_decline.setVisibility(View.GONE);
                layout_accept.setVisibility(View.GONE);
                mMap.clear();
                circularProgressBar.setProgress(0);
                UserUtils.sendDeclineRequest(root_layout, getContext(), driverRequestReceived.getKey());
                driverRequestReceived = null;

            } else {
                if (ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(mapFragment.getView(), getString(R.string.permision_requered), Snackbar.LENGTH_SHORT).show();
                    return;
                }
                fusedLocationProviderClient.getLastLocation()
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                                Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            }
                        }).addOnSuccessListener(location -> {
                    chip_decline.setVisibility(View.GONE);
                    layout_start_uber.setVisibility(View.GONE);
                    mMap.clear();
                    UserUtils.sendDeclineAndRemoveTripRequest(root_layout, getContext(),
                            driverRequestReceived.getKey(), tripNumberId);
                    tripNumberId = ""; //Set tripNumberId to empty
                    driverRequestReceived = null;
                    makeDriverOnline(location);
                });
            }
        }
    }

    @OnClick(R.id.btn_start_uber)
    void onStartUberClick() {
        //Clears route
        if (blackPolyline != null) blackPolyline.remove();
        if (greyPolyline != null) greyPolyline.remove();
        //Cancel waiting timer
        if (waiting_timer != null) waiting_timer.cancel();
        layout_notify_rider.setVisibility(View.GONE);
        if (driverRequestReceived != null) {
            LatLng destinationLatLng = new LatLng(
                    Double.parseDouble(driverRequestReceived.getDestinationLocation().split(",")[0]),
                    Double.parseDouble(driverRequestReceived.getDestinationLocation().split(",")[1]));

            mMap.addMarker(new MarkerOptions()
                    .position(destinationLatLng)
                    .title(driverRequestReceived.getDestinationLocationString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
            //draw path
            drawPathFromCurrentLocation(driverRequestReceived.getDestinationLocation());
        }
        btn_start_uber.setVisibility(View.GONE);
        chip_decline.setVisibility(View.GONE);
        btn_complete_trip.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.btn_complete_trip)
    void onCompleteTripClick() {
        //Toast.makeText(getContext(), "Complete Trip fake action", Toast.LENGTH_SHORT).show();
        Map<String, Object> update_trip = new HashMap<>();
        update_trip.put("done", true);
        FirebaseDatabase.getInstance().getReference(Common.Trip).child(tripNumberId).updateChildren(update_trip)
                .addOnFailureListener(e -> Snackbar.make(mapFragment.requireView(), e.getMessage(), Snackbar.LENGTH_LONG).show())
                .addOnSuccessListener(aVoid -> {
                    //Get location
                    if (ActivityCompat.checkSelfPermission(getContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(getContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Snackbar.make(mapFragment.requireView(), getContext().getString(R.string.permision_requered), Snackbar.LENGTH_LONG).show();

                        return;
                    }
                    fusedLocationProviderClient.getLastLocation().addOnFailureListener(e ->
                            Snackbar.make(mapFragment.requireView(), e.getMessage(), Snackbar.LENGTH_LONG).show())
                            .addOnSuccessListener(location -> {
                                UserUtils.sendCompleteTripToRider(mapFragment.requireView(), getContext(), driverRequestReceived.getKey(), tripNumberId);
                                //Clear map
                                mMap.clear();
                                tripNumberId = ""; //set tripNumberId to empty
                                isTripStart = false;
                                chip_decline.setVisibility(View.GONE);

                                layout_accept.setVisibility(View.GONE);
                                circularProgressBar.setProgress(0);

                                layout_start_uber.setVisibility(View.GONE);

                                layout_notify_rider.setVisibility(View.GONE);
                                progress_notify.setProgress(0);

                                btn_complete_trip.setEnabled(false);
                                btn_complete_trip.setVisibility(View.GONE);

                                btn_start_uber.setEnabled(false);
                                btn_start_uber.setVisibility(View.VISIBLE);

                                destinationGeoFire = null;
                                pickupGeoFire = null;

                                makeDriverOnline(location);


                            });

                });
    }

    private void drawPathFromCurrentLocation(String destinationLocation) {

        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(requireView(), getString(R.string.permision_requered), Snackbar.LENGTH_LONG).show();

            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> {
                    Snackbar.make(requireView(), e.getMessage(), Snackbar.LENGTH_LONG).show();

                }).addOnSuccessListener(location ->
        {
            compositeDisposable.add(iGoogleAPI.getDirections("driving",
                    "less driving",
                    new StringBuilder()
                            .append(location.getLatitude())
                            .append(",")
                            .append(location.getLongitude())
                            .toString(),
                    destinationLocation,
                    getString(R.string.google_api_keys))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(returnResult -> {

                        try {
                            //Parse json
                            JSONObject jsonObject = new JSONObject(returnResult);
                            JSONArray jsonArray = jsonObject.getJSONArray("routes");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject route = jsonArray.getJSONObject(i);
                                JSONObject poly = route.getJSONObject("overview_polyline");
                                String polyline = poly.getString("points");
                                polylineList = Common.decodePoly(polyline);
                            }

                            polylineOptions = new PolylineOptions();
                            polylineOptions.color(Color.GRAY);
                            polylineOptions.width(12);
                            polylineOptions.startCap(new SquareCap());
                            polylineOptions.jointType(JointType.ROUND);
                            polylineOptions.addAll(polylineList);
                            greyPolyline = mMap.addPolyline(polylineOptions);

                            blackPolylineOptions = new PolylineOptions();
                            blackPolylineOptions.color(Color.BLACK);
                            blackPolylineOptions.width(5);
                            blackPolylineOptions.startCap(new SquareCap());
                            blackPolylineOptions.jointType(JointType.ROUND);
                            blackPolylineOptions.addAll(polylineList);
                            blackPolyline = mMap.addPolyline(blackPolylineOptions);


                            LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                            LatLng destination = new LatLng(Double.parseDouble(destinationLocation.split(",")[0]),
                                    Double.parseDouble(destinationLocation.split(",")[1]));

                            LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                    .include(origin)
                                    .include(destination)
                                    .build();

                            createGeoFireDestinationLocation(driverRequestReceived.getKey(), destination);

                            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 160));
                            mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom - 1));


                        } catch (Exception e) {
                            //Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }));

        });

    }

    private void createGeoFireDestinationLocation(String key, LatLng destination) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Common.TRIP_DESTINATION_LOCATION_REF);
        destinationGeoFire = new GeoFire(ref);
        destinationGeoFire.setLocation(key, new GeoLocation(destination.latitude, destination.longitude),
                (key1, error) -> {

                });
    }

    //Routes...........
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IGoogleAPI iGoogleAPI;
    private Polyline blackPolyline, greyPolyline;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private List<LatLng> polylineList;

    private DriverRequestReceived driverRequestReceived;
    private Disposable countDownEvent;
    private String tripNumberId = "";

    private GoogleMap mMap;
    HomeViewModel homeViewModel;
    SupportMapFragment mapFragment;
    private boolean isFirstTime = true;

    //Location............
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    //online system.................
    DatabaseReference onlineRef, currentUserRef, driversLocationRef;
    GeoFire geoFire;
    ValueEventListener onlineValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (snapshot.exists() && currentUserRef != null) {
                currentUserRef.onDisconnect().removeValue();
                //isFirstTime=true;
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Snackbar.make(mapFragment.getView(), error.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    };

    @Override
    public void onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        geoFire.removeLocation(FirebaseAuth.getInstance().getCurrentUser().getUid());
        onlineRef.removeEventListener(onlineValueEventListener);

        if (EventBus.getDefault().hasSubscriberForEvent(DriverRequestReceived.class))
            EventBus.getDefault().removeStickyEvent(DriverRequestReceived.class);
        if (EventBus.getDefault().hasSubscriberForEvent(NotifyToRiderEvent.class))
            EventBus.getDefault().removeStickyEvent(NotifyToRiderEvent.class);
        EventBus.getDefault().unregister(this);
        compositeDisposable.clear();
        onlineSystemAlreadyRegister = false;
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            if (!EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().register(this);
        } catch (EventBusException e) {
            Log.d("OnStartMethod", e.getMessage());
            //Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        /*if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);*/

    }

    @Override
    public void onResume() {
        super.onResume();
        registerOnlineSystem();
    }

    private void registerOnlineSystem() {
        if (!onlineSystemAlreadyRegister) {
            onlineRef.addValueEventListener(onlineValueEventListener);
            onlineSystemAlreadyRegister = true;
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        initViews(root);
        init();
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        return root;
    }

    private void initViews(View root) {
        ButterKnife.bind(this, root);
    }

    private void init() {

        iGoogleAPI = RetrofitClient.getInstance().create(IGoogleAPI.class);

        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected");

        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(root_layout, getString(R.string.permision_requered), Snackbar.LENGTH_SHORT).show();
            return;
        }

        buildLocationRequest();
        buildLocationCallback();
        updateLocation();
    }

    private void updateLocation() {
        if (fusedLocationProviderClient == null) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());

            if (ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                return;
            }
        }
    }

    private void buildLocationCallback() {
        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude());

                    if (pickupGeoFire != null) {
                        pickupGeoQuery = pickupGeoFire.queryAtLocation(new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                locationResult.getLastLocation().getLongitude()), Common.MIN_RANGE_PICKUP_IN_KM);

                        pickupGeoQuery.addGeoQueryEventListener(pickupGeoQueryListener);
                    }

                    //Destination
                    if (destinationGeoFire != null) {
                        destinationGeoQuery = destinationGeoFire.queryAtLocation(new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                locationResult.getLastLocation().getLongitude()), Common.MIN_RANGE_PICKUP_IN_KM);

                        destinationGeoQuery.addGeoQueryEventListener(destinationGeoQueryListener);
                    }

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));

                    if (!isTripStart) {
                        //Here , after get location , we will get address name
                        makeDriverOnline(locationResult.getLastLocation());
                    } else {
                        //Update location of Driver
                        Map<String, Object> update_data = new HashMap<>();
                        update_data.put("currentLat", locationResult.getLastLocation().getLatitude());
                        update_data.put("currentLng", locationResult.getLastLocation().getLongitude());

                        FirebaseDatabase.getInstance()
                                .getReference(Common.Trip)
                                .child(tripNumberId)
                                .updateChildren(update_data)
                                .addOnFailureListener(e -> Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_LONG).show())
                                .addOnSuccessListener(aVoid -> {


                                });
                    }
                }
            };
        }
    }

    private void makeDriverOnline(Location location) {
        String saveCityName = cityName;
        cityName = LocationUtils.getAddressFromLocation(getContext(), location); //Next get new city name when location change
        if (!cityName.equals(saveCityName)) //If old and new city name aren't equals
        {
            if (currentUserRef != null) //check currentUserRef
            {
                currentUserRef.removeValue() //Delete currentUserRef (Driver location old position)
                        .addOnFailureListener(e -> {

                            Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_LONG).show();

                        }).addOnSuccessListener(unused -> {

                    updateDriverLocation(location);

                });
            }
        }else //If city name old and new are equals, we just update with delete
        {
            updateDriverLocation(location);
        }
    }

    private void updateDriverLocation(Location location) {

        if (!TextUtils.isEmpty(cityName)){
            driversLocationRef = FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCES)
                    .child(cityName);
            currentUserRef = driversLocationRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            geoFire = new GeoFire(driversLocationRef);

            //update location......................
            geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(location.getLatitude(),
                    location.getLongitude()), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                    if (error != null)
                        Snackbar.make(mapFragment.getView(), error.getMessage(), Snackbar.LENGTH_LONG).show();

                }
            });
            registerOnlineSystem();//only register when we done setup
        }else {
            Snackbar.make(mapFragment.getView(), getString(R.string.service_unavailable_here), Snackbar.LENGTH_LONG).show();
        }

    }

    private void buildLocationRequest() {
        if (locationRequest == null) {
            locationRequest = new LocationRequest();
            locationRequest.setSmallestDisplacement(50f); //50 m
            locationRequest.setInterval(15000); //15 sec
            locationRequest.setFastestInterval(10000); //10 sec
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        //Check permission..................
        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            Snackbar.make(getView(), getString(R.string.permision_requered), Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        mMap.setOnMyLocationButtonClickListener(() -> {
                            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                Snackbar.make(getView(), getString(R.string.permision_requered), Snackbar.LENGTH_SHORT).show();
                                return false;
                            }


                            fusedLocationProviderClient.getLastLocation()
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show()).addOnSuccessListener(location -> {
                                try {
                                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f));
                                } catch (NullPointerException e) {
                                    e.printStackTrace();
                                }
                            });
                            return true;

                        });

                        //set layout button
                        View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1"))
                                .getParent()).findViewById(Integer.parseInt("2"));

                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

                        //Right button.............
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        params.setMargins(0, 0, 0, 50);

                        //Move location
                        buildLocationRequest();
                        buildLocationCallback();
                        updateLocation();

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(getContext(), "Permission " + permissionDeniedResponse.getPermissionName() + "" + "was denied!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();
        //mMap.getUiSettings().setZoomControlsEnabled(true);

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.uber_maps_style));
            if (!success) {
                Log.e("HomeFragment", "Style parsing error");
            }

        } catch (Resources.NotFoundException e) {
            Log.e("HomeFragment", "" + e.getMessage());
        }


        Snackbar.make(mapFragment.getView(), "You're online", Snackbar.LENGTH_LONG).show();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    private void onDriverRequestReceive(DriverRequestReceived event) {
        driverRequestReceived = event;

        //Get current location
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(requireView(), getString(R.string.permision_requered), Snackbar.LENGTH_LONG).show();

            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> {
                    Snackbar.make(requireView(), e.getMessage(), Snackbar.LENGTH_LONG).show();

                }).addOnSuccessListener(location ->
        {
            compositeDisposable.add(iGoogleAPI.getDirections("driving",
                    "less driving",
                    new StringBuilder()
                            .append(location.getLatitude())
                            .append(",")
                            .append(location.getLongitude())
                            .toString(),
                    event.getPickupLocation(),
                    getString(R.string.google_api_keys))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(returnResult -> {

                        try {
                            //Parse json
                            JSONObject jsonObject = new JSONObject(returnResult);
                            JSONArray jsonArray = jsonObject.getJSONArray("routes");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject route = jsonArray.getJSONObject(i);
                                JSONObject poly = route.getJSONObject("overview_polyline");
                                String polyline = poly.getString("points");
                                polylineList = Common.decodePoly(polyline);
                            }

                            polylineOptions = new PolylineOptions();
                            polylineOptions.color(Color.GRAY);
                            polylineOptions.width(12);
                            polylineOptions.startCap(new SquareCap());
                            polylineOptions.jointType(JointType.ROUND);
                            polylineOptions.addAll(polylineList);
                            greyPolyline = mMap.addPolyline(polylineOptions);

                            blackPolylineOptions = new PolylineOptions();
                            blackPolylineOptions.color(Color.BLACK);
                            blackPolylineOptions.width(5);
                            blackPolylineOptions.startCap(new SquareCap());
                            blackPolylineOptions.jointType(JointType.ROUND);
                            blackPolylineOptions.addAll(polylineList);
                            blackPolyline = mMap.addPolyline(blackPolylineOptions);

                            //Animator
                            ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 100);
                            valueAnimator.setDuration(1100);
                            valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
                            valueAnimator.setInterpolator(new LinearInterpolator());
                            valueAnimator.addUpdateListener(value ->
                            {
                                List<LatLng> points = greyPolyline.getPoints();
                                int percentValue = (int) value.getAnimatedValue();
                                int size = points.size();
                                int newPoints = (int) (size * (percentValue / 100.0f));
                                List<LatLng> p = points.subList(0, newPoints);
                                blackPolyline.setPoints(p);
                            });
                            valueAnimator.start();

                            LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                            LatLng destination = new LatLng(Double.parseDouble(event.getPickupLocation().split(",")[0]),
                                    Double.parseDouble(event.getPickupLocation().split(",")[1]));

                            LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                    .include(origin)
                                    .include(destination)
                                    .build();

                            //add car icon for origin
                            JSONObject object = jsonArray.getJSONObject(0);
                            JSONArray legs = object.getJSONArray("legs");
                            JSONObject legObjects = legs.getJSONObject(0);

                            JSONObject time = legObjects.getJSONObject("duration");
                            String duration = time.getString("text");

                            JSONObject distanceEstimate = legObjects.getJSONObject("distance");
                            String distance = distanceEstimate.getString("text");

                            txt_estimate_time.setText(duration);
                            txt_estimate_distance.setText(distance);

                            mMap.addMarker(new MarkerOptions()
                                    .position(destination)
                                    .icon(BitmapDescriptorFactory.defaultMarker())
                                    .title("Pickup Location"));

                            createGeoFirePickupLocation(event.getKey(), destination);


                            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 160));
                            mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom - 1));

                            //show layout
                            chip_decline.setVisibility(View.VISIBLE);
                            layout_accept.setVisibility(View.VISIBLE);

                            //Count down
                            countDownEvent = Observable.interval(100, TimeUnit.MILLISECONDS)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .doOnNext(x -> {
                                        circularProgressBar.setProgress(circularProgressBar.getProgress() + 1f);
                                    }).takeUntil(aLong -> aLong == 100)
                                    .doOnComplete(() -> {
                                        createTripPlan(event, duration, distance);
                                    }).subscribe();


                        } catch (Exception e) {
                            //Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }));

        });
    }

    private void createGeoFirePickupLocation(String key, LatLng destination) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Common.TRIP_PICKUP_REF);
        pickupGeoFire = new GeoFire(ref);
        pickupGeoFire.setLocation(key, new GeoLocation(destination.latitude, destination.longitude), (key1, error) -> {
            if (error != null) {
                Snackbar.make(root_layout, error.getMessage(), Snackbar.LENGTH_LONG).show();
            } else {
                Log.d("EDMTDev", key1 + " was create success on geo Fire");
            }
        });
    }

    private void createTripPlan(DriverRequestReceived event, String duration, String distance) {
        setProgressLayout(true);
        //Sync server time with device
        FirebaseDatabase.getInstance()
                .getReference(".info/serverTimeOffset")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long timeOffset = snapshot.getValue(Long.class);

                        FirebaseDatabase.getInstance()
                                .getReference(Common.RIDER_INFO)
                                .child(event.getKey())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {

                                            RiderModel riderModel = snapshot.getValue(RiderModel.class);

                                            //get location
                                            if (ActivityCompat.checkSelfPermission(getContext(),
                                                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                                    &&
                                                    ActivityCompat.checkSelfPermission(getContext(),
                                                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                                Snackbar.make(mapFragment.getView(), getContext().getString(R.string.permision_requered), Snackbar.LENGTH_LONG).show();
                                                return;
                                            }
                                            fusedLocationProviderClient.getLastLocation().addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_LONG).show();

                                                }
                                            }).addOnSuccessListener(new OnSuccessListener<Location>() {
                                                @Override
                                                public void onSuccess(Location location) {

                                                    //Create trip planner
                                                    TripPlanModel tripPlanModel = new TripPlanModel();
                                                    tripPlanModel.setDriver(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                                    tripPlanModel.setRider(event.getKey());

                                                    tripPlanModel.setDriverInfoModel(Common.currentUser);
                                                    tripPlanModel.setRiderModel(riderModel);
                                                    tripPlanModel.setOrigin(event.getPickupLocation());
                                                    tripPlanModel.setOriginString(event.getPickupLocationString());
                                                    tripPlanModel.setDestination(event.getDestinationLocation());
                                                    tripPlanModel.setDestinationString(event.getDestinationLocationString());
                                                    tripPlanModel.setDistancePickup(distance);
                                                    tripPlanModel.setDurationPickup(duration);
                                                    tripPlanModel.setCurrentLat(location.getLatitude());
                                                    tripPlanModel.setCurrentLng(location.getLongitude());

                                                    tripNumberId = Common.createUniqueTripIdNumber(timeOffset);
                                                    FirebaseDatabase.getInstance().getReference(Common.Trip)
                                                            .child(tripNumberId)
                                                            .setValue(tripPlanModel)
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_LONG).show();
                                                                }
                                                            }).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                            txt_rider_name.setText(riderModel.getFirstName());
                                                            txt_start_uber_estimate_time.setText(duration);
                                                            txt_start_uber_estimate_distance.setText(distance);

                                                            setOfflineModeForDriver(event, duration, distance);

                                                        }
                                                    });


                                                }
                                            });

                                        } else {
                                            Snackbar.make(mapFragment.getView(), getContext().getString(R.string.rider_not_found) + " " + event.getKey(), Snackbar.LENGTH_LONG).show();
                                        }

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Snackbar.make(mapFragment.getView(), error.getMessage(), Snackbar.LENGTH_LONG).show();

                                    }
                                });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Snackbar.make(mapFragment.getView(), error.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void setOfflineModeForDriver(DriverRequestReceived event, String duration, String distance) {

        UserUtils.sendAcceptRequestToRider(mapFragment.getView(), getContext(), event.getKey(), tripNumberId);

        //Go to offline
        if (currentUserRef != null)
            currentUserRef.removeValue();

        setProgressLayout(false);
        layout_accept.setVisibility(View.GONE);
        layout_start_uber.setVisibility(View.VISIBLE);
        isTripStart = true;
    }

    private void setProgressLayout(boolean isProcess) {
        int color = -1;
        if (isProcess) {
            color = ContextCompat.getColor(getContext(), R.color.dark_gray);
            circularProgressBar.setIndeterminateMode(true);
            txt_rating.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_star, 0);
        } else {
            color = ContextCompat.getColor(getContext(), android.R.color.white);
            circularProgressBar.setIndeterminateMode(false);
            circularProgressBar.setProgress(0);
            txt_rating.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_star_white, 0);

        }

        txt_estimate_time.setTextColor(color);
        txt_estimate_distance.setTextColor(color);
        ImageViewCompat.setImageTintList(img_round, ColorStateList.valueOf(color));
        txt_rating.setTextColor(color);
        txt_type_uber.setTextColor(color);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onNotifyToRider(NotifyToRiderEvent event) {
        layout_notify_rider.setVisibility(View.VISIBLE);
        progress_notify.setMax(Common.WAIT_TIME_IN_MIN * 60);
        waiting_timer = new CountDownTimer(Common.WAIT_TIME_IN_MIN * 60 * 1000, 1000) {
            @Override
            public void onTick(long l) {
                progress_notify.setProgress(progress_notify.getProgress() + 1);
                txt_notify_rider.setText(String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(1) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(1)),
                        TimeUnit.MILLISECONDS.toSeconds(1) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(1))));


            }

            @Override
            public void onFinish() {
                Snackbar.make(root_layout, getString(R.string.time_over), Snackbar.LENGTH_LONG).show();

            }
        }.start();
    }
}