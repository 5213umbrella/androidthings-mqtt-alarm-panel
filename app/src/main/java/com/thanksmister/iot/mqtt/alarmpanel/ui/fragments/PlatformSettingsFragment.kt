/*
 * Copyright (c) 2018. ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.iot.mqtt.alarmpanel.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.PreferenceFragmentCompat
import android.text.TextUtils
import android.view.View
import com.thanksmister.iot.mqtt.alarmpanel.R
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration.Companion.PREF_MODULE_WEB
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration.Companion.PREF_PLATFORM_BACK_BEHAVIOR
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration.Companion.PREF_PLATFORM_ADMIN_MENU
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration.Companion.PREF_PLATFORM_BAR
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration.Companion.PREF_WEB_URL
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class PlatformSettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject lateinit var configuration: Configuration

    private var webModulePreference: CheckBoxPreference? = null
    private var platformbarPreference: CheckBoxPreference? = null
    private var adminMenuPreference: CheckBoxPreference? = null
    private var backBehaviorPreference: CheckBoxPreference? = null
    private var webUrlPreference: EditTextPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_platform)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        webModulePreference = findPreference(PREF_MODULE_WEB) as CheckBoxPreference
        platformbarPreference = findPreference(PREF_PLATFORM_BAR) as CheckBoxPreference
        adminMenuPreference = findPreference(PREF_PLATFORM_ADMIN_MENU) as CheckBoxPreference
        backBehaviorPreference = findPreference(PREF_PLATFORM_BACK_BEHAVIOR) as CheckBoxPreference
        webUrlPreference = findPreference(PREF_WEB_URL) as EditTextPreference

        if (!TextUtils.isEmpty(configuration.webUrl)) {
            webUrlPreference!!.text = configuration.webUrl
            webUrlPreference!!.summary = configuration.webUrl
        }

        webModulePreference!!.isChecked = configuration.hasPlatformModule()
        platformbarPreference!!.isChecked = configuration.platformBar
        adminMenuPreference!!.isChecked = configuration.hideAdminMenu
        backBehaviorPreference!!.isChecked = configuration.adjustBackBehavior
        platformbarPreference!!.isEnabled = configuration.hasPlatformModule()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            PREF_MODULE_WEB -> {
                val checked = webModulePreference!!.isChecked
                configuration.setWebModule(checked)
                platformbarPreference!!.isEnabled = checked
            }
            PREF_PLATFORM_BAR -> {
                val checked = platformbarPreference!!.isChecked
                configuration.platformBar = checked
            }
            PREF_PLATFORM_ADMIN_MENU -> {
                val checked = adminMenuPreference!!.isChecked
                configuration.hideAdminMenu = checked
            }
            PREF_PLATFORM_BACK_BEHAVIOR -> {
                val checked = backBehaviorPreference!!.isChecked
                configuration.adjustBackBehavior = checked
            }
            PREF_WEB_URL -> {
                val value = webUrlPreference!!.text
                configuration.webUrl = value
                webUrlPreference!!.text = value
                webUrlPreference!!.summary = value
            }
        }
    }
}