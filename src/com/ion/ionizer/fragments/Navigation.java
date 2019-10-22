/*
 * Copyright (C) 2019 ion-OS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ion.ionizer.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto; 
import com.android.internal.util.hwkeys.ActionUtils;
import com.android.settings.SettingsPreferenceFragment;

import com.ion.ionizer.R;

public class Navigation extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    public static final String TAG = "Navigation";

    private static final String PREF_HW_BUTTONS = "hw_buttons";
    private static final String ENABLE_NAV_BAR = "enable_nav_bar";

    private SwitchPreference mEnableNavigationBar;
    private boolean mIsNavSwitchingMode = false;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.ion_settings_navigation);
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final boolean needsNavbar = ActionUtils.hasNavbarByDefault(getActivity());
        // bits for hardware keys present on device
        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        if (needsNavbar && deviceKeys == 0) {
            getPreferenceScreen().removePreference(findPreference(PREF_HW_BUTTONS));
        }

        // Navigation bar related options
        mEnableNavigationBar = (SwitchPreference) findPreference(ENABLE_NAV_BAR);

        // Only visible on devices that have a navigation bar already
        if (ActionUtils.hasNavbarByDefault(getActivity())) {
            mEnableNavigationBar.setOnPreferenceChangeListener(this);
            mHandler = new Handler();
            updateNavBarOption();
        } else {
            prefScreen.removePreference(mEnableNavigationBar);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mEnableNavigationBar) {
            if (mIsNavSwitchingMode) {
                return false;
            }
            mIsNavSwitchingMode = true;
            boolean isNavBarChecked = ((Boolean) newValue);
            mEnableNavigationBar.setEnabled(false);
            writeNavBarOption(isNavBarChecked);
            updateNavBarOption();
            mEnableNavigationBar.setEnabled(true);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mIsNavSwitchingMode = false;
                }
            }, 1000);
            return true;
        }
        return false;
    }

    private void writeNavBarOption(boolean enabled) {
        Settings.System.putIntForUser(getActivity().getContentResolver(),
                Settings.System.FORCE_SHOW_NAVBAR, enabled ? 1 : 0, UserHandle.USER_CURRENT);
    }

    private void updateNavBarOption() {
        boolean enabled = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.FORCE_SHOW_NAVBAR, 1, UserHandle.USER_CURRENT) != 0;
        mEnableNavigationBar.setChecked(enabled);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ION_IONIZER;
    }
}
