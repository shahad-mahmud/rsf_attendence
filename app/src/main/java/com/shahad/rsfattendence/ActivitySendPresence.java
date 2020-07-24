package com.shahad.rsfattendence;

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
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
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
import com.shahad.rsfattendence.helperClasses.InternetCheck;
import com.shahad.rsfattendence.helperClasses.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/*
 * Created by SHAHAD MAHMUD on 7/22/20
 */
public class ActivitySendPresence extends AppCompatActivity {
    private static final String TAG = "SEND_PRESENCE";
    private static final String SERVICE_ACCESS_USER_NAME = "RSFWSA";
    private static final String SERVICE_ACCESS_CODE = "abcd12345";

    private RequestQueue queue;
    private MaterialAlertDialogBuilder dialogBuilder;
    private LoadingDialog loadingDialog;

    private String imei_num = "";

    private String latitude;
    private String longitude;

    //-------------UI elements--------------------
    private TextView promptTextView;
    private Spinner customerSpinner;
    private MaterialButton checkCustomerButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence);

        queue = Volley.newRequestQueue(ActivitySendPresence.this);
        dialogBuilder = new MaterialAlertDialogBuilder(ActivitySendPresence.this);
        loadingDialog = new LoadingDialog(ActivitySendPresence.this);

        findElements();

        getImeiNum();
        getLocation();

        checkCustomerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check internet
                new InternetCheck(new InternetCheck.Consumer() {
                    @Override
                    public void accept(Boolean internet) {
                        if (internet) {
                            if (imei_num != null && latitude != null && longitude != null) {
                                getCustomers();
                            } else {
                                showDialog("Error!", "Ops! Can not read the IMEI " +
                                        "number or Location. Please try again.");
                                getImeiNum();
                            }
                        } else {
                            showDialog("No internet", "Ops! Internet is not available. Please connect to the " +
                                    "internet and try again!");
                        }
                    }
                });
            }
        });
    }

    void findElements() {
        promptTextView = findViewById(R.id.presence_prompt_text);

        customerSpinner = findViewById(R.id.presence_spinner);

        checkCustomerButton = findViewById(R.id.presence_check_customer_button);
    }

    void showDialog(String title, String message) {
        dialogBuilder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    void populateSpinner(ArrayList<String> list) {
        promptTextView.setVisibility(View.GONE);
        customerSpinner.setVisibility(View.VISIBLE);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        customerSpinner.setAdapter(adapter);
    }

    private void getImeiNum() {
        loadingDialog.startLoadingDialog("Reading IMEI number...");
        // first check for permission
        if (ContextCompat.checkSelfPermission(ActivitySendPresence.this,
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
            loadingDialog.dismissLoadingDialog();
        } else {
            loadingDialog.dismissLoadingDialog();
            //permission is not granted. Show error
            showDialog("Permission not granted", "Permission to read IMEI number is not " +
                    "granted. Please log in again with granting the permissions.");
        }
    }

    private void getLocation() {
        loadingDialog.startLoadingDialog("Reading current location...");
        // first check for permission
        if (ContextCompat.checkSelfPermission(ActivitySendPresence.this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(ActivitySendPresence.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // permission is already granted. Get the Location
            final LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(2000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.getFusedLocationProviderClient(ActivitySendPresence.this)
                    .requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            super.onLocationResult(locationResult);

                            LocationServices.getFusedLocationProviderClient(ActivitySendPresence.this)
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
                            loadingDialog.dismissLoadingDialog();
                        }
                    }, Looper.getMainLooper());
        } else {
            loadingDialog.dismissLoadingDialog();
            //permission is not granted. Ask for permission
            showDialog("Permission not granted", "Permission to read Location is not " +
                    "granted. Please log in again with granting the permissions.");
        }
    }

    void getCustomers() {
        loadingDialog.startLoadingDialog("Fetching Customer List...");

        String url = "https://202.164.211.110/RASolarERPWebServices/RASolarERP_SecurityAdmin.svc/" +
                "CustomerInfo?DeviceID=" + imei_num + "&PanelSerialNo=&BatterySerialNo=&LocationLatitude=" +
                latitude + "&LocationLongitude=" + longitude + "&ServiceAccessUserName=" +
                SERVICE_ACCESS_USER_NAME + "&ServiceAccessCode=" + SERVICE_ACCESS_CODE;

        Log.d(TAG + " cus url", url);

        final JsonObjectRequest request = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null) {
                            Log.d(TAG + " LOGIN", response.toString());
                            try {
                                JSONArray jsonArray = response.getJSONArray("CustomerInfo");
                                int lenOfArray = jsonArray.length();
                                ArrayList<String> cusList = new ArrayList<String>();

                                for (int i = 0; i < lenOfArray; i++) {
                                    JSONObject object = jsonArray.getJSONObject(i);
                                    String temp = object.getString("CustomerName");
                                    Log.d(TAG + " cus Str", temp);
                                    cusList.add(temp);
                                }

                                populateSpinner(cusList);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                        loadingDialog.dismissLoadingDialog();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                Log.e(TAG + " Access code", Objects.requireNonNull(error.getMessage()));
                error.printStackTrace();
                loadingDialog.dismissLoadingDialog();
                showDialog("Error", "Error occurred while fetching the customer" +
                        " list. Please try again.");
            }
        });

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(request);
    }
}
