package com.shahad.rsfattendence;
/*
 * Created by SHAHAD MAHMUD on 7/16/20
 */

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
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
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.shahad.rsfattendence.helperClasses.InternetCheck;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class ActivityLogin extends AppCompatActivity {
    private static final String TAG = "LOGIN_ACT";
    private static final String SERVICE_ACCESS_USER_NAME = "RSFWSA";
    private static final String SERVICE_ACCESS_CODE = "abcd12345";

    private static final int REQ_PHN_STATE_PER = 936;
    private static final int REQ_INTERNET_PER = 711;
    private static final int REQ_LOC_PER = 345;
    private static final int REQ_ALL_PER = 11;

    private TextInputEditText user_id_input, user_pass_input;
    private MaterialButton login_button;

    private String imei_num = "";
    private RequestQueue queue;

    private String latitude;
    private String longitude;

    private String deviceId;
    private MaterialAlertDialogBuilder dialogBuilder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        queue = Volley.newRequestQueue(ActivityLogin.this);
        dialogBuilder = new MaterialAlertDialogBuilder(ActivityLogin.this);

        // get all the elements
        getElements();
        getPermissionsAndOthers();


        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // get user name input and password
                final String user_id = Objects.requireNonNull(user_id_input.getText()).toString();
                final String pass = Objects.requireNonNull(user_pass_input.getText()).toString();

                new InternetCheck(new InternetCheck.Consumer() {
                    @Override
                    public void accept(Boolean internet) {
                        if (internet) {
                            getAccessCode();

                            if (imei_num != null) {
                                getDefaultDeviceId(imei_num);


                                if (latitude != null && longitude != null) {
                                    performLogin(imei_num, user_id, pass, latitude, longitude);
                                } else {
                                    showDialog("Ops! Can not read the location information. Please try again.");
                                    getLocation();
                                }
                            } else {
                                showDialog("Ops! Can not read the IMEI number. Please try again.");
                                getImeiNum();
                            }
                        } else {
                            showDialog("Ops! Internet is not available. Please connect to the " +
                                    "internet and try again!");
                        }
                    }
                });

            }
        });
    }

    void showDialog(String message) {
        dialogBuilder.setTitle("No internet")
                .setMessage(message)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    private void getElements() {
        user_id_input = findViewById(R.id.login_user_id);
        user_pass_input = findViewById(R.id.login_user_pass);

        login_button = findViewById(R.id.login_button);
    }

    private void getPermissionsAndOthers() {
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(ActivityLogin.this,
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            getImeiNum();
        } else {
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (ContextCompat.checkSelfPermission(ActivityLogin.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //get Location
            getLocation();
        } else {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), REQ_ALL_PER);
        }
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
                Log.i(TAG + " IMEI", imei_num);
            } else {
                imei_num = telephonyManager.getDeviceId(0);
                Log.i(TAG + " IMEI", imei_num);
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
            final LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(2000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.getFusedLocationProviderClient(ActivityLogin.this)
                    .requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            super.onLocationResult(locationResult);

                            LocationServices.getFusedLocationProviderClient(ActivityLogin.this)
                                    .removeLocationUpdates(this);
                            if (locationResult != null && locationResult.getLocations().size() > 0) {
                                int lastLocIndex = locationResult.getLocations().size() - 1;
                                double f_latitude = locationResult.getLocations().get(lastLocIndex)
                                        .getLatitude();
                                double f_longitude = locationResult.getLocations().get(lastLocIndex)
                                        .getLongitude();

                                latitude = String.valueOf(f_latitude);
                                longitude = String.valueOf(f_longitude);
                                Log.i(
                                        TAG + " Location",
                                        "lat: " + latitude
                                                + "long: " + longitude
                                );
                            }
                        }
                    }, Looper.getMainLooper());
        } else {
            //permission is not granted. Ask for permission
            ActivityCompat.requestPermissions(ActivityLogin.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOC_PER
            );
        }
    }

    private void showDialog(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
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
        } else if (requestCode == REQ_ALL_PER) {
            Map<String, Integer> perms = new HashMap<>();
            // Initialize the map with both permissions
            perms.put(Manifest.permission.READ_PHONE_STATE, PackageManager.PERMISSION_GRANTED);
            perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);

            if (grantResults.length > 0) {
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                if (perms.get(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "sms & location services permission granted");
                    getPermissionsAndOthers();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_PHONE_STATE)
                            || ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showDialog("Phone state and Location Services Permissions required for this app to continue",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                getPermissionsAndOthers();
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                break;
                                        }
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG)
                                .show();
                    }
                }
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
                            Log.d(TAG + " Access Code", response.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                Log.e(TAG + " Access code", Objects.requireNonNull(error.getMessage()));
                error.printStackTrace();
            }
        });

        queue.add(request);
    }

    private void getDefaultDeviceId(String imei) {
        String url = "https://202.164.211.110/RASolarERPWebServices/RASolarERP_SecurityAdmin.svc/" +
                "DefaultDeviceUserID?DeviceID=" + imei + "&DeviceAppVersionNo=02.7.0.0" +
                "&ServiceAccessUserName=" + SERVICE_ACCESS_USER_NAME + "&ServiceAccessCode=" + SERVICE_ACCESS_CODE;

        final JsonObjectRequest request = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null) {
                            Log.d(TAG + " Device Id", response.toString());

                            try {
                                JSONArray jsonArray = response.getJSONArray("SecurityInfo");
                                JSONObject info = jsonArray.getJSONObject(0);
                                deviceId = info.getString("DefaultDeviceUserID");
                                Log.d(TAG + " Device Id", deviceId.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
//                Log.e(TAG + " Access code", Objects.requireNonNull(error.getMessage()));
            }
        });

        queue.add(request);
    }

    void performLogin(String deviceId, String used_id, String user_pass, String lat, String lon) {
        Log.d(TAG + " user_id", used_id);
        Log.d(TAG + " pass", user_pass);
        String url = "https://202.164.211.110/RASolarERPWebServices/RASolarERP_SecurityAdmin.svc/" +
                "LoginToRASolarERPMobileApp?DeviceID=" + deviceId + "&DeviceAppVersionNo=02.7.0.0&" +
                "MobileAppUserID=" + used_id + "&MobileAppUserPassword=" + user_pass +
                "&LocationLatitude=" + lat + "&LocationLongitude=" + lon + "&ServiceAccessUserName="
                + ActivityLogin.SERVICE_ACCESS_USER_NAME + "&ServiceAccessCode=" + ActivityLogin.SERVICE_ACCESS_CODE;

        Log.d(TAG + " log url", url);

        final JsonObjectRequest request = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null) {
                            Log.d(TAG + " LOGIN", response.toString());

                            try {
                                JSONArray jsonArray = response.getJSONArray("SecurityInfo");
                                JSONObject info = jsonArray.getJSONObject(0);
//                                String deviceId = info.getString("DefaultDeviceUserID");
                                Log.d(TAG + " LOGIN", info.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                Log.e(TAG + " Access code", Objects.requireNonNull(error.getMessage()));
                error.printStackTrace();
            }
        });

        queue.add(request);
    }
}
