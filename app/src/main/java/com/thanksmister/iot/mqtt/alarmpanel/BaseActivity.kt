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

package com.thanksmister.iot.mqtt.alarmpanel

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.text.TextUtils
import android.view.Display
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.things.device.ScreenManager
import com.google.android.things.device.TimeManager
import com.google.android.things.update.StatusListener
import com.google.android.things.update.UpdateManager
import com.google.android.things.update.UpdateManager.POLICY_APPLY_AND_REBOOT
import com.google.android.things.update.UpdateManagerStatus
import com.google.android.things.update.UpdatePolicy
import com.thanksmister.iot.mqtt.alarmpanel.managers.ConnectionLiveData
import com.thanksmister.iot.mqtt.alarmpanel.network.DarkSkyOptions
import com.thanksmister.iot.mqtt.alarmpanel.network.ImageOptions
import com.thanksmister.iot.mqtt.alarmpanel.network.MQTTOptions
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration
import com.thanksmister.iot.mqtt.alarmpanel.ui.views.ScreenSaverView
import com.thanksmister.iot.mqtt.alarmpanel.utils.DialogUtils
import com.thanksmister.iot.mqtt.alarmpanel.utils.NetworkUtils
import com.thanksmister.iot.mqtt.alarmpanel.viewmodel.MessageViewModel
import dagger.android.support.DaggerAppCompatActivity
import dpreference.DPreference
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

abstract class BaseActivity : DaggerAppCompatActivity() {

    @Inject lateinit var configuration: Configuration
    @Inject lateinit var preferences: DPreference
    @Inject lateinit var dialogUtils: DialogUtils
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: MessageViewModel
    private var wakeLock: PowerManager.WakeLock? = null
    private var inactivityHandler: Handler? = Handler()
    private var hasNetwork = AtomicBoolean(true)
    val disposable = CompositeDisposable()
    private var connectionLiveData: ConnectionLiveData? = null
    private var wifiManager: WifiManager? = null


    abstract fun getLayoutId(): Int

    private val inactivityCallback = Runnable {
        dialogUtils.hideScreenSaverDialog()
        showScreenSaver()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MessageViewModel::class.java)

        Timber.d("Network SSID: " + configuration.networkId)
        Timber.d("Network Password: " + configuration.networkPassword)

        if( !TextUtils.isEmpty(configuration.networkId) && !TextUtils.isEmpty(configuration.networkPassword)) {
            NetworkUtils.connectNetwork(this@BaseActivity, configuration.networkId, configuration.networkPassword )
        }
    }

    override fun onStart(){
        super.onStart()
        // These are Android Things specific settings for setting the time, display, and update manager
        val handler = Handler()
        handler.postDelayed({ setSystemInformation() }, 1000)
        application.registerReceiver(wifiConnectionReceiver, intentFilterForWifiConnectionReceiver)
    }

    public override fun onStop() {
        super.onStop()
        application.unregisterReceiver(wifiConnectionReceiver)
    }

    public override fun onResume() {
        super.onResume()
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (inactivityHandler != null) {
            inactivityHandler!!.removeCallbacks(inactivityCallback)
            inactivityHandler = null
        }
        disposable.dispose()
    }

    // These are Android Things specific settings for setting the time, display, and update manager
    private fun setSystemInformation() {
        try {
            ScreenManager(Display.DEFAULT_DISPLAY).setBrightnessMode(ScreenManager.BRIGHTNESS_MODE_MANUAL);
            ScreenManager(Display.DEFAULT_DISPLAY).setScreenOffTimeout(configuration.screenTimeout, TimeUnit.MILLISECONDS);
            ScreenManager(Display.DEFAULT_DISPLAY).setBrightness(configuration.screenBrightness);
            ScreenManager(Display.DEFAULT_DISPLAY).setDisplayDensity(configuration.screenDensity);
            TimeManager().setTimeZone(configuration.timeZone)
            UpdateManager().addStatusListener(object: StatusListener {
                override fun onStatusUpdate(status: UpdateManagerStatus?) {
                    when (status?.currentState) {
                        UpdateManagerStatus.STATE_UPDATE_AVAILABLE -> {
                            Timber.d("Update available")
                            dialogUtils.showProgressDialog(this@BaseActivity, getString(R.string.progress_updating), false)
                        }
                        UpdateManagerStatus.STATE_DOWNLOADING_UPDATE -> {
                            Timber.d("Update downloading")
                            dialogUtils.showProgressDialog(this@BaseActivity, getString(R.string.progress_updating), false)
                        }
                    }
                }
            });
            UpdateManager().performUpdateNow(POLICY_APPLY_AND_REBOOT) // always apply update and reboot
        } catch (e:IllegalStateException) {
            Timber.e(e.message)
        }

        connectionLiveData = ConnectionLiveData(this)
        connectionLiveData?.observe(this, Observer { connected ->
            if(connected!!) {
                handleNetworkConnect()
            } else {
                handleNetworkDisconnect()
            }
        })
    }

    /**
     * Resets the screen timeout and brightness to the default (or user set) settings
     */
    fun setScreenDefaults() {
        Timber.d("setScreenDefaults")
        Timber.d("screenBirghness: " + configuration.screenBrightness)
        Timber.d("setScreenOffTimeout: " + configuration.screenTimeout)
        ScreenManager(Display.DEFAULT_DISPLAY).setBrightness(configuration.screenBrightness);
        ScreenManager(Display.DEFAULT_DISPLAY).setScreenOffTimeout(configuration.screenTimeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Keeps the screen on extra long time if the alarm is triggered.
     */
    fun setScreenTriggered() {
        Timber.d("setScreenTriggered")
        ScreenManager(Display.DEFAULT_DISPLAY).setBrightness(configuration.screenBrightness);
        ScreenManager(Display.DEFAULT_DISPLAY).setScreenOffTimeout(3, TimeUnit.HOURS); // 3 hours
    }

    private fun setScreenBrightness(brightness: Int) {
        ScreenManager(Display.DEFAULT_DISPLAY).setBrightness(brightness);
        ScreenManager(Display.DEFAULT_DISPLAY).setScreenOffTimeout(configuration.screenTimeout, TimeUnit.MILLISECONDS);
    }

    fun resetInactivityTimer() {
        Timber.d("resetInactivityTimer")
        dialogUtils.hideScreenSaverDialog()
        inactivityHandler?.removeCallbacks(inactivityCallback)
        inactivityHandler?.postDelayed(inactivityCallback, configuration.inactivityTime)
    }

    fun stopDisconnectTimer() {
        Timber.d("stopDisconnectTimer")
        dialogUtils.hideScreenSaverDialog()
        inactivityHandler!!.removeCallbacks(inactivityCallback)
    }

    override fun onUserInteraction() {
        Timber.d("onUserInteraction")
        resetInactivityTimer()
        setScreenDefaults()
    }

    fun readMqttOptions(): MQTTOptions {
        return MQTTOptions(preferences)
    }

    fun readWeatherOptions(): DarkSkyOptions {
        return DarkSkyOptions.from(preferences)
    }

    fun readImageOptions(): ImageOptions {
        return ImageOptions.from(preferences)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.itemId == android.R.id.home
    }

    /**
     * Show the screen saver only if the alarm isn't triggered. This shouldn't be an issue
     * with the alarm disabled because the disable time will be longer than this.
     */
    open fun showScreenSaver() {
        Timber.d("showScreenSaver")
        if (!viewModel.isAlarmTriggeredMode() && viewModel.hasScreenSaver()) {
            inactivityHandler!!.removeCallbacks(inactivityCallback)
            if(configuration.showClockScreenSaverModule()) {
                setScreenBrightness(40);
            } else if (configuration.showPhotoScreenSaver()) {
                setScreenBrightness(80);
            }
            dialogUtils.showScreenSaver(this@BaseActivity,
                    configuration.showPhotoScreenSaver(), configuration.showClockScreenSaverModule(),
                    readImageOptions(), object : ScreenSaverView.ViewListener {
                override fun onMotion() {
                    resetInactivityTimer()
                    setScreenDefaults()
                }
            }, View.OnClickListener {
                resetInactivityTimer()
                setScreenDefaults()
            })
        } else if (!viewModel.isAlarmTriggeredMode()) {
            setScreenBrightness(0);
        }
    }

    /**
     * On network disconnect show notification or alert message, clear the
     * screen saver and awake the screen. Override this method in activity
     * to for extra network disconnect handling such as bring application
     * into foreground.
     */
    open fun handleNetworkDisconnect() {
        dialogUtils.hideScreenSaverDialog()
        dialogUtils.showAlertDialogToDismiss(this@BaseActivity, getString(R.string.text_notification_network_title),
                    getString(R.string.text_notification_network_description))
        hasNetwork.set(false)
    }

    /**
     * On network connect hide any alert dialogs generated by
     * the network disconnect and clear any notifications.
     */
    open fun handleNetworkConnect() {
        dialogUtils.hideAlertDialog()
        hasNetwork.set(true)
    }

    open fun hasNetworkConnectivity(): Boolean {
        return hasNetwork.get()
    }

    private val intentFilterForWifiConnectionReceiver: IntentFilter
        get() {
            val randomIntentFilter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
            randomIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            randomIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
            return randomIntentFilter
        }

    private val wifiConnectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val action = intent.action
            if (!TextUtils.isEmpty(action)) {
                when (action) {
                    WifiManager.WIFI_STATE_CHANGED_ACTION,
                    WifiManager.SUPPLICANT_STATE_CHANGED_ACTION -> {
                        val wifiInfo = wifiManager?.connectionInfo
                        var wirelessNetworkName = wifiInfo?.ssid
                        wirelessNetworkName = wirelessNetworkName?.replace("\"", "");
                        if(configuration.networkId == wirelessNetworkName) {
                            Timber.d("WiFi connected to " + configuration.networkId)
                            Toast.makeText(this@BaseActivity, getString(R.string.toast_connecting_network), Toast.LENGTH_LONG).show()
                        } else {
                            Timber.d("WiFi not connected to " + configuration.networkId)
                        }
                    }
                }
            }
        }
    }
}