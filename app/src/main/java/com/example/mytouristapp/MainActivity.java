package com.example.mytouristapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.CollationElementIterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FAST_UPDATE_INTERVAL = 5;
    private static final int PERMISSIONS_FINE_LOCATION = 99;

    boolean updateOn = false;  //variable to remember if we are tracking location or not
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_wayPointCounts;
    Button btn_newWaypoint, btn_showWaypointList;
    Switch sw_locationupdates, sw_gps;

    Location currentLocation;

    List<Location> savedLocations;



    LocationRequest locationRequest = LocationRequest.create();   //Location request is a config file for all settings related to FusedLocationProvider
    FusedLocationProviderClient fusedLocationProviderClient;      //Google's API for location services.
    private LocationCallback locationCallBack;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_wayPointCounts = findViewById(R.id.tv_countOfCrumbs);
        btn_newWaypoint = findViewById(R.id.btn_newWayPoint);
        btn_showWaypointList = findViewById(R.id.btn_showWayPointsList);

        sw_gps = findViewById(R.id.sw_gps);

        sw_locationupdates = findViewById(R.id.sw_locationsupdates);

        //set all properties of LocationRequest

        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);

        locationRequest.setFastestInterval(1000 * FAST_UPDATE_INTERVAL);

        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    updateUIValues(location);
                }
            }
        };


        btn_newWaypoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get the gps location

                //add the new location to the list
                MyApplication myApplication = (MyApplication)getApplicationContext();
                savedLocations = myApplication.getMyLocations();
                savedLocations.add(currentLocation);
            }
        });

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_gps.isChecked()) {
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Usando GPS");  //most accurate - use GPS
                } else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Usando torres de WIFI");
                }
            }
        });
        sw_locationupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_locationupdates.isChecked()) {
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        });

        updateGPS();
    } //end onCreate method

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){
            case PERMISSIONS_FINE_LOCATION:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    updateGPS();
                }else{
                    Toast.makeText(this, "La aplicación requiere permisos para funcionar correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void updateGPS(){
        //get permissions from the user to track GPS
        //get the current location from the fused client
        //update the UI - i.e. set all properties in their associated text view items.

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        if(ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            //user provided permission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //we got permissions. Put the values of location. XXX into the UI components.
                    if (location != null) {
                        updateUIValues(location);
                        currentLocation = location;
                    }
                }
            });
        }else{
            //permission not granted
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                requestPermissions( new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    private void updateUIValues(Location location) {
        //update all of the text view objects with a new location.

        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));

        if(location.hasAltitude()){
            tv_altitude.setText(String.valueOf(location.getAltitude()));
        }else{
            tv_altitude.setText("No disponible");
        }

        if(location.hasSpeed()){
            tv_speed.setText(String.valueOf(location.getSpeed()));
        }else{
            tv_speed.setText("No disponible");
        }

        Geocoder geocoder = new Geocoder(MainActivity.this);

        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(), 1);
            tv_address.setText(addresses.get(0).getAddressLine(0));
        }catch (Exception e){
            tv_address.setText("No fue posible obtener la dirección de la calle");
        }


        MyApplication myApplication = (MyApplication)getApplicationContext();
        savedLocations = myApplication.getMyLocations();

        //mostrar la cantidad de waypoints
        tv_wayPointCounts.setText(Integer.toString(savedLocations.size()));

    }

    private void startLocationUpdates() {
        tv_updates.setText("La ubicación está siendo rastreada");
        if(ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null); //metodo para updatear la location desde el fusedProvider
        }

    }

    private void stopLocationUpdates() {
        tv_updates.setText("La ubicación no está siendo rastreada");
        tv_lat.setText("No está siendo rastreado");
        tv_lon.setText("No está siendo rastreado");
        tv_accuracy.setText("No está siendo rastreado");
        tv_speed.setText("No está siendo rastreado");
        tv_altitude.setText("No está siendo rastreado");
        tv_sensor.setText("No está siendo rastreado");
        tv_address.setText("No está siendo rastreado");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }

}