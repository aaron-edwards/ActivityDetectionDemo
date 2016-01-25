package com.aarone.activitydetectiondemo;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationRequest;

import butterknife.Bind;
import butterknife.ButterKnife;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Subscription;

public class CurrentActionActivity extends AppCompatActivity {
    private static final String TAG = CurrentActionActivity.class.getName();
    private Subscription detectActivitySubscription;

    @Bind(R.id.textView)
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_action);
        ButterKnife.bind(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(this);

        detectActivitySubscription = locationProvider.getDetectedActivity(0)
                .subscribe(result -> {
                    Log.d(TAG, "Detected Activity: " + result);
                    textView.setText(getDetectedActivityString(result));
                });
    }

    private String getDetectedActivityString(ActivityRecognitionResult result) {
        return Stream.of(result.getProbableActivities())
                .sortBy(activity -> 100 - activity.getConfidence())
                .map(activity -> String.format("%s : %d%%", getActivityString(this, activity.getType()), activity.getConfidence()))
                .collect(Collectors.joining("\n"));
    }

    @Override
    protected void onStop() {
        if( detectActivitySubscription != null && !detectActivitySubscription.isUnsubscribed()) {
            detectActivitySubscription.unsubscribe();
        }
        super.onStop();
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
