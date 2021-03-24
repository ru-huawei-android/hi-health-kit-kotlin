package com.sample.huawei.hihealth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hihealth.error.HiHealthError
import com.huawei.hihealthkit.HiHealthDataQuery
import com.huawei.hihealthkit.HiHealthDataQueryOption
import com.huawei.hihealthkit.auth.HiHealthAuth
import com.huawei.hihealthkit.auth.HiHealthOpenPermissionType
import com.huawei.hihealthkit.data.HiHealthPointData
import com.huawei.hihealthkit.data.store.HiHealthDataStore
import com.huawei.hihealthkit.data.type.HiHealthPointType
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

private const val TAG: String = "MainActivity"

private const val HUAWEI_HEALTH_APP_PACKAGE_NAME = "com.huawei.health"

private const val PERMISSION_GRANTED = 1
private const val PERMISSION_NOT_GRANTED = 2

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    /*
        Массив пермишенов на чтение данных, которые мы хотим получить
     */
    private var readPermissions = intArrayOf(
        HiHealthOpenPermissionType.HEALTH_OPEN_PERMISSION_TYPE_READ_USER_PROFILE_FEATURE,
        HiHealthOpenPermissionType.HEALTH_OPEN_PERMISSION_TYPE_READ_DATA_POINT_STEP_SUM,
        HiHealthOpenPermissionType.HEALTH_OPEN_PERMISSION_TYPE_READ_USER_PROFILE_INFORMATION
    )

    /*
        Массив пермишенов на запись данных, которые мы хотим получить
    */
    private var writeWeightPermissions = intArrayOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestAuthorizationBtn.setOnClickListener(requestAuthorization)
        genderBtn.setOnClickListener(getGender)
        weightBtn.setOnClickListener(getWeight)
        stepBtn.setOnClickListener(getSteps)
    }


    private val getSteps = View.OnClickListener {
        val timeout = 0
        // получаем время
        val endTime = System.currentTimeMillis()
        var startTime: Long = getStartOfDay()

        val firstDayOfWeek = getFirstDayOf(Calendar.DAY_OF_WEEK)
        val firstDayOfMonth = getFirstDayOf(Calendar.DAY_OF_MONTH)
        val firstDayOfYear = getFirstDayOf(Calendar.DAY_OF_YEAR)

        when (radioGroup.checkedRadioButtonId) {
            R.id.week -> startTime = firstDayOfWeek
            R.id.month -> startTime = firstDayOfMonth
            R.id.year -> startTime = firstDayOfYear
        }

        // Получаем ArrayList<HiHealthPointData>, который представляет значения для каждого дня
        val hiHealthDataQuery = HiHealthDataQuery(
            HiHealthPointType.DATA_POINT_STEP_SUM,
            startTime,
            endTime,
            HiHealthDataQueryOption()
        )
        HiHealthDataStore.execQuery(
            applicationContext,
            hiHealthDataQuery,
            timeout
        ) { resultCode, data ->
            Log.d(TAG, "HiHealthDataStore.execQuery resultCode: $resultCode")
            if (data != null) {
                @Suppress("UNCHECKED_CAST")
                val dataList: List<HiHealthPointData> = data as ArrayList<HiHealthPointData>
                var steps = 0
                dataList.forEach { steps += it.value }
                result.text = getString(R.string.steps, steps)
            } else {
                showErrorMessage(getString(R.string.data_type_step_count))
            }
        }
    }

    private val getGender = View.OnClickListener {
        HiHealthDataStore.getGender(applicationContext) { errorCode, gender ->
            if (errorCode == HiHealthError.SUCCESS) {
                result.text = when (gender) {
                    0 -> getString(R.string.gender_female)
                    1 -> getString(R.string.gender_male)
                    else -> getString(R.string.gender_undefined)
                }
            } else {
                showErrorMessage(getString(R.string.data_type_basic_personal_inf))
            }
        }
    }


    private val getWeight = View.OnClickListener {
        HiHealthDataStore.getWeight(applicationContext) { responseCode, weight ->
            if (responseCode == HiHealthError.SUCCESS) {
                if (weight is Float) {
                    result.text = getString(R.string.weight, weight)
                }
            } else {
                showErrorMessage(getString(R.string.data_type_basic_measurement))
            }
        }
    }

    private val requestAuthorization = View.OnClickListener {
        HiHealthAuth.getDataAuthStatusEx(
            applicationContext,
            writeWeightPermissions,
            readPermissions
        ) { resultCode, resultDesc, writePermissions, readPermissions ->
            Log.d(
                TAG,
                "getDataAuthStatusEx resultCode: $resultCode, " +
                        "resultDesc: $resultDesc, " +
                        "readPermissions: ${readPermissions.asList()}, " +
                        "writePermissions: ${writePermissions.asList()}"
            )

            if (resultCode != HiHealthError.SUCCESS
                || readPermissions.contains(PERMISSION_NOT_GRANTED)
                || writePermissions.contains(PERMISSION_NOT_GRANTED)
            ) {
                startAuth()
            } else {
                result.text = getString(R.string.req_auth_already_success)
            }
        }
    }

    private fun startAuth() {
        loadingView.visibility = VISIBLE
        HiHealthAuth.requestAuthorization(
            applicationContext,
            writeWeightPermissions,
            readPermissions
        ) { resultCode, resultDesc ->
            Log.d(TAG, "requestAuthorization onResult: $resultCode, resultDesc: $resultDesc")

            loadingView.visibility = GONE

            when (resultCode) {
                HiHealthError.SUCCESS -> result.text = getString(R.string.req_auth_success)
                HiHealthError.FAILED -> {
                    result.text = getString(R.string.req_auth_failed)
                    //let user install Huawei Health app.
                    startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            data =
                                Uri.parse("appmarket://details?id=$HUAWEI_HEALTH_APP_PACKAGE_NAME")
                            // or use `data = Uri.parse("https://appgallery.cloud.huawei.com/marketshare/app/C10414141")`
                        }
                    )
                }
                HiHealthError.PARAM_INVALIED -> result.text =
                    getString(R.string.req_auth_param_invalid)
                HiHealthError.ERR_API_EXECEPTION -> {
                    result.text = getString(R.string.req_auth_err_api_ex)
                    //let user launch HiHealth and allow data collection
                    startActivity(
                        packageManager.getLaunchIntentForPackage(HUAWEI_HEALTH_APP_PACKAGE_NAME)
                    )
                }
                HiHealthError.ERR_PERMISSION_EXCEPTION -> result.text =
                    getString(R.string.req_auth_err_perm_ex)
                HiHealthError.ERR_SCOPE_EXCEPTION -> result.text =
                    getString(R.string.req_auth_err_scope_ex)
                else -> result.text = getString(R.string.req_auth_err_undefined)
            }
        }
    }

    private fun showErrorMessage(dataType: String) {
        result.text = getString(R.string.errorMessage, dataType)
    }

    private fun getFirstDayOf(dayOf: Int): Long {
        val cal = Calendar.getInstance()
        cal[Calendar.HOUR_OF_DAY] = 0 // ! clear would not reset the hour of day !
        cal.clear(Calendar.MINUTE)
        cal.clear(Calendar.SECOND)
        cal.clear(Calendar.MILLISECOND)

        return when (dayOf) {
            Calendar.DAY_OF_WEEK -> {
                cal[dayOf] = cal.firstDayOfWeek
                return cal.timeInMillis
            }
            Calendar.DAY_OF_MONTH -> {
                cal[dayOf] = 1
                return cal.timeInMillis
            }
            Calendar.DAY_OF_YEAR -> {
                cal[dayOf] = 1
                return cal.timeInMillis
            }
            else -> 0
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH]
        val day = calendar[Calendar.DATE]
        calendar[year, month, day, 0, 0] = 0
        return calendar.timeInMillis
    }
}
