package com.sourceservermanager;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by Matthew on 2/22/2016.
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

}
