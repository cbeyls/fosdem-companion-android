package be.digitalia.fosdem.fragments;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import be.digitalia.fosdem.R;

public class MapFragment extends AbstractMapFragment implements GoogleApiClient.ConnectionCallbacks, LocationListener {

    private static final LatLng TOP_LEFT = new LatLng(50.81495814,4.37645320);
    private static final LatLng TOP_RIGHT = new LatLng(50.8166223,4.38251400);
    private static final LatLng BOTTOM_RIGHT = new LatLng(50.81110086,4.38665628);
    private static final LatLng BOTTOM_LEFT = new LatLng(50.8094395,4.3812597);

    private static final double M_TOP = getM(TOP_LEFT, TOP_RIGHT);
    private static final double P_TOP = getP(TOP_RIGHT, M_TOP);
    private static final double M_RIGHT = getM(TOP_RIGHT, BOTTOM_RIGHT);
    private static final double P_RIGHT = getP(BOTTOM_RIGHT, M_RIGHT);
    private static final double M_BOTTOM = getM(BOTTOM_LEFT, BOTTOM_RIGHT);
    private static final double P_BOTTOM = getP(BOTTOM_RIGHT, M_BOTTOM);
    private static final double M_LEFT = getM(TOP_LEFT, BOTTOM_LEFT);
    private static final double P_LEFT = getP(BOTTOM_LEFT, M_LEFT);

    private static final double DISTANCE_BETWEEN_LEFT_AND_RIGHT;
    static {
        DISTANCE_BETWEEN_LEFT_AND_RIGHT = Math.sqrt(
                Math.pow(TOP_RIGHT.longitude - TOP_LEFT.longitude,2) +
                        Math.pow(TOP_RIGHT.latitude - TOP_LEFT.latitude, 2)
        );
    }
    private static final double DISTANCE_BETWEEN_TOP_AND_BOTTOM;
    static {
        DISTANCE_BETWEEN_TOP_AND_BOTTOM = Math.sqrt(
                Math.pow(TOP_RIGHT.longitude - BOTTOM_RIGHT.longitude,2) +
                        Math.pow(TOP_RIGHT.latitude - BOTTOM_RIGHT.latitude, 2)
        );
    }

    private static final long POSITION_UPDATE_INTERVAL_IN_MS = 10000l;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private ImageView mIvPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(POSITION_UPDATE_INTERVAL_IN_MS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_map, container, false);
	}

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mIvPosition = (ImageView) view.findViewById(R.id.ivPosition);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    private boolean isInsideZone(LatLng pos) {
        return    isAbove(pos, M_LEFT  , P_LEFT)    &&
                  isUnder(pos, M_TOP   , P_TOP)     &&
                  isUnder(pos, M_RIGHT , P_RIGHT)   &&
                  isAbove(pos, M_BOTTOM, P_BOTTOM);
    }

    private boolean isUnder(LatLng pos, double m, double p) {
        return getRemotenessFromBorder(pos, m, p) <= 0;
    }

    private boolean isAbove(LatLng pos, double m, double p) {
        return getRemotenessFromBorder(pos, m, p) >= 0;
    }

    private double getRemotenessFromBorder(LatLng pos, double m, double p) {
        return pos.latitude - m*pos.longitude - p;
    }

    private static double getM(LatLng pos1, LatLng pos2) {
        if (pos1.longitude - pos2.longitude != 0) {
            return (pos1.latitude - pos2.latitude) / (pos1.longitude - pos2.longitude);
        }

        return 0;
    }

    private static double getP(LatLng pos2, double m) {
        return pos2.latitude - (pos2.longitude * m);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (lastLocation != null) {
            setPositionOnMap(lastLocation);
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void setPositionOnMap(Location pos) {
        LatLng latLng = new LatLng(pos.getLatitude(), pos.getLongitude());
        if (isInsideZone(latLng)) {
            mIvPosition.setVisibility(View.VISIBLE);
            setPositionImage(latLng);
        } else {
            mIvPosition.setVisibility(View.GONE);
        }
    }

    private void setPositionImage(LatLng latLng) {
        View v = getView();
        if (v == null) {
            return;
        }
        int left = (int) Math.round(getDistanceFromBorder(latLng, M_LEFT, P_LEFT) / DISTANCE_BETWEEN_LEFT_AND_RIGHT * v.getWidth());
        int top = (int) Math.round(getDistanceFromBorder(latLng, M_TOP, P_TOP) / DISTANCE_BETWEEN_TOP_AND_BOTTOM * v.getWidth());
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mIvPosition.getLayoutParams();
        if (lp == null) {
            lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        lp.setMargins(left - mIvPosition.getWidth()/2, top - mIvPosition.getHeight()/2, 0, 0);
        mIvPosition.setLayoutParams(lp);
    }

    private double getDistanceFromBorder(LatLng latLng, double m, double p) {
        double remoteness = getRemotenessFromBorder(latLng, m, p);

        return Math.abs(remoteness) / Math.sqrt((m*m)+1);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        setPositionOnMap(location);
    }
}
