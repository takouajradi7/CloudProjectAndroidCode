package com.example.suividelocalisation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String SERVER_URL = "http://192.168.1.41:8080"; // Mettez à jour avec votre URL de serveur

    private Handler locationUpdateHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkLocationPermission()) {
            startLocationUpdates();
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void startLocationUpdates() {
        Runnable locationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                // Create a FusedLocationProviderClient.
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

                // Check for permission before accessing location.
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                // Now you have the actual latitude and longitude values.
                                // You can send this data to the server.
                                sendLocationToServer(latitude, longitude);
                            }
                        }
                    });
                }

                //locationUpdateHandler.postDelayed(this, 300000); // Update every 5 minutes
            }
        };

        locationUpdateHandler.post(locationUpdateRunnable);
    }


    private void sendLocationToServer(double latitude, double longitude) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("LocationSent", "Position envoyée au serveur : Latitude " + latitude + " Longitude " + longitude);
                    URL url = new URL(SERVER_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; utf-8");
                    connection.setDoOutput(true);

                    // Créer un objet JSON avec la latitude et la longitude
                    JSONObject jsonInput = new JSONObject();
                    jsonInput.put("latitude", latitude);
                    jsonInput.put("longitude", longitude);

                    // Convertir l'objet JSON en chaîne
                    String jsonInputString = jsonInput.toString();

                    // Écrire la chaîne JSON dans le flux de sortie de la connexion
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = jsonInputString.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    // Lire la réponse du serveur
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine = null;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        System.out.println(response.toString());
                    }

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Log.d("LocationSent", "Position envoyée au serveur : Latitude " + latitude + " Longitude " + longitude);
                    } else {
                        Log.e("ServerError", "Erreur lors de l'envoi au serveur");
                    }

                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
