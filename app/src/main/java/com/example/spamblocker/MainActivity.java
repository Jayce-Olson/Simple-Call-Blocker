package com.example.spamblocker;
// package com.myself.spamblocker;
import android.Manifest;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;

import android.app.AlertDialog;
import android.content.DialogInterface;

//import com.myself.spamblocker.R;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;
    private static final int REQUEST_ID = 1;
    private Switch callBlockingSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);

        callBlockingSwitch = findViewById(R.id.callBlockingSwitch);

        SharedPreferences preferences = getSharedPreferences("CallScreenerPrefs", MODE_PRIVATE);

        // Retrieve the "isAppEnabled" value, defaulting to false if not found
        boolean isAppEnabled = preferences.getBoolean("isAppEnabled", false);

        if (isAppEnabled) {
            callBlockingSwitch.setChecked(true);
        }

        // Set listener for the switch
        callBlockingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    // Check if the app is the default call screening service
                    if (!isCallScreeningApp(MainActivity.this)) {
                        showDefaultAppPrompt(MainActivity.this);
                    }

                    if (!hasRequiredPermissions()) {
                        disableCallBlocking();
                        requestRequiredPermissions();
                    } else {
                        // Enable call blocking
                        enableCallBlocking();
                    }

                } else {
                    // Disable call blocking
                    disableCallBlocking();
                }
            }
        });
    }

    // Method to check if all required permissions are granted
    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;// &&
                // ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;// &&
                // ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
    }

    // Method to request required permissions
    private void requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this, new String[] {
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE
        }, REQUEST_PERMISSIONS);
    }

    // Handling the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED /*&&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED*/) {
                // All permissions are granted
                Toast.makeText(this, "Permissions granted. Call blocking enabled.", Toast.LENGTH_SHORT).show();
                enableCallBlocking();
            } else {
                // Permissions are denied
                Toast.makeText(this, "Permissions denied. Cannot enable call blocking.", Toast.LENGTH_SHORT).show();
                callBlockingSwitch.setChecked(false);
            }
        }
    }

    private void enableCallBlocking() {
        TextView statusTextView = findViewById(R.id.statusTextView);
        // Logic to enable call blocking
        Toast.makeText(this, "Call blocking enabled.", Toast.LENGTH_SHORT).show();
        callBlockingSwitch.setChecked(true);
        statusTextView.setText("Call blocker enabled");
        SharedPreferences preferences = getSharedPreferences("CallScreenerPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isAppEnabled", true);
        editor.apply();
    }

    private void disableCallBlocking() {
        TextView statusTextView = findViewById(R.id.statusTextView);
        // Logic to disable call blocking
        Toast.makeText(this, "Call blocking disabled.", Toast.LENGTH_SHORT).show();
        callBlockingSwitch.setChecked(false);
        statusTextView.setText("Call blocker disabled");
        SharedPreferences preferences = getSharedPreferences("CallScreenerPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isAppEnabled", false);
        editor.apply();
    }

    public void promptForDefaultCallScreeningApp(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);

            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                // Intent to request becoming the default call screening app
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);

                // Check if there is an app that can handle this intent
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, REQUEST_ID); // use startActivityForResult to handle the result
                } else {
                    // Handle case where no app can handle this intent
                    Toast.makeText(this, "Unable to open call screening settings", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Call screening role is not available on this device.", Toast.LENGTH_LONG).show();
            }
        } else {
            // Fallback for devices with Android versions lower than Q (API 29)
            Toast.makeText(this, "Call screening is not supported on this version of Android.", Toast.LENGTH_LONG).show();
        }
    }

    public void showDefaultAppPrompt(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Set Default Call Screening App")
                .setMessage("To use call screening features, please set this app as your default call screening app.")
                .setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Open the settings screen
                        promptForDefaultCallScreeningApp(context);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ID) {
            if (resultCode == Activity.RESULT_OK) {
                enableCallBlocking();
            } else {
                disableCallBlocking();
            }
        }
    }
        public static boolean isCallScreeningApp (android.content.Context context){
            RoleManager roleManager = (RoleManager) context.getSystemService(Context.ROLE_SERVICE);
            return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        }
    }
