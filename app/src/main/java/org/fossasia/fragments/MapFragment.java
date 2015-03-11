package org.fossasia.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.fossasia.R;

import java.util.Locale;

public class MapFragment extends Fragment {

    private static final double DESTINATION_LATITUDE = 1.29677;
    private static final double DESTINATION_LONGITUDE = 103.786914;
    private static final String DESTINATION_NAME = "Plug-In@Blk71";
    private GoogleMap mMap;
    private int resultCode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_map, container, false);

        resultCode= GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        if(resultCode != ConnectionResult.SUCCESS)
        {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(), 69);
            dialog.setCancelable(true);

            dialog.show();
        }
        else {
            mMap = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map)).getMap();


            MarkerOptions markerOptions;
            LatLng position;

            markerOptions = new MarkerOptions();

            if (isGoogleMapsInstalled()) {
                position = new LatLng(DESTINATION_LATITUDE, DESTINATION_LONGITUDE);
                markerOptions.position(position);
                markerOptions.title(DESTINATION_NAME);
                mMap.addMarker(markerOptions);
                CameraUpdate cameraPosition = CameraUpdateFactory.newLatLngZoom(position, 15.0f);
                mMap.animateCamera(cameraPosition);
            }
            else
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("Please install Google Maps");
                builder.setCancelable(true);
                builder.setPositiveButton("Install", getGoogleMapsListener());
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
        return v;
    }

    /*
    *
    * Check GoogleMaps App is installed or Not in phone
    * */
    public boolean isGoogleMapsInstalled()
    {
        try
        {
            ApplicationInfo info = getActivity().getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0);
            return true;
        }
        catch(PackageManager.NameNotFoundException e)
        {
            return false;
        }
    }

    /*
    *
    * If GoogleMaps is not , then install from Google Playstore
    *
    * */
    public DialogInterface.OnClickListener getGoogleMapsListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps"));
                startActivity(intent);


            }
        };
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.map, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.directions:
                launchDirections();
                return true;
        }
        return false;
    }

    private void launchDirections() {
        // Build intent to start Google Maps directions
        String uri = String.format(Locale.US,
                "https://www.google.com/maps/search/%1$s/@%2$f,%3$f,17z",
                DESTINATION_NAME, DESTINATION_LATITUDE, DESTINATION_LONGITUDE);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

        startActivity(intent);
    }
}
