package com.labtech.mod;

import java.util.Set;

import de.robv.android.xposed.library.ui.ValueSeekBarPreference;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;

public class NotiModActivity extends PreferenceActivity {
	private CheckBoxPreference nm_NL;
	private ListPreference nm_options;
	private CheckBoxPreference nm_disablesettings;
	private CheckBoxPreference nm_bapm;
	private CheckBoxPreference nm_aosp;
	private ValueSeekBarPreference nm_aosppercent;
	private ListPreference nm_aosploc;
	private CheckBoxPreference nm_hideicon;
	private SharedPreferences mPrefs;

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return super.getSharedPreferences(Misc.PREFERENCE,
				Context.MODE_WORLD_READABLE);
	}

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.pref);
		nm_NL = (CheckBoxPreference) findPreference(Misc.PREF_NL);
		nm_bapm = (CheckBoxPreference) findPreference(Misc.PREF_BAPM);
		nm_aosp = (CheckBoxPreference) findPreference(Misc.PREF_AOSP);
		nm_options = (ListPreference) findPreference(Misc.PREF_OP);
		nm_disablesettings = (CheckBoxPreference) findPreference(Misc.PREF_DISABLE_SETTINGS);
		nm_aosppercent = (ValueSeekBarPreference) findPreference(Misc.PREF_AOSPPERCENT);
		nm_aosploc = (ListPreference) findPreference(Misc.PREF_AOSPLOC);
		nm_hideicon = (CheckBoxPreference) findPreference(Misc.PREF_HIDEICON);

		mPrefs = getSharedPreferences(Misc.PREFERENCE,
				Context.MODE_WORLD_READABLE);
		OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				updatePreference(preference, newValue);
				return true;
			}
		};
		nm_NL.setChecked(mPrefs.getBoolean(Misc.PREF_NL, true));
		nm_NL.setOnPreferenceChangeListener(listener);
		nm_bapm.setChecked(mPrefs.getBoolean(Misc.PREF_BAPM, true));
		nm_bapm.setOnPreferenceChangeListener(listener);
		nm_aosp.setChecked(mPrefs.getBoolean(Misc.PREF_AOSP, true));
		nm_aosp.setOnPreferenceChangeListener(listener);
		nm_disablesettings.setChecked(mPrefs.getBoolean(
				Misc.PREF_DISABLE_SETTINGS, true));
		nm_disablesettings.setOnPreferenceChangeListener(listener);
		nm_options.setDefaultValue(mPrefs.getString(Misc.PREF_OP, "both"));
		nm_options.setSummary("   ");
		nm_options.setSummary("%s");
		nm_options
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						updatePreference(preference, newValue);
						nm_options.setSummary("   ");
						nm_options.setSummary("%s");
						return true;
					}
				});
		nm_aosploc
				.setDefaultValue(mPrefs.getString(Misc.PREF_AOSPLOC, "right"));
		nm_aosploc.setSummary("   ");
		nm_aosploc.setSummary("%s");
		nm_aosploc
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						updatePreference(preference, newValue);
						nm_aosploc.setSummary("   ");
						nm_aosploc.setSummary("%s");
						return true;
					}
				});
		nm_aosppercent.setProgress(mPrefs.getInt(Misc.PREF_AOSPPERCENT, 25));
		nm_aosppercent.setOnPreferenceChangeListener(listener);
		nm_hideicon.setChecked(mPrefs.getBoolean(Misc.PREF_HIDEICON, false));
		nm_hideicon
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference arg0,
							Object arg1) {
						Log.d("nm_hideicon", arg1+"");
						PackageManager pm = getPackageManager();
						pm.setComponentEnabledSetting(
								new ComponentName(getApplicationContext(),
										Misc.MOD_PACKAGE_NAME + ".NotiLauncher"),
								(Boolean) arg1 ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
										: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
								PackageManager.DONT_KILL_APP);
						return true;
					}
				});

	}

	protected void commited() {
		Intent i = new Intent(Misc.SETTINGS_UPDATED_INTENT);
		sendBroadcast(i);
	}

	@SuppressWarnings("unchecked")
	private void updatePreference(Preference preference, Object newValue) {
		Editor editor = mPrefs.edit();
		if (newValue instanceof String) {
			editor.putString(preference.getKey(), (String) newValue);
		} else if (newValue instanceof Integer) {
			editor.putInt(preference.getKey(), (Integer) newValue);
		} else if (newValue instanceof Set<?>) {
			editor.putStringSet(preference.getKey(), (Set<String>) newValue);
		} else if (newValue instanceof Boolean) {
			editor.putBoolean(preference.getKey(), (Boolean) newValue);
		}
		editor.commit();
		commited();
	}
}
