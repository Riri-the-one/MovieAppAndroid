package com.example.movies_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Carte Google Maps : position utilisateur + cinémas simulés à proximité.
 */
public class CinemaMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION = 300;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    /** Noms de cinémas simulés autour de la position. */
    private static final String[] CINEMA_NAMES = {
            "Gaumont Pathé",
            "UGC Ciné Cité",
            "MK2 Bibliothèque",
            "Cinéma Le Grand Rex"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cinema_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        if (hasLocationPermission()) {
            enableUserLocation();
            centerOnUserAndShowCinemas();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION);
        }
    }

    /** Active le point bleu « ma position » sur la carte. */
    private void enableUserLocation() {
        if (!hasLocationPermission() || googleMap == null) {
            return;
        }
        try {
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException ignored) {
        }
    }

    /** Récupère la position GPS et place les marqueurs cinéma. */
    private void centerOnUserAndShowCinemas() {
        if (!hasLocationPermission()) {
            return;
        }
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        LatLng center;
                        if (location != null) {
                            center = new LatLng(location.getLatitude(), location.getLongitude());
                        } else {
                            // Position par défaut (Paris) si GPS indisponible
                            center = new LatLng(48.8566, 2.3522);
                            Toast.makeText(this,
                                    "GPS indisponible, affichage carte par défaut.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 14f));
                        googleMap.addMarker(new MarkerOptions()
                                .position(center)
                                .title(getString(R.string.your_position))
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_AZURE)));
                        addNearbyCinemaMarkers(center);
                    });
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show();
        }
    }

    /** Simule des cinémas à proximité avec des décalages autour du centre. */
    private void addNearbyCinemaMarkers(LatLng center) {
        double[][] offsets = {
                {0.012, 0.008},
                {-0.009, 0.011},
                {0.007, -0.010},
                {-0.011, -0.007}
        };
        for (int i = 0; i < CINEMA_NAMES.length; i++) {
            LatLng cinemaPos = new LatLng(
                    center.latitude + offsets[i][0],
                    center.longitude + offsets[i][1]);
            googleMap.addMarker(new MarkerOptions()
                    .position(cinemaPos)
                    .title(CINEMA_NAMES[i])
                    .snippet("Cinéma à proximité")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED)));
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
                centerOnUserAndShowCinemas();
            } else {
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show();
                // Carte visible sans position : Paris + cinémas simulés
                LatLng paris = new LatLng(48.8566, 2.3522);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(paris, 13f));
                addNearbyCinemaMarkers(paris);
            }
        }
    }
}
