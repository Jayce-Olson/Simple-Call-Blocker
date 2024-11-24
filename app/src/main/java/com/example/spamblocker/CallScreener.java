package com.example.spamblocker;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telecom.Connection;

import androidx.annotation.NonNull;
import android.content.SharedPreferences;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.Locale;

public class CallScreener extends CallScreeningService{

    @Override
    public void onScreenCall(@NonNull Call.Details callDetails) {
        // From what I read in the documentation, this is only call on unknown numbers (not in contacts),
        // however contact numbers were getting through anyways, so I implemented contact checking anyways.
        try {
            boolean isIncoming = callDetails.getCallDirection() == Call.Details.DIRECTION_INCOMING;
            SharedPreferences preferences = getSharedPreferences("CallScreenerPrefs", MODE_PRIVATE);
            boolean isAppEnabled = preferences.getBoolean("isAppEnabled", false);
            CallResponse.Builder response = new CallResponse.Builder();
            Uri handle = callDetails.getHandle();
            String phoneNumber = handle.getSchemeSpecificPart();
            String normalizedNumber = PhoneNumberUtils.formatNumber(phoneNumber, Locale.getDefault().getCountry());

            if (isIncoming && isAppEnabled && !isContact(normalizedNumber)) {

                switch (callDetails.getCallerNumberVerificationStatus()) {
                    case Connection.VERIFICATION_STATUS_PASSED:
                        // Network verification passed, likely a valid call.
                        response.setRejectCall(true);
                        response.setSkipCallLog(false);
                        response.setSkipNotification(false);
                        break;
                    default:
                        // Network could not perform verification.
                        // This branch matches Connection.VERIFICATION_STATUS_NOT_VERIFIED
                        response.setDisallowCall(true);
                        response.setSkipNotification(false);
                        response.setSkipCallLog(false);
                }
                respondToCall(callDetails, response.build());
            }else{
                respondToCall(callDetails, response.setDisallowCall(false).build());
            }
        } catch (Exception e) {}
    }

    private boolean isContact(String phoneNumber) {
        Cursor cursor = null;
        boolean isContact = false;

        try {
            cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?",
                    new String[]{phoneNumber},
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                isContact = cursor.getCount() > 0;
                Log.d("ContactCheck", "Contact found: " + isContact);
            } else {
                Log.d("ContactCheck", "No contact found.");
            }
        } catch (Exception e) {
            Log.e("ContactCheck", "Error querying contacts", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return isContact;
    }
}
