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

package com.thanksmister.iot.mqtt.alarmpanel.ui.activities

import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import com.thanksmister.iot.mqtt.alarmpanel.BaseActivity
import com.thanksmister.iot.mqtt.alarmpanel.BuildConfig
import com.thanksmister.iot.mqtt.alarmpanel.R
import com.thanksmister.iot.mqtt.alarmpanel.ui.fragments.ControlsFragment
import com.thanksmister.iot.mqtt.alarmpanel.ui.fragments.MainFragment
import com.thanksmister.iot.mqtt.alarmpanel.ui.fragments.PlatformFragment
import com.thanksmister.iot.mqtt.alarmpanel.ui.modules.CameraModule
import com.thanksmister.iot.mqtt.alarmpanel.ui.modules.MQTTModule
import com.thanksmister.iot.mqtt.alarmpanel.ui.modules.TextToSpeechModule
import com.thanksmister.iot.mqtt.alarmpanel.utils.AlarmUtils
import com.thanksmister.iot.mqtt.alarmpanel.utils.ComponentUtils.NOTIFICATION_STATE_TOPIC
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber


class MainActivity : BaseActivity(), ViewPager.OnPageChangeListener, ControlsFragment.OnControlsFragmentListener,
        MQTTModule.MQTTListener, CameraModule.CallbackListener, MainFragment.OnMainFragmentListener, PlatformFragment.OnPlatformFragmentListener {

    private lateinit var pagerAdapter: PagerAdapter

    private var textToSpeechModule: TextToSpeechModule? = null
    private var mqttModule: MQTTModule? = null
    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null
    private var cameraModule: CameraModule? = null
    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBackgroundThread = HandlerThread("BackgroundThread")
        mBackgroundThread?.start()
        mBackgroundHandler = Handler(mBackgroundThread?.looper)

        pagerAdapter = MainSlidePagerAdapter(supportFragmentManager)
        view_pager.adapter = pagerAdapter
        view_pager.addOnPageChangeListener(this)
        view_pager.setPagingEnabled(false)

        if(BuildConfig.DEBUG) {
            configuration.alarmCode = BuildConfig.ALARM_CODE
            readWeatherOptions().darkSkyKey = BuildConfig.DARK_SKY_KEY
            readWeatherOptions().setLat(BuildConfig.LATITUDE)
            readWeatherOptions().setLon(BuildConfig.LONGITUDE)
            readMqttOptions().setBroker(BuildConfig.BROKER)
            configuration.webUrl = BuildConfig.HASS_URL
            configuration.setMailFrom(BuildConfig.MAIL_FROM)
            configuration.setMailGunApiKey(BuildConfig.MAIL_GUN_KEY)
            configuration.setMailTo(BuildConfig.MAIL_TO)
            configuration.setMailGunUrl(BuildConfig.MAIL_GUN_URL)
            configuration.telegramChatId = BuildConfig.TELEGRAM_CHAT_ID
            configuration.telegramToken = BuildConfig.TELEGRAM_TOKEN
            readImageOptions().setClientId(BuildConfig.IMGUR_CLIENT_ID)
            readImageOptions().setTag(BuildConfig.IMGUR_TAG) // Imgur tags
            readWeatherOptions().setIsCelsius(true)
            configuration.isFirstTime = false
            configuration.setHasNotifications(true)
            configuration.setClockScreenSaverModule(true)
            configuration.setHasCamera(true)
            configuration.setWebModule(true)
            configuration.setShowWeatherModule(true)
            configuration.setTssModule(true)
        }

        if (configuration.isFirstTime) {
            alertDialog = AlertDialog.Builder(this@MainActivity, R.style.CustomAlertDialog)
                    .setMessage(getString(R.string.dialog_first_time))
                    .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { _, _ ->
                        configuration.isFirstTime = false;
                        val intent = SettingsActivity.createStartIntent(this@MainActivity)
                        startActivity(intent)
                    })
                    .show()
        }
    }

    public override fun onStart() {
        super.onStart()
        lifecycle.addObserver(dialogUtils)
        disposable.add(viewModel.getAlarmState()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ state ->
                    Timber.e("Alarm state: " + state)
                    Timber.e("Alarm mode: " + viewModel.getAlarmMode())
                    this@MainActivity.runOnUiThread({
                        when (state) {
                            AlarmUtils.STATE_DISARM,
                            AlarmUtils.STATE_ARM_AWAY,
                            AlarmUtils.STATE_ARM_HOME -> {
                                acquireTemporaryWakeLock()
                                setScreenDefaults()
                                resetInactivityTimer()
                            }
                            AlarmUtils.STATE_TRIGGERED -> {
                                acquireTemporaryWakeLock()
                                setScreenTriggered()
                                awakenDeviceForAction()
                            }
                            AlarmUtils.STATE_PENDING -> {
                                acquireTemporaryWakeLock()
                                setScreenDefaults()
                                awakenDeviceForAction()
                            }
                        }
                    })
                }, { error -> Timber.e("Unable to get message: " + error)}))
    }

    override fun onResume() {
        super.onResume()
        resetInactivityTimer()
        mBackgroundHandler!!.post(initializeOnBackground)
        setViewPagerState()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (mBackgroundThread != null) mBackgroundThread!!.quit()
        } catch (t: Throwable) {
            // close quietly
        }
        mBackgroundThread = null
        mBackgroundHandler = null
        if(alertDialog != null) {
            alertDialog?.dismiss()
            alertDialog = null
        }
    }

    override fun onBackPressed() {
        if (view_pager.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            view_pager.currentItem = view_pager.currentItem - 1
        }
    }

    override fun navigateAlarmPanel() {
        view_pager.currentItem = 0
    }

    override fun navigatePlatformPanel() {
        view_pager.currentItem = 1
    }

    private fun setViewPagerState() {
        if (viewModel.hasPlatform()) {
            view_pager.setPagingEnabled(true)
        } else {
            view_pager.setPagingEnabled(false)
        }
    }

    override fun publishArmedHome() {
        if (mqttModule != null) {
            mqttModule?.publish(AlarmUtils.COMMAND_ARM_HOME)
        }
    }

    override fun publishArmedAway() {
        if (mqttModule != null) {
            mqttModule?.publish(AlarmUtils.COMMAND_ARM_AWAY)
        }
    }

    override fun publishDisarmed() {
        if (mqttModule != null) {
            mqttModule?.publish(AlarmUtils.COMMAND_DISARM)
        }
        Handler().postDelayed({ captureImage() }, 300)
    }

    private val initializeOnBackground = Runnable {

        if (textToSpeechModule == null && viewModel.hasTss()) {
            textToSpeechModule = TextToSpeechModule(this@MainActivity, configuration)
            runOnUiThread({
                lifecycle.addObserver(textToSpeechModule!!)
            })
        }

        if (mqttModule == null && readMqttOptions().isValid) {
            mqttModule = MQTTModule(this@MainActivity.applicationContext, readMqttOptions(),this@MainActivity)
            runOnUiThread({
                lifecycle.addObserver(mqttModule!!)
            })
        }

        if (cameraModule == null && viewModel.hasCamera()) {
            cameraModule = CameraModule(this@MainActivity, mBackgroundHandler!!,this@MainActivity)
            runOnUiThread({
                lifecycle.addObserver(cameraModule!!)
            })
        }
    }

    @LayoutRes
    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun handleNetworkConnect() {
        super.handleNetworkConnect()
        if (mqttModule != null) {
            mqttModule?.restart()
        }
    }

    override fun handleNetworkDisconnect() {
        super.handleNetworkDisconnect()
        if (mqttModule != null) {
            mqttModule?.pause()
        }
    }

    /**
     * We need to awaken the device and allow the user to take action.
     */
    private fun awakenDeviceForAction() {
        Timber.d("awakenDeviceForAction")
        stopDisconnectTimer() // stop screen saver mode
        if (view_pager != null && pagerAdapter.count > 0) {
            dialogUtils.hideAlertDialog()
            view_pager.currentItem = 0
        }
    }

    private fun captureImage() {
        if (cameraModule != null && viewModel.hasCamera()) {
            cameraModule?.takePicture(configuration.getCameraRotate()!!)
        }
    }

    override fun onMQTTMessage(id: String, topic: String, payload: String) {
        if(NOTIFICATION_STATE_TOPIC == topic) {
            this@MainActivity.runOnUiThread({
                awakenDeviceForAction()
                setScreenDefaults()
                if (viewModel.hasAlerts()) {
                    dialogUtils.showAlertDialog(this@MainActivity, payload)
                }
                if (textToSpeechModule != null && viewModel.hasTss()) {
                    textToSpeechModule!!.speakText(payload)
                }
            })
        }
        disposable.add(viewModel.insertMessage(id, topic, payload)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { error -> Timber.e("onMQTTMessage error" + error.message)}))
    }

    override fun onMQTTException(message: String) {
        this@MainActivity.runOnUiThread {
            dialogUtils.showAlertDialog(this@MainActivity, message)
        }
    }

    override fun onMQTTDisconnect() {
        this@MainActivity.runOnUiThread {
            dialogUtils.showAlertDialog(this@MainActivity, getString(R.string.error_mqtt_connection), DialogInterface.OnClickListener { _, _ ->
                if (mqttModule != null) {
                    mqttModule!!.restart()
                }
            })
        }
    }

    override fun onCameraException(message: String) {
        this@MainActivity.runOnUiThread {
            dialogUtils.showAlertDialog(this@MainActivity, message)
        }
    }

    override fun onCameraComplete(bitmap: Bitmap) {
        viewModel.sendCapturedImage(bitmap)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun manuallyLaunchScreenSaver() {
        showScreenSaver()
    }

    private inner class MainSlidePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            when (position) {
                0 -> return MainFragment.newInstance()
                1 -> return PlatformFragment.newInstance()
                else -> return MainFragment.newInstance()
            }
        }
        override fun getCount(): Int {
            return 2
        }
    }
}