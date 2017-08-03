package fr.woorib.beacon.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import fr.woorib.backand.client.exception.BackandException;
import fr.woorib.beacon.data.BeaconEntry;
import fr.woorib.beacon.persistance.backand.BackandStore;
import fr.woorib.beacon.services.BeaconService;
import fr.woorib.beacon.services.BeaconServiceImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String JAVA_VERSION_PROPERTY = "java.version";

    private BeaconService service;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        File file = this.getDir("" + System.currentTimeMillis(), Context.MODE_PRIVATE);

        System.setProperty("buddy.folder", file.getAbsolutePath());
        super.onCreate(savedInstanceState);
        Handler fh = new ConsoleHandler();
        fh.setLevel(Level.FINEST);
        Logger logger = Logger.getLogger("fr.woorib");
        logger.addHandler(fh);
        logger.setLevel(Level.FINEST);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        try {
            new RetrieveClientTask().execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        List<BeaconEntry> userBeacons = new ArrayList<>();
        try {
            userBeacons.addAll(new BeaconRetrieverTask().execute().get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        for (BeaconEntry entry : userBeacons) {
            System.out.println(entry);
            LatLng latLng = new LatLng(entry.getLatitude(), entry.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title(entry.getDescription()));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }

    private class RetrieveClientTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                service = new BeaconServiceImpl(new BackandStore("username", "password"));
            } catch (BackandException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class BeaconRetrieverTask extends AsyncTask<Integer, Void, List<BeaconEntry>>{
        @Override
        protected List<BeaconEntry> doInBackground(Integer... integers) {
            return service.getUserBeacons(null);
        }
    }
}
