package com.sentaroh.android.SMBExplorer;

/*
The MIT License (MIT)
Copyright (c) 2011 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ActivitySetting extends PreferenceActivity {
    private static Logger log= LoggerFactory.getLogger(ActivitySetting.class);
    private static Context mContext=null;
    private static PreferenceFragment mPrefFrag=null;

    private static ActivitySetting mPrefActivity=null;

    private static GlobalParameter mGp=null;

//    private CommonUtilities mUtil=null;

//	private GlobalParameters mGp=null;

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mContext=this;
        mGp=GlobalWorkArea.getGlobalParameters(mContext);
//        setTheme(mGp.applicationTheme);
        super.onCreate(savedInstanceState);
        mPrefActivity=this;
        log.debug(CommonUtilities.getExecutedMethodName()+" entered");
//        if (mGp.settingFixDeviceOrientationToPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onStart(){
        super.onStart();
        log.debug(CommonUtilities.getExecutedMethodName()+" entered");
    };

    @Override
    public void onResume(){
        super.onResume();
        log.debug(CommonUtilities.getExecutedMethodName()+" entered");
//		setTitle(R.string.settings_main_title);
    };

    @Override
    public void onBuildHeaders(List<Header> target) {
        log.debug(CommonUtilities.getExecutedMethodName()+" entered");
        loadHeadersFromResource(R.xml.settings_frag, target);
    };

    @Override
    public boolean onIsMultiPane () {
        mContext=this;
        mGp=GlobalWorkArea.getGlobalParameters(mContext);
//    	mPrefActivity=this;
        log.debug(CommonUtilities.getExecutedMethodName()+" entered");
        return true;
    };

    @Override
    protected void onPause() {
        super.onPause();
        log.debug(CommonUtilities.getExecutedMethodName()+" entered");
    };

    @Override
    final public void onStop() {
        super.onStop();
        log.debug(CommonUtilities.getExecutedMethodName()+" entered");
    };

    @Override
    final public void onDestroy() {
        super.onDestroy();
        log.debug(CommonUtilities.getExecutedMethodName()+" entered");
    };

    public static class SettingsMisc extends PreferenceFragment {
        private static Logger log= LoggerFactory.getLogger(SettingsMisc.class);
        private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
                        checkSettings(shared_pref, key_string);
                    }
                };

//        private CommonUtilities mUtil=null;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mPrefFrag=this;
            log.debug(CommonUtilities.getExecutedMethodName()+" entered");

            addPreferencesFromResource(R.xml.settings_frag_misc);

            SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

            shared_pref.edit().putBoolean(getString(R.string.settings_exit_clean),true).commit();
            findPreference(getString(R.string.settings_exit_clean).toString()).setEnabled(false);
            checkSettings(shared_pref, getString(R.string.settings_exit_clean));
        };

        private boolean checkSettings(SharedPreferences shared_pref, String key_string) {
            boolean isChecked = false;
            Preference pref_key=mPrefFrag.findPreference(key_string);
            if (key_string.equals(getString(R.string.settings_exit_clean))) {
                isChecked=true;
                if (shared_pref.getBoolean(key_string, true)) {
                    pref_key.setSummary(getString(R.string.settings_exit_clean_summary_ena));
                } else {
                    pref_key.setSummary(getString(R.string.settings_exit_clean_summary_dis));
                }
            }

            return isChecked;
        };

        @Override
        public void onStart() {
            super.onStart();
            log.debug(CommonUtilities.getExecutedMethodName()+" entered");
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listenerAfterHc);
            getActivity().setTitle(R.string.settings_misc_title);
        };
        @Override
        public void onStop() {
            super.onStop();
            log.debug(CommonUtilities.getExecutedMethodName()+" entered");
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
        };
    };

}
