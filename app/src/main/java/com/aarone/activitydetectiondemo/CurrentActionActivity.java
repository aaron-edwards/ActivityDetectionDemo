package com.aarone.activitydetectiondemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationRequest;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Subscription;

public class CurrentActionActivity extends FragmentActivity {
    private static final String TAG = CurrentActionActivity.class.getName();
    private static final int PERMISSION_REQUEST_CODE = 1;

    private ReactiveLocationProvider locationProvider;
    private Subscription locationSubscription;
    private Subscription detectActivitySubscription;

    @Bind(R.id.textCurrentActivity)
    TextView currentActivityText;
    @Bind(R.id.textLocation)
    TextView locationText;
    @Bind(R.id.textAddress)
    TextView addressText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_action);
        ButterKnife.bind(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationProvider = new ReactiveLocationProvider(this);

        detectActivitySubscription = locationProvider.getDetectedActivity(0)
            .subscribe(result -> {
                Log.d(TAG, "Detected Activity: " + result);
                currentActivityText.setText(getDetectedActivityString(result));
            });

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            pollLocation();
        }

    }

    private void pollLocation() {
        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000);

        locationSubscription = locationProvider.getUpdatedLocation(request)
                .flatMap(location -> {
                    Log.d(TAG, "Location: " + location);
                    locationText.setText(
                            "Latitude: " + location.getLatitude() + "\n" +
                                    "Longitude: " + location.getLongitude() + "\n" +
                                    "Altitude: " + location.getAltitude());
                    return locationProvider.getReverseGeocodeObservable(location.getLatitude(), location.getLongitude(), 1);
                })
                .filter(addresses -> ! addresses.isEmpty())
                .map(addresses -> addresses.get(0))
                .map(address -> constructAddressString(address))
                .subscribe(address -> addressText.setText(address));
    }

    private String constructAddressString(Address address) {
        StringBuilder addressStringBuilder = new StringBuilder();
        for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
            addressStringBuilder.append(address.getAddressLine(i));
            addressStringBuilder.append("\n");
        }
        return addressStringBuilder.toString();
    }


    private String getDetectedActivityString(ActivityRecognitionResult result) {
        return Stream.of(result.getProbableActivities())
                .sortBy(activity -> 100 - activity.getConfidence())
                .map(activity -> String.format("%s : %d%%", getActivityString(this, activity.getType()), activity.getConfidence()))
                .collect(Collectors.joining("\n"));
    }

    @Override
    protected void onStop() {
        unsubscribe(locationSubscription);
        unsubscribe(detectActivitySubscription);
        super.onStop();
    }

    private void unsubscribe(Subscription subscription) {
        if( subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pollLocation();
                }
            }}
    }


    public static String getActivityString(Context context, int detectedActivityType) {
        Resources resources = context.getResources();
        switch (detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.in_vehicle);
            case DetectedActivity.ON_BICYCLE:
                return resources.getString(R.string.on_bicycle);
            case DetectedActivity.ON_FOOT:
                return resources.getString(R.string.on_foot);
            case DetectedActivity.RUNNING:
                return resources.getString(R.string.running);
            case DetectedActivity.STILL:
                return resources.getString(R.string.still);
            case DetectedActivity.TILTING:
                return resources.getString(R.string.tilting);
            case DetectedActivity.UNKNOWN:
                return resources.getString(R.string.unknown);
            case DetectedActivity.WALKING:
                return resources.getString(R.string.walking);
            default:
                return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
        }
    }
}
