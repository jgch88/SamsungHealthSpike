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

    fun readTodayStepCountData() {
        val startTime = getTodayStartUtcTime()
        val ONE_DAY_IN_MS = 24 * 60 * 60 * 1000L
        val endTime = startTime + ONE_DAY_IN_MS

        val request = HealthDataResolver.ReadRequest.Builder()
            .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
            .setProperties(arrayOf(HealthConstants.StepCount.COUNT))
            .setLocalTimeRange(HealthConstants.StepCount.START_TIME,
                HealthConstants.StepCount.TIME_OFFSET, startTime, endTime)
            .build()

        mResolver = HealthDataResolver(mStore, null)

        try {
            mResolver?.read(request)?.setResultListener { result ->
                var dayTime = 0L
                var totalCount = 0

                try {
                    val iterator = result.iterator();
                    while (iterator.hasNext()) {
                        val data = iterator.next()
                        dayTime = data.getLong("day_time")
                        totalCount = data.getInt("count")
                        println("***************result")
                        println("$dayTime, $totalCount")
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
        val pmsManager = HealthPermissionManager(mStore);
        try {
            pmsManager.requestPermissions(mutableSetOf(permKey, permKey2), this)
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
        val today = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        return today.timeInMillis
    }
}
