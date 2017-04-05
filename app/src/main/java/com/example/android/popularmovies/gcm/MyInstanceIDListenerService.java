package com.example.android.popularmovies.gcm;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by Julian Heetel on 03.04.2017.
 *
 */

public class MyInstanceIDListenerService extends InstanceIDListenerService {
    private static final String TAG = MyInstanceIDListenerService.class.getSimpleName();

//    @Override
//    public void onTokenRefresh() {
//        Intent intent = new Intent(this, RegistrationIntentService.class);
//        startService(intent);
//    }
}
