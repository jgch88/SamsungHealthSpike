package com.radium.laboratories.samsunghealthspike

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.samsung.android.sdk.healthdata.*
import java.lang.Exception
import java.util.*


class MainActivity : AppCompatActivity() {
    private var mStore: HealthDataStore? = null
    private var mResolver: HealthDataResolver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // anonymous class in kotlin = object
        mStore = HealthDataStore(this, object : HealthDataStore.ConnectionListener {
            override fun onConnectionFailed(error: HealthConnectionErrorResult) {
                println("Error ${error.errorCode}")
            }

            override fun onConnected() {
                println("Connected")
                requestPermission()
                if (isPermissionAcquired()) {
                    println("got permissions")
                    readTodayStepCountData()
                    readHeartRate()
                    print("user")
                    getUserId()
                    getDeviceId()
                    allDevices()
                } else {
                    println("requesting permissions")
                }
            }

            override fun onDisconnected() {
                println("Disconnected")
            }
        })
        mStore?.connectService()

    }

    fun getUserId() {
        val usrProfile = HealthUserProfile.getProfile(mStore)
        println(usrProfile.userName)
    }

    fun getDeviceId() {
        val device = HealthDeviceManager(mStore).localDevice
        val deviceUuid = device.uuid
        val mfg = device.manufacturer
        val model = device.model
        val custom = device.customName
        println("*********local device")
        println("$device, $deviceUuid, $mfg, $model, $custom")
    }

    fun allDevices() {
        // pass in bfn
        // if all devices doesnt include bfn, throw error
        val devices = HealthDeviceManager(mStore).allDevices
        for (device in devices) {
            val deviceUuid = device.uuid
            val mfg = device.manufacturer
            val model = device.model
            val custom = device.customName
            println("*********all devices")
            println("$device, $deviceUuid, $mfg, $model, $custom")
        }
    }

    fun readHeartRate() {
        val startTime = getTodayStartUtcTime()
        val ONE_DAY_IN_MS = 24 * 60 * 60 * 1000L
        val endTime = startTime + ONE_DAY_IN_MS

        val request = HealthDataResolver.ReadRequest.Builder()
            .setDataType(HealthConstants.HeartRate.HEALTH_DATA_TYPE)
            .setProperties(arrayOf(
                HealthConstants.HeartRate.HEART_BEAT_COUNT,
                HealthConstants.HeartRate.BINNING_DATA,
                HealthConstants.HeartRate.DEVICE_UUID,
                HealthConstants.HeartRate.CREATE_TIME,
                HealthConstants.HeartRate.HEART_RATE
            ))
            .setLocalTimeRange(HealthConstants.HeartRate.START_TIME,
                HealthConstants.HeartRate.TIME_OFFSET, startTime, endTime)
            .build()

        mResolver = HealthDataResolver(mStore, null)

        try {
            mResolver?.read(request)?.setResultListener { result ->
                try {
                    val iterator = result.iterator();
                    while (iterator.hasNext()) {
                        val data = iterator.next()
                        val heartRate = data.getLong(HealthConstants.HeartRate.HEART_BEAT_COUNT)
                        val bin = data.getString(HealthConstants.HeartRate.BINNING_DATA)
                        val uuid = data.getString(HealthConstants.HeartRate.DEVICE_UUID)
                        val time = data.getString(HealthConstants.HeartRate.CREATE_TIME)
                        val hr = data.getInt(HealthConstants.HeartRate.HEART_RATE)
                        println("***************heart rate result")
                        println("$time, $hr, $uuid")
                        println("***************heart rate bin")
                        println("$bin")
                    }
                } finally {
                    result.close()
                }
            }
        } catch (e: Exception) {
            println("error reading heart rate ${e.message}")
        }
    }

    fun readTodayStepCountData() {
        val startTime = getTodayStartUtcTime()
        val ONE_DAY_IN_MS = 24 * 60 * 60 * 1000L
        val endTime = startTime + ONE_DAY_IN_MS

        val request = HealthDataResolver.ReadRequest.Builder()
            .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
            .setProperties(arrayOf(
                HealthConstants.StepCount.COUNT,
                HealthConstants.StepCount.CREATE_TIME,
                HealthConstants.StepCount.DEVICE_UUID
            ))
                // TIME_OFFSET needs to set for showing the data's measured time properly???
            .setLocalTimeRange(HealthConstants.StepCount.START_TIME,
                HealthConstants.StepCount.TIME_OFFSET, startTime, endTime)
            .build()

        mResolver = HealthDataResolver(mStore, null)

        try {
            mResolver?.read(request)?.setResultListener { result ->

                try {
                    val iterator = result.iterator();
                    while (iterator.hasNext()) {
                        val data = iterator.next()
                        val totalCount = data.getInt(HealthConstants.StepCount.COUNT) // HealthData
                        val uuid = data.getString(HealthConstants.StepCount.DEVICE_UUID)
                        val time = data.getFloat(HealthConstants.StepCount.CREATE_TIME)
                        println("***************result")
                        println("$time, $totalCount, $uuid")
                    }
                } finally {
                    result.close()
                }
            }
        } catch (e: Exception) {
            println("error reading steps ${e.message}")
        }
    }

    fun requestPermission() {
        println("requestPermission")
        val permKey = HealthPermissionManager.PermissionKey(
            HealthConstants.StepCount.HEALTH_DATA_TYPE,
            HealthPermissionManager.PermissionType.READ
        )
        val permKey2 = HealthPermissionManager.PermissionKey(
            HealthConstants.HeartRate.HEALTH_DATA_TYPE,
            HealthPermissionManager.PermissionType.READ
        )
        val permKey3 = HealthPermissionManager.PermissionKey(
            HealthConstants.USER_PROFILE_DATA_TYPE,
            HealthPermissionManager.PermissionType.READ
        )
        val pmsManager = HealthPermissionManager(mStore);
        try {
            pmsManager.requestPermissions(mutableSetOf(permKey, permKey2,permKey3), this)
                .setResultListener { result ->
                    println("Permission callback received")
                    val resultMap = result.resultMap
                    println(resultMap.entries)
                    if (resultMap.containsValue(false)) {
                        showPermissionAlarmDialog()
                    }
                }
        } catch (e: Exception) {
            println("permission setting fails ${e.message}")
        }
    }

    fun showPermissionAlarmDialog() {
        if (isFinishing) {
            return
        }

        val alert = AlertDialog.Builder(this);
        alert.setTitle("Notice")
            .setMessage("Steps should be acquired")
            .setPositiveButton("OK", null)
            .show()
    }

    fun isPermissionAcquired(): Boolean {
        val permKey =
            HealthPermissionManager.PermissionKey(
                HealthConstants.StepCount.HEALTH_DATA_TYPE,
                HealthPermissionManager.PermissionType.READ
            )
        val permKey2 = HealthPermissionManager.PermissionKey(
            HealthConstants.HeartRate.HEALTH_DATA_TYPE,
            HealthPermissionManager.PermissionType.READ
        )
        val pmsManager = HealthPermissionManager(mStore)
        try {
            val resultMap = pmsManager.isPermissionAcquired(mutableSetOf(permKey, permKey2))
            return resultMap.get(permKey2) ?: false
        } catch (e: Exception) {
            println("Error getting permission ${e.message}")
        }
        return false
    }

    fun getTodayStartUtcTime(): Long {
        val today = Calendar.getInstance(TimeZone.getTimeZone("GMT+8:00"))

        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val result = today.timeInMillis
        println("today time $result")
        return result
    }
}
