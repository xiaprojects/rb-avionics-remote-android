package com.xiaprojects.rb

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun showTimeoutConfirmModal(pref: Preference, newValue: Any) {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_timeout_warning_title)
            .setMessage(R.string.settings_timeout_warning_text)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(
                android.R.string.ok
            ) { dialog, whichButton ->

            }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            findPreference<SeekBarPreference>(SplashScreenActivity.SettingsConst.SETTINGS_BUTTON_TIMEOUT)
                ?.setOnPreferenceChangeListener { pref, newValue ->
                    if (newValue as Int > 1) {
                        return@setOnPreferenceChangeListener true
                    }
                    AlertDialog.Builder(pref.context)
                        .setTitle(R.string.settings_timeout_warning_title)
                        .setMessage(R.string.settings_timeout_warning_text)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(
                            android.R.string.ok
                        ) { dialog, whichButton ->
                            findPreference<SeekBarPreference>(SplashScreenActivity.SettingsConst.SETTINGS_BUTTON_TIMEOUT)?.value =
                                newValue
                        }
                        .setNegativeButton(android.R.string.cancel, null).show()
                    false
                }

            // Set app version
            try {
                val context = requireContext()
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                findPreference<Preference>("version")?.summary = packageInfo.versionName
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}