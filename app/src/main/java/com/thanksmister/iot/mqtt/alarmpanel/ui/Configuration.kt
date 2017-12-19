/*
 * <!--
 *   ~ Copyright (c) 2017. ThanksMister LLC
 *   ~
 *   ~ Licensed under the Apache License, Version 2.0 (the "License");
 *   ~ you may not use this file except in compliance with the License. 
 *   ~ You may obtain a copy of the License at
 *   ~
 *   ~ http://www.apache.org/licenses/LICENSE-2.0
 *   ~
 *   ~ Unless required by applicable law or agreed to in writing, software distributed 
 *   ~ under the License is distributed on an "AS IS" BASIS, 
 *   ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *   ~ See the License for the specific language governing permissions and 
 *   ~ limitations under the License.
 *   -->
 */

package com.thanksmister.iot.mqtt.alarmpanel.ui

import android.text.TextUtils
import com.thanksmister.iot.mqtt.alarmpanel.utils.AlarmUtils

import dpreference.DPreference
import javax.inject.Inject

/**
 * Store configurations
 */
class Configuration @Inject
constructor(private val sharedPreferences: DPreference) {

    var webUrl: String?
        get() = this.sharedPreferences.getPrefString(PREF_WEB_URL, null)
        set(value) = this.sharedPreferences.setPrefString(PREF_WEB_URL, value)

    var inactivityTime: Long
        get() = this.sharedPreferences.getPrefLong(PREF_INACTIVITY_TIME, 300000)
        set(value) = this.sharedPreferences.setPrefLong(PREF_INACTIVITY_TIME, value)

    var timeZone: String
        get() = this.sharedPreferences.getPrefString(PREF_DEVICE_TIME_ZONE, "America/Los_Angeles")
        set(value) = this.sharedPreferences.setPrefString(PREF_DEVICE_TIME_ZONE, value)

    var screenDensity: Int
        get() = this.sharedPreferences.getPrefInt(PREF_DEVICE_SCREEN_DENSITY, 160)
        set(value) = this.sharedPreferences.setPrefInt(PREF_DEVICE_SCREEN_DENSITY, value)

    var screenBrightness: Int
        get() = this.sharedPreferences.getPrefInt(PREF_DEVICE_SCREEN_BRIGHTNESS, 200)
        set(value) = this.sharedPreferences.setPrefInt(PREF_DEVICE_SCREEN_BRIGHTNESS, value)

    var screenTimeout: Long
        get() = this.sharedPreferences.getPrefLong(PREF_DEVICE_SCREEN_TIMEOUT, 1800000)
        set(value) = this.sharedPreferences.setPrefLong(PREF_DEVICE_SCREEN_TIMEOUT, value)

    var isFirstTime: Boolean
        get() = sharedPreferences.getPrefBoolean(PREF_FIRST_TIME, true)
        set(value) = sharedPreferences.setPrefBoolean(PREF_FIRST_TIME, value)

    var pendingTime: Int
        get() = this.sharedPreferences.getPrefInt(PREF_PENDING_TIME, AlarmUtils.PENDING_TIME)
        set(value) = this.sharedPreferences.setPrefInt(PREF_PENDING_TIME, value)

    var disableTime: Int
        get() = this.sharedPreferences.getPrefInt(PREF_DISABLE_DIALOG_TIME, AlarmUtils.DISABLE_TIME)
        set(value) = this.sharedPreferences.setPrefInt(PREF_DISABLE_DIALOG_TIME, value)

    var alarmCode: Int
        get() = this.sharedPreferences.getPrefInt(PREF_ALARM_CODE, 1234)
        set(value) = this.sharedPreferences.setPrefInt(PREF_ALARM_CODE, value)

    var timeFormat: Int
        get() = sharedPreferences.getPrefInt(PREF_DEVICE_TIME_FORMAT, 12)
        set(value) = sharedPreferences.setPrefInt(PREF_DEVICE_TIME_FORMAT, value)

    var useTimeServer: Boolean
        get() = sharedPreferences.getPrefBoolean(PREF_DEVICE_TIME_SERVER, true)
        set(value) = sharedPreferences.setPrefBoolean(PREF_DEVICE_TIME_SERVER, value)

    fun hasPlatformModule(): Boolean {
        return sharedPreferences.getPrefBoolean(PREF_MODULE_WEB, false)
    }

    fun setWebModule(value: Boolean) {
        this.sharedPreferences.setPrefBoolean(PREF_MODULE_WEB, value)
    }

    fun hasTssModule(): Boolean {
        return sharedPreferences.getPrefBoolean(PREF_MODULE_TSS, false)
    }

    fun setTssModule(value: Boolean) {
        this.sharedPreferences.setPrefBoolean(PREF_MODULE_TSS, value)
    }

    fun hasAlertsModule(): Boolean {
        return sharedPreferences.getPrefBoolean(PREF_MODULE_ALERTS, false)
    }

    fun setAlertsModule(value: Boolean) {
        this.sharedPreferences.setPrefBoolean(PREF_MODULE_ALERTS, value)
    }

    fun showClockScreenSaverModule(): Boolean {
        return sharedPreferences.getPrefBoolean(PREF_MODULE_CLOCK_SAVER, false)
    }

    fun setClockScreenSaverModule(value: Boolean) {
        this.sharedPreferences.setPrefBoolean(PREF_MODULE_CLOCK_SAVER, value)
    }

    @Deprecated("Instagram no longer works")
    fun showPhotoScreenSaver(): Boolean {
        return false //sharedPreferences.getPrefBoolean(PREF_MODULE_PHOTO_SAVER, false)
    }

    @Deprecated("Instagram no longer works")
    fun setPhotoScreenSaver(value: Boolean) {
        this.sharedPreferences.setPrefBoolean(PREF_MODULE_PHOTO_SAVER, value)
    }

    fun setShowWeatherModule(show: Boolean) {
        sharedPreferences.setPrefBoolean(PREF_MODULE_WEATHER, show)
    }

    fun showWeatherModule(): Boolean {
        return sharedPreferences.getPrefBoolean(PREF_MODULE_WEATHER, false)
    }

    fun hasNotifications(): Boolean {
        return sharedPreferences.getPrefBoolean(PREF_MODULE_NOTIFICATION, false)
    }

    fun setHasNotifications(value: Boolean) {
        this.sharedPreferences.setPrefBoolean(PREF_MODULE_NOTIFICATION, value)
    }

    fun getMailTo(): String? {
        return sharedPreferences.getPrefString(PREF_MAIL_TO, null)
    }

    fun setMailTo(value: String) {
        sharedPreferences.setPrefString(PREF_MAIL_TO, value)
    }

    fun getMailFrom(): String? {
        return sharedPreferences.getPrefString(PREF_MAIL_FROM, null)
    }

    fun setMailFrom(value: String) {
        sharedPreferences.setPrefString(PREF_MAIL_FROM, value)
    }

    fun getMailGunApiKey(): String? {
        return sharedPreferences.getPrefString(PREF_MAIL_API_KEY, null)
    }

    fun setMailGunApiKey(value: String) {
        sharedPreferences.setPrefString(PREF_MAIL_API_KEY, value)
    }

    fun getMailGunUrl(): String? {
        return sharedPreferences.getPrefString(PREF_MAIL_URL, null)
    }

    fun setMailGunUrl(value: String) {
        sharedPreferences.setPrefString(PREF_MAIL_URL, value)
    }

    fun hasMailGunCredentials(): Boolean {
        return !TextUtils.isEmpty(getMailGunUrl()) && !TextUtils.isEmpty(getMailGunApiKey())
                && !TextUtils.isEmpty(getMailTo()) && !TextUtils.isEmpty(getMailFrom())
    }

    fun hasCamera(): Boolean {
        return sharedPreferences.getPrefBoolean(PREF_MODULE_CAMERA, false)
    }

    fun setHasCamera(value: Boolean) {
        this.sharedPreferences.setPrefBoolean(PREF_MODULE_CAMERA, value)
    }

    fun getCameraRotate(): Int? {
        return sharedPreferences.getPrefInt(PREF_CAMERA_ROTATE, 0)
    }

    fun setCameraRotate(value: Int) {
        sharedPreferences.setPrefInt(PREF_CAMERA_ROTATE, value)
    }

    /**
     * Reset the `SharedPreferences` and database
     */
    fun reset() {
        sharedPreferences.removePreference(PREF_PENDING_TIME)
        sharedPreferences.removePreference(PREF_MODULE_CLOCK_SAVER)
        sharedPreferences.removePreference(PREF_MODULE_PHOTO_SAVER)
        sharedPreferences.removePreference(PREF_INACTIVITY_TIME)
        sharedPreferences.removePreference(PREF_MODULE_WEATHER)
        sharedPreferences.removePreference(PREF_MODULE_WEB)
        sharedPreferences.removePreference(PREF_MODULE_NOTIFICATION)
        sharedPreferences.removePreference(PREF_WEB_URL)
        sharedPreferences.removePreference(PREF_FIRST_TIME)
        sharedPreferences.removePreference(PREF_MAIL_TO)
        sharedPreferences.removePreference(PREF_MAIL_FROM)
        sharedPreferences.removePreference(PREF_MODULE_CAMERA)
        sharedPreferences.removePreference(PREF_MAIL_API_KEY)
        sharedPreferences.removePreference(PREF_MAIL_URL)
        sharedPreferences.removePreference(PREF_CAMERA_ROTATE)
        sharedPreferences.removePreference(PREF_MODULE_TSS)
        sharedPreferences.removePreference(PREF_MODULE_ALERTS)
        sharedPreferences.removePreference(PREF_DEVICE_SCREEN_DENSITY)
        sharedPreferences.removePreference(PREF_DEVICE_TIME_ZONE)
        sharedPreferences.removePreference(PREF_DEVICE_TIME)
        sharedPreferences.removePreference(PREF_DEVICE_TIME_FORMAT)
        sharedPreferences.removePreference(PREF_DEVICE_TIME_SERVER)
        sharedPreferences.removePreference(PREF_DEVICE_SCREEN_BRIGHTNESS)
        sharedPreferences.removePreference(PREF_DEVICE_SCREEN_TIMEOUT)
    }

    companion object {
        @JvmField val PREF_PENDING_TIME = "pref_pending_time"
        @JvmField val PREF_ALARM_CODE = "pref_alarm_code"
        @JvmField val PREF_MODULE_CLOCK_SAVER = "pref_module_saver_clock"
        @JvmField val PREF_MODULE_PHOTO_SAVER = "pref_module_saver_photo"
        @JvmField val PREF_IMAGE_SOURCE = "pref_image_source"
        @JvmField val PREF_IMAGE_FIT_SIZE = "pref_image_fit"
        @JvmField val PREF_IMAGE_ROTATION = "pref_image_rotation"
        @JvmField val PREF_INACTIVITY_TIME = "pref_inactivity_time"
        @JvmField val PREF_MODULE_NOTIFICATION = "pref_module_notification"
        @JvmField val PREF_MODULE_TSS = "pref_module_tss"
        @JvmField val PREF_MODULE_ALERTS = "pref_module_alerts"
        @JvmField val PREF_MAIL_TO = "pref_mail_to"
        @JvmField val PREF_MAIL_FROM = "pref_mail_from"
        @JvmField val PREF_MAIL_API_KEY = "pref_mail_api_key"
        @JvmField val PREF_MAIL_URL = "pref_mail_url"
        @JvmField val PREF_DISABLE_DIALOG_TIME = "pref_disable_dialog_time" // this isn't configurable

        @JvmField val PREF_MODULE_CAMERA = "pref_module_camera"
        @JvmField val PREF_CAMERA_ROTATE = "pref_camera_rotate"
        @JvmField  val PREF_MODULE_WEATHER = "pref_module_weather"
        @JvmField val PREF_MODULE_WEB = "pref_module_web"
        @JvmField val PREF_WEB_URL = "pref_web_url"
        private val PREF_FIRST_TIME = "pref_first_time"
        @JvmField val PREF_DEVICE_TIME_SERVER = "pref_device_time_server"
        @JvmField val PREF_DEVICE_TIME_FORMAT = "pref_device_time_format"
        @JvmField val PREF_DEVICE_TIME = "pref_device_time"
        @JvmField val PREF_DEVICE_TIME_ZONE = "pref_device_time_zone"
        @JvmField val PREF_DEVICE_SCREEN_DENSITY = "pref_device_screen_density"
        @JvmField val PREF_DEVICE_SCREEN_BRIGHTNESS = "pref_device_brightness"
        @JvmField val PREF_DEVICE_SCREEN_TIMEOUT = "pref_device_timeout"
    }
}