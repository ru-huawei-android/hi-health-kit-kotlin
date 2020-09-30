/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.sample.huawei.hihealth

import android.os.Bundle
import android.util.Log
import android.view.View
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

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val TAG: String = "MainActivity"

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
        val localDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
        val zoneTime = localDateTime.atZone(ZoneId.systemDefault())
        val endTime = zoneTime.toInstant().toEpochMilli()
        var startTime: Long = 0


        val firstDayOfWeek = getFirstDayOfWeek(Calendar.DAY_OF_WEEK)
        val firstDayOfMonth = getFirstDayOfWeek(Calendar.DAY_OF_MONTH)
        val firstDayOfYear = getFirstDayOfWeek(Calendar.DAY_OF_YEAR)

        when (radioGroup.checkedRadioButtonId) {
            R.id.day -> startTime = endTime - TimeUnit.DAYS.toMillis(1)
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
        HiHealthDataStore.execQuery(applicationContext, hiHealthDataQuery, timeout) { resultCode, data ->
            if (data != null) {
                val dataList: List<HiHealthPointData> = data as ArrayList<HiHealthPointData>
                var steps = 0;
                for (obj in dataList) {
                    steps += obj.value;
                }
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
        HiHealthDataStore.getWeight(applicationContext) { errorCode, weight ->
            if (errorCode == HiHealthError.SUCCESS) {
                result.text = "Weight: $weight"
            } else {
                showErrorMessage(getString(R.string.data_type_basic_measurement))
            }
        }
    }

    private val requestAuthorization = View.OnClickListener {
        HiHealthAuth.requestAuthorization(applicationContext, writeWeightPermissions, readPermissions) { resultCode, resultDesc ->
            when (resultCode) {
                HiHealthError.SUCCESS -> result.text = getString(R.string.req_auth_success)
                HiHealthError.FAILED -> result.text = getString(R.string.req_auth_failed)
                HiHealthError.PARAM_INVALIED -> result.text = getString(R.string.req_auth_param_invalid)
                HiHealthError.ERR_API_EXECEPTION -> result.text = getString(R.string.req_auth_err_api_ex)
                HiHealthError.ERR_PERMISSION_EXCEPTION -> result.text = getString(R.string.req_auth_err_perm_ex)
                HiHealthError.ERR_SCOPE_EXCEPTION -> result.text = getString(R.string.req_auth_err_scope_ex)
                else -> result.text = getString(R.string.req_auth_err_undefined)
            }

            Log.d(TAG, "requestAuthorization onResult:$resultCode")
        }
    }

    private fun showErrorMessage(dataType: String) {
        result.text = getString(R.string.errorMessage, dataType)
    }

    private fun getFirstDayOfWeek(dayOf: Int): Long {
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
}
