package com.shahad.rsfattendence;
/*
 * Created by SHAHAD MAHMUD on 7/16/20
 */

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class ActivityLogin extends AppCompatActivity {
    private static final String TAG = "LOGIN_ACT";
    private static final int REQ_PHN_STATE_PER = 936;
    private static final int REQ_INTERNET_PER = 711;
    private static final int REQ_LOC_PER = 345;

    private TextInputEditText user_id_input, user_pass_input;
    private Button login_button;

    private String imei_num = "";
    private RequestQueue queue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        queue = Volley.newRequestQueue(ActivityLogin.this);

        // get all the elements
        getElements();

        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the IMEI number
                getImeiNum();
                Log.i(TAG, imei_num);
                getAccessCode();
                getDefaultDeviceId();
                getLocation();
            }
        });
    }

    private void getElements() {
        user_id_input = findViewById(R.id.login_user_id);
        user_pass_input = findViewById(R.id.login_user_pass);

        login_button = findViewById(R.id.login_button);
    }

    private void getImeiNum() {
        // first check for permission
        if (ContextCompat.checkSelfPermission(ActivityLogin.this,
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // permission is already granted. Get the number
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            assert telephonyManager != null;
            if (Build.VERSION.SDK_INT >= 26) {
                imei_num = telephonyManager.getImei(0);
            } else {
                imei_num = telephonyManager.getDeviceId(0);
            }
        } else {
            //permission is not granted. Ask for permission
            ActivityCompat.requestPermissions(
                    ActivityLogin.this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    REQ_PHN_STATE_PER
            );
        }
    }

    private void getLocation() {
        // first check for permission
        if (ContextCompat.checkSelfPermission(ActivityLogin.this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(ActivityLogin.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // permission is already granted. Get the Location
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            assert locationManager != null;
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // gps is not enabled. ask to on gps
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("We need to access your current location to continue. Enable Location Service?").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        Toast.makeText(ActivityLogin.this,
                                "Location service is turned off. Can not log in.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                final AlertDialog alertDialog = builder.create();
                alertDialog.show();
            } else {
                Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (locationGPS != null) {
                    double lat = locationGPS.getLatitude();
                    double longi = locationGPS.getLongitude();
                    String latitude = String.valueOf(lat);
                    String longitude = String.valueOf(longi);

                    Log.i(TAG, longitude + " " + latitude);
                } else {
                    Toast.makeText(this, "Unable to find location. Can not log in.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            //permission is not granted. Ask for permission
            ActivityCompat.requestPermissions(ActivityLogin.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOC_PER
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_PHN_STATE_PER) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission was granted
                getImeiNum();
            } else {
                Toast.makeText(this, "Permission was not granted." +
                        " Can not log in.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_LOC_PER) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission was granted
                getLocation();
            } else {
                Toast.makeText(this, "Permission was not granted." +
                        " Can not log in.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getAccessCode() {
        String url = "https://202.164.211.110/RASolarERPWebServices/RASolarERP_SecurityAdmin.svc/" +
                "ServiceAccessCode?ServiceAccessUserName=RSFWSA&ServiceAccessPassword=abcd12345";
        JsonObjectRequest request = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null) {
                            Log.i(TAG, response.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(request);
    }

    private void getDefaultDeviceId() {
        String url = "https://202.164.211.110/RASolarERPWebServices/RASolarERP_SecurityAdmin.svc/" +
                "DefaultDeviceUserID?DeviceID=356096060016845&DeviceAppVersionNo=02.7.0.0" +
                "&ServiceAccessUserName=RSFWSA&ServiceAccessCode=abcd12345";

        final JsonObjectRequest request = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null) {
                            Log.i(TAG, response.toString());

                            try {
                                JSONArray jsonArray = response.getJSONArray("SecurityInfo");
                                JSONObject info = jsonArray.getJSONObject(0);
                                String deviceId = info.getString("DefaultDeviceUserID");
                                Log.i(TAG, info.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(request);
    }
}
