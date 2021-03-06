package com.thanksmister.iot.mqtt.alarmpanel.network

import android.text.TextUtils
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration
import com.thanksmister.iot.mqtt.alarmpanel.utils.AlarmUtils
import com.thanksmister.iot.mqtt.alarmpanel.utils.AlarmUtils.Companion.ALARM_COMMAND_TOPIC
import com.thanksmister.iot.mqtt.alarmpanel.utils.AlarmUtils.Companion.ALARM_STATE_TOPIC
import com.thanksmister.iot.mqtt.alarmpanel.utils.ComponentUtils.*
import com.thanksmister.iot.mqtt.alarmpanel.utils.DeviceUtils
import dpreference.DPreference
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

/**
 * For original implementation see https://github.com/androidthings/sensorhub-cloud-iot.
 */
class MQTTOptions @Inject
constructor(private val sharedPreferences: DPreference) {

    val isValid: Boolean
        get() = if (getTlsConnection()) {
            !TextUtils.isEmpty(getBroker()) &&
                    !TextUtils.isEmpty(getClientId()) &&
                    getStateTopics().isNotEmpty() &&
                    !TextUtils.isEmpty(getCommandTopic()) &&
                    !TextUtils.isEmpty(getStateTopic()) &&
                    !TextUtils.isEmpty(getUsername()) &&
                    !TextUtils.isEmpty(getPassword())
        } else !TextUtils.isEmpty(getBroker()) &&
                !TextUtils.isEmpty(getClientId()) &&
                getStateTopics().isNotEmpty() &&
                !TextUtils.isEmpty(getCommandTopic())

    val brokerUrl: String
        get() = if (!TextUtils.isEmpty(getBroker())) {
            if (getBroker().contains("http://") || getBroker().contains("https://")) {
                String.format(Locale.getDefault(), HTTP_BROKER_URL_FORMAT, getBroker(), getPort())
            } else if (getTlsConnection()) {
                String.format(Locale.getDefault(), SSL_BROKER_URL_FORMAT, getBroker(), getPort())
            } else {
                String.format(Locale.getDefault(), TCP_BROKER_URL_FORMAT, getBroker(), getPort())
            }
        } else ""

    fun getBroker(): String {
        return sharedPreferences.getPrefString(PREF_BROKER, "")
    }

    fun getClientId(): String {
        var clientId = sharedPreferences.getPrefString(PREF_CLIENT_ID, null)
        if (TextUtils.isEmpty(clientId)) {
            clientId = DeviceUtils.uuIdHash
        }
        return clientId
    }

    fun getCommandTopic(): String {
        return sharedPreferences.getPrefString(PREF_COMMAND_TOPIC, ALARM_COMMAND_TOPIC)
    }

    fun getStateTopic(): String {
        return sharedPreferences.getPrefString(PREF_STATE_TOPIC, ALARM_STATE_TOPIC)
    }

    fun getStateTopics(): Array<String> {
        val topics = ArrayList<String>()
        topics.add(sharedPreferences.getPrefString(PREF_STATE_TOPIC, ALARM_STATE_TOPIC))
        topics.add(sharedPreferences.getPrefString(PREF_NOTIFICATION_TOPIC, NOTIFICATION_STATE_TOPIC))
        topics.add(sharedPreferences.getPrefString(PREF_CAPTURE_TOPIC, IMAGE_CAPTURE_STATE_TOPIC))
        return topics.toArray(arrayOf<String>())
    }

    fun getNotificationTopic(): String? {
        return sharedPreferences.getPrefString(PREF_NOTIFICATION_TOPIC, NOTIFICATION_STATE_TOPIC)
    }

    fun getCameraTopic(): String? {
        return sharedPreferences.getPrefString(PREF_CAPTURE_TOPIC, IMAGE_CAPTURE_STATE_TOPIC)
    }

    fun getUsername(): String {
        return sharedPreferences.getPrefString(PREF_USERNAME, "")
    }

    fun getPassword(): String {
        return sharedPreferences.getPrefString(PREF_PASSWORD, "")
    }

    fun getPort(): Int {
        return sharedPreferences.getPrefInt(PREF_PORT, AlarmUtils.PORT)
    }

    fun getTlsConnection(): Boolean {
        return sharedPreferences.getPrefBoolean(PREF_TLS_CONNECTION, false)
    }

    fun setUsername(value: String) {
        this.sharedPreferences.setPrefString(PREF_USERNAME, value)
        setOptionsUpdated(true)
    }

    fun setClientId(value: String) {
        this.sharedPreferences.setPrefString(PREF_CLIENT_ID, value)
        setOptionsUpdated(true)
    }

    fun setBroker(value: String) {
        this.sharedPreferences.setPrefString(PREF_BROKER, value)
        setOptionsUpdated(true)
    }

    fun setPort(value: Int) {
        this.sharedPreferences.setPrefInt(PREF_PORT, value)
        setOptionsUpdated(true)
    }

    fun setPassword(value: String) {
        this.sharedPreferences.setPrefString(PREF_PASSWORD, value)
        setOptionsUpdated(true)
    }

    fun setCommandTopic(value: String) {
        this.sharedPreferences.setPrefString(PREF_COMMAND_TOPIC, value)
        setOptionsUpdated(true)
    }

    fun setAlarmTopic(value: String) {
        this.sharedPreferences.setPrefString(PREF_STATE_TOPIC, value)
        setOptionsUpdated(true)
    }

    fun setNotificationTopic(value: String) {
        this.sharedPreferences.setPrefString(PREF_NOTIFICATION_TOPIC, value)
        setOptionsUpdated(true)
    }

    fun setCaptureTopic(value: String) {
        this.sharedPreferences.setPrefString(PREF_CAPTURE_TOPIC, value)
        setOptionsUpdated(true)
    }

    fun setTlsConnection(value: Boolean) {
        this.sharedPreferences.setPrefBoolean(PREF_TLS_CONNECTION, value)
        setOptionsUpdated(true)
    }

    fun setOptionsUpdated(value: Boolean) {
        this.sharedPreferences.setPrefBoolean(MQTT_OPTIONS_UPDATED, value)
    }

    fun hasUpdates(): Boolean {
        val updates = sharedPreferences.getPrefBoolean(MQTT_OPTIONS_UPDATED, false)
        Timber.d("Updates: " + updates)
        return updates
    }

    fun reset() {
        sharedPreferences.removePreference(PREF_STATE_TOPIC)
        sharedPreferences.removePreference(PREF_NOTIFICATION_TOPIC)
        sharedPreferences.removePreference(PREF_CAPTURE_TOPIC)
        sharedPreferences.removePreference(PREF_USERNAME)
        sharedPreferences.removePreference(PREF_COMMAND_TOPIC)
        sharedPreferences.removePreference(PREF_TLS_CONNECTION)
        sharedPreferences.removePreference(PREF_PASSWORD)
        sharedPreferences.removePreference(PREF_PORT)
        sharedPreferences.removePreference(PREF_CLIENT_ID)
        sharedPreferences.removePreference(PREF_PASSWORD)
        sharedPreferences.removePreference(PREF_BROKER)
        sharedPreferences.removePreference(MQTT_OPTIONS_UPDATED)
    }

    companion object {
        const val SSL_BROKER_URL_FORMAT = "ssl://%s:%d"
        const val TCP_BROKER_URL_FORMAT = "tcp://%s:%d"
        const val HTTP_BROKER_URL_FORMAT = "%s:%d"
        const val PREF_STATE_TOPIC = "pref_alarm_topic"
        const val PREF_NOTIFICATION_TOPIC = "pref_notification_topic"
        const val PREF_CAPTURE_TOPIC = "pref_capture_topic"
        const val PREF_USERNAME = "pref_username"
        const val PREF_COMMAND_TOPIC = "pref_command_topic"
        const val PREF_TLS_CONNECTION = "pref_tls_connection"
        const val PREF_PASSWORD = "pref_password"
        const val PREF_PORT = "pref_port"
        const val PREF_CLIENT_ID = "pref_client_id"
        const val PREF_BROKER = "pref_broker"
        const val MQTT_OPTIONS_UPDATED = "pref_mqtt_options_updated"
    }
}