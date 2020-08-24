package com.shahad.rsfattendence;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.shahad.rsfattendence.helperClasses.IconDialog;
import com.shahad.rsfattendence.helperClasses.InternetCheck;
import com.shahad.rsfattendence.helperClasses.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

/*
 * Created by SHAHAD MAHMUD on 7/22/20
 */
public class ActivitySendPresence extends AppCompatActivity {
    private static final String TAG = "SEND_PRESENCE";
    private static final String SERVICE_ACCESS_USER_NAME = "RSFWSA";
    private static final String SERVICE_ACCESS_CODE = "abcd12345";

    private static final String SHARED_PREF_FILE_NAME = "sharedPrefForLogIn";
    private static final String SHARED_PREF_KEY_IS_LOGGED_IN = "loginStatus";
    private static final String SHARED_PREF_KEY_LOGIN_TIME = "loginTime";

    private static final int AUTO_LOGIN_DURATION = 1800000; // automatic login if last login time was
    // less than 30 minutes.
    private static final int BACK_PRESS_TIME_INTERVAL = 2500; // time in milliseconds

    private static final int REQ_CODE_PANEL = 513;
    private static final int REQ_CODE_BATTERY = 500;
    private SharedPreferences sharedPreferences;
    private long backPressTime;

    private RequestQueue queue;
    private MaterialAlertDialogBuilder dialogBuilder;
    private LoadingDialog loadingDialog;
    private IconDialog iconDialog;

    private String imei_num = "";

    private String latitude;
    private String longitude;

    private String selectedCustomerId;
    private ArrayList<String> cusList;
    private ArrayList<String> cusCodeList;

    //-------------UI elements--------------------
    private TextView promptTextView;
    private Spinner customerSpinner;
    private MaterialButton checkCustomerButton, panelSerialScanButton, batterySerialScanButton,
            sendPresenceButton, logOutButton;
    private MaterialToolbar toolbar;

    private RelativeLayout panelSerialLayout, batterySerialLayout, spinnerLayout;
    private TextInputEditText panelSerialInput, batterySerialInput;

    private ImageButton spinnerButton;
    private View.OnClickListener spinnerOnclickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateSessionLastActiveTime();
            customerSpinner.performClick();
        }
    };
    private AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            panelSerialLayout.setVisibility(View.VISIBLE);
            batterySerialLayout.setVisibility(View.VISIBLE);
            sendPresenceButton.setVisibility(View.VISIBLE);
            promptTextView.setVisibility(View.GONE);

            if (cusList != null)
                selectedCustomerId = cusCodeList.get(position);

            Log.d(TAG + " spinner", String.valueOf(position));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private View.OnClickListener panelSerialScanListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateSessionLastActiveTime();
            Intent intent = new Intent(ActivitySendPresence.this, ActivityScanner.class);
            startActivityForResult(intent, REQ_CODE_PANEL);
        }
    };

    private View.OnClickListener batterySerialScanListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateSessionLastActiveTime();
            Intent intent = new Intent(ActivitySendPresence.this, ActivityScanner.class);
            startActivityForResult(intent, REQ_CODE_BATTERY);
        }
    };
    private View.OnClickListener sendPresenceOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateSessionLastActiveTime();
            //check internet
            new InternetCheck(new InternetCheck.Consumer() {
                @Override
                public void accept(Boolean internet) {
                    if (internet) {
                        if (imei_num != null && latitude != null && longitude != null) {

                            boolean isOk = true;
                            String panelSerial = "";
                            String batterySerial = "";

                            if (panelSerialInput != null)
                                panelSerial = Objects.requireNonNull(panelSerialInput.getText()).toString();
                            if (batterySerialInput != null)
                                batterySerial = Objects.requireNonNull(batterySerialInput.getText()).toString();

                            if (selectedCustomerId == null)
                                isOk = false;

                            if (isOk) {
                                sendPresence(panelSerial, batterySerial);
                                sendPresenceButton.setClickable(false);
                            } else
                                Toast.makeText(
                                        ActivitySendPresence.this,
                                        "Can not send presence. Please try again",
                                        Toast.LENGTH_LONG
                                ).show();
                        } else {
                            showDialog("Error!", "Ops! Can not read the IMEI " +
                                    "number or Location. Please try again.");
                            getImeiNum();
                            getLocation();
                        }
                    } else {
                        showDialog("No internet", "Ops! Internet is not available. Please connect to the " +
                                "internet and try again!");
                    }
                }
            });
        }
    };
    private View.OnClickListener customerCheckOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateSessionLastActiveTime();
            //check internet
            new InternetCheck(new InternetCheck.Consumer() {
                @Override
                public void accept(Boolean internet) {
                    if (internet) {
                        if (imei_num == null || imei_num.equals("")) {
                            iconDialog.startIconDialog("Ops! Can not read the IMEI number. " +
                                    "Please try again.", R.drawable.ic_mobile_cross);
                            getImeiNum();
                        } else if (latitude == null || latitude.equals("")
                                || longitude == null || longitude.equals("")) {
                            iconDialog.startIconDialog("Ops! Can not read Location " +
                                    "information. Pleas try again!", R.drawable.ic_location);
                            getLocation();
                        } else if (imei_num != null && latitude != null && longitude != null) {
                            getCustomers();
                            checkCustomerButton.setClickable(false);
                        }
                    } else {
                        showDialog("No internet", "Ops! Internet is not available. Please connect to the " +
                                "internet and try again!");
                    }
                }
            });
        }
    };

    private View.OnClickListener logoutButtonOnclickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ActivitySendPresence.this);
            String message = "Are you sure to logout?";
            updateSessionLastActiveTime();

            builder.setCancelable(true)
                    .setMessage(message)
                    .setPositiveButton(
                            "Yes",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sharedPreferences = getSharedPreferences(SHARED_PREF_FILE_NAME,
                                            Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();

                                    editor.putBoolean(SHARED_PREF_KEY_IS_LOGGED_IN, false);
                                    editor.apply();
                                    finish();
                                }
                            }
                    )
                    .setNegativeButton(
                            "No",
                            null
                    )
                    .setNeutralButton(
                            "Cancel",
                            null
                    );

            builder.show();

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence);

        queue = Volley.newRequestQueue(ActivitySendPresence.this);
        dialogBuilder = new MaterialAlertDialogBuilder(ActivitySendPresence.this);
        loadingDialog = new LoadingDialog(ActivitySendPresence.this);
        iconDialog = new IconDialog(ActivitySendPresence.this);

        findElements();

        setSupportActionBar(toolbar);

        panelSerialScanButton.setOnClickListener(panelSerialScanListener);
        batterySerialScanButton.setOnClickListener(batterySerialScanListener);

        checkCustomerButton.setOnClickListener(customerCheckOnClickListener);
        customerSpinner.setOnItemSelectedListener(itemSelectedListener);
        spinnerButton.setOnClickListener(spinnerOnclickListener);
        sendPresenceButton.setOnClickListener(sendPresenceOnClickListener);
        logOutButton.setOnClickListener(logoutButtonOnclickListener);
    }

    @Override
    public void onBackPressed() {
        if (backPressTime + BACK_PRESS_TIME_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed();
            sharedPreferences = getSharedPreferences(SHARED_PREF_FILE_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean(SHARED_PREF_KEY_IS_LOGGED_IN, false);
            editor.apply();

            finish();
            return;
        } else {
            Toast.makeText(
                    ActivitySendPresence.this,
                    "Please press back again to logout and exit",
                    Toast.LENGTH_SHORT
            ).show();
        }

        backPressTime = System.currentTimeMillis();
    }

    void findElements() {
        promptTextView = findViewById(R.id.presence_prompt_text);

        customerSpinner = findViewById(R.id.presence_spinner);

        checkCustomerButton = findViewById(R.id.presence_check_customer_button);
        panelSerialScanButton = findViewById(R.id.presence_panel_serial_scan_button);
        batterySerialScanButton = findViewById(R.id.presence_battery_serial_scan_button);
        sendPresenceButton = findViewById(R.id.presence_send_button);

        panelSerialLayout = findViewById(R.id.presence_panel_serial_holder);
        batterySerialLayout = findViewById(R.id.presence_battery_serial_holder);
        spinnerLayout = findViewById(R.id.presence_spinner_holder);

        panelSerialInput = findViewById(R.id.presence_panel_serial_input);
        batterySerialInput = findViewById(R.id.presence_battery_serial_input);

        spinnerButton = findViewById(R.id.presence_spinner_open_button);
        logOutButton = findViewById(R.id.presence_logout_button);
        toolbar = findViewById(R.id.presence_toolbar);
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
//        promptTextView.setVisibility(View.GONE);
        if (list.size() > 0) {
            spinnerLayout.setVisibility(View.VISIBLE);

            String promptMessage = "Select a customer to continue or check again.";
            promptTextView.setText(promptMessage);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            customerSpinner.setAdapter(adapter);
        } else {
            iconDialog.startIconDialog("No Data Found", R.drawable.ic_cross);
        }
    }

    @SuppressLint("HardwareIds")
    private void getImeiNum() {
//        loadingDialog.startLoadingDialog("Reading IMEI number...");
        // first check for permission
        if (ContextCompat.checkSelfPermission(ActivitySendPresence.this,
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // permission is already granted. Get the number
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            assert telephonyManager != null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imei_num = Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ANDROID_ID
                );
            } else if (Build.VERSION.SDK_INT >= 26) {
                imei_num = telephonyManager.getImei(0);
            } else {
                imei_num = telephonyManager.getDeviceId(0);
            }

            Log.i(TAG + " IMEI", imei_num);
//            loadingDialog.dismissLoadingDialog();
        } else {
//            loadingDialog.dismissLoadingDialog();
            //permission is not granted. Show error
            showDialog("Permission not granted", "Permission to read IMEI number is not " +
                    "granted. Please log in again with granting the permissions.");
        }
    }

    private void getLocation() {
//        final LoadingDialog loadingDialogLoc = new LoadingDialog(ActivitySendPresence.this);
//        loadingDialogLoc.startLoadingDialog("Reading current location...");
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

                                Toast.makeText(
                                        ActivitySendPresence.this,
                                        "Latitude: " + latitude +
                                                " Longitude: " + longitude,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
//                            loadingDialogLoc.dismissLoadingDialog();
                        }
                    }, Looper.getMainLooper());
        } else {
//            loadingDialogLoc.dismissLoadingDialog();
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
                                cusList = new ArrayList<>();
                                cusCodeList = new ArrayList<>();

                                String message = "";

                                for (int i = 0; i < lenOfArray; i++) {
                                    JSONObject object = jsonArray.getJSONObject(i);
                                    String temp = object.getString("CustomerName");
                                    String tempCode = object.getString("CustomerCode");
                                    String code = object.getString("MessageCode");
                                    message = object.getString("MessageText");

                                    if (code.equals("200")) {
                                        cusList.add(temp);
                                        cusCodeList.add(tempCode);
                                    }
                                    Log.d(TAG + " cus Str", temp);
                                }

                                if (cusList.size() > 0) {
                                    populateSpinner(cusList);
                                } else {
                                    if (message.equals("") || message == null)
                                        message = "No output result found.";
                                    iconDialog.startIconDialog(message, R.drawable.ic_cross);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                        loadingDialog.dismissLoadingDialog();
                        checkCustomerButton.setClickable(true);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                Log.e(TAG + " Access code", Objects.requireNonNull(error.getMessage()));
                error.printStackTrace();
                loadingDialog.dismissLoadingDialog();
                checkCustomerButton.setClickable(true);
                showDialog("Result", "No location or customer found this time. " +
                        "Please try again after a while.");

            }
        });

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                1,
                0
        ));

        Log.d(TAG + " timeout", String.valueOf(request.getTimeoutMs()));
        queue.add(request);
    }

    void sendPresence(final String panelSerial, String batterySerial) {
        loadingDialog.startLoadingDialog("Sending presence information...");

        String url = "https://202.164.211.110/RASolarERPWebServices/RASolarERP_SecurityAdmin.svc/" +
                "SendPresenceAtCustomerHome?DeviceID=" + imei_num + "&CustomerCode=" + selectedCustomerId +
                "&PanelSerialNo=" + panelSerial + "&BatterySerialNo=" + batterySerial + "&LocationLatitude=" +
                latitude + "&LocationLongitude=" + longitude + "&ServiceAccessUserName=" +
                SERVICE_ACCESS_USER_NAME + "&ServiceAccessCode=" + SERVICE_ACCESS_CODE;

        Log.d(TAG + " pres url", url);

        final JsonObjectRequest request = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null) {
                            Log.d(TAG + " PRES", response.toString());
                            try {
                                JSONArray jsonArray = response.getJSONArray("ReturnMessage");
                                JSONObject object = jsonArray.getJSONObject(0);

                                Log.d(TAG + " PRE VW", object.toString());

                                if (object.getString("MessageCode").equals("200")) {
                                    // presence successful

                                    iconDialog.startIconDialog("In/Out Presence Successful. Thanks",
                                            R.drawable.ic_check);

                                    panelSerialInput.setText("");
                                    batterySerialInput.setText("");

                                    spinnerLayout.setVisibility(View.GONE);
                                    panelSerialLayout.setVisibility(View.GONE);
                                    batterySerialLayout.setVisibility(View.GONE);
                                    sendPresenceButton.setVisibility(View.GONE);

                                    String s = "Please check customers to continue";
                                    promptTextView.setText(s);
                                    promptTextView.setVisibility(View.VISIBLE);
                                    Log.d(TAG + " PRE SC", object.getString("MessageText"));
                                } else {
                                    // presence not successful
                                    iconDialog.startIconDialog(object.getString("MessageText"),
                                            R.drawable.ic_cross);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                        loadingDialog.dismissLoadingDialog();
                        sendPresenceButton.setClickable(true);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                Log.e(TAG + " Access code", Objects.requireNonNull(error.getMessage()));
                error.printStackTrace();
                loadingDialog.dismissLoadingDialog();
                sendPresenceButton.setClickable(true);
                showDialog("Request time out", "The server took too long to respond. Please check" +
                        " your internet connection and try again.");
            }
        });

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_PANEL: {
                if (resultCode == Activity.RESULT_OK) {
                    assert data != null;
                    String panelSerialCode = data.getStringExtra("SCAN_RES");
                    panelSerialInput.setText(panelSerialCode);
                } else {
                    Toast.makeText(
                            ActivitySendPresence.this,
                            "Can not read Panel serial. Please enter manually!",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
            break;

            case REQ_CODE_BATTERY: {
                if (resultCode == Activity.RESULT_OK) {
                    assert data != null;
                    String batterySerialCode = data.getStringExtra("SCAN_RES");
                    batterySerialInput.setText(batterySerialCode);
                } else {
                    Toast.makeText(
                            ActivitySendPresence.this,
                            "Can not read Panel serial. Please enter manually!",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
            break;

            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        getImeiNum();
        getLocation();
        updateSessionLastActiveTime();

        Log.d(TAG, "onResume call");

        sharedPreferences = getSharedPreferences(SHARED_PREF_FILE_NAME, MODE_PRIVATE);
        long lastLogTime = sharedPreferences.getLong(SHARED_PREF_KEY_LOGIN_TIME, 0);

        if (!(System.currentTimeMillis() - lastLogTime < AUTO_LOGIN_DURATION &&
                sharedPreferences.getBoolean(SHARED_PREF_KEY_IS_LOGGED_IN, false))) {
            // session expired
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SHARED_PREF_KEY_IS_LOGGED_IN, false);
            editor.apply();

            AlertDialog.Builder builder = new AlertDialog.Builder(ActivitySendPresence.this);

            builder.setTitle("Session Expired")
                    .setMessage("The current session has expired. Please login again!")
                    .setCancelable(false)
                    .setIcon(R.drawable.ic_cross)
                    .setPositiveButton(
                            "Okay",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(ActivitySendPresence.this, ActivityLogin.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                    )
                    .setNegativeButton(
                            "Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            }
                    );

            builder.create().show();
        }

    }

    private void updateSessionLastActiveTime() {
        sharedPreferences = getSharedPreferences(SHARED_PREF_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putLong(SHARED_PREF_KEY_LOGIN_TIME, System.currentTimeMillis());
        editor.apply();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG + "TCH", "touched");
        updateSessionLastActiveTime();

        return super.onTouchEvent(event);
    }
}
