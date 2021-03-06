package jp.co.recruit.erikura.presenters.util

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.data.Value
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.ErikuraApplication.Companion.applicationContext
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Singleton
class GoogleFitApiManager {
//    companion object {
//        const val REQUEST_OAUTH_REQUEST_CODE = 1
//
//        val fitnessOptions: GoogleSignInOptionsExtension
//            get() =
//                FitnessOptions.builder()
//                    .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
//                    .addDataType(DataType.AGGREGATE_HEIGHT_SUMMARY, FitnessOptions.ACCESS_READ)
//                    .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
//                    .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
//                    .addDataType(DataType.TYPE_STEP_COUNT_CADENCE, FitnessOptions.ACCESS_READ)
//                    .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
//                    .addDataType(DataType.TYPE_HEIGHT, FitnessOptions.ACCESS_READ)
//                    .build()
//    }
//
//    fun checkPermission(): Boolean {
//        return GoogleSignIn.hasPermissions(
//            GoogleSignIn.getLastSignedInAccount(ErikuraApplication.instance.applicationContext),
//            fitnessOptions
//        )
//    }
//
//    fun requestPermission(fragment: Fragment) {
//        if (!checkPermission()) {
//            // ????????????????????????
//            GoogleSignIn.requestPermissions(
//                fragment,
//                REQUEST_OAUTH_REQUEST_CODE,
//                GoogleSignIn.getLastSignedInAccount(ErikuraApplication.instance.applicationContext),
//                fitnessOptions
//            )
//        }
//    }
//
//    fun requestPermission2(activity: FragmentActivity) {
//        if (!checkPermission()) {
//            // ????????????????????????
//            GoogleSignIn.requestPermissions(
//                activity,
//                REQUEST_OAUTH_REQUEST_CODE,
//                GoogleSignIn.getLastSignedInAccount(ErikuraApplication.instance.applicationContext),
//                fitnessOptions
//            )
//        }
//    }
//
//    fun setAccount(activity: FragmentActivity): GoogleSignInAccount {
//        return GoogleSignIn.getAccountForExtension(activity, fitnessOptions)
//    }
//
//    fun startFitnessSubscription(activity: FragmentActivity) {
//        Toast.makeText(applicationContext, "GoogleFit ?????????????????????????????????", Toast.LENGTH_LONG).show()
//
////        val now = Date()
////        val startTime = DateUtils.beginningOfDay(now)
////        val endTime = now
////        Log.v("START TIME: ", startTime.toString())
////        Log.v("END TIME: ", endTime.toString())
//
//        val startTime = Calendar.getInstance().run {
//            add(Calendar.DATE, -1)
//            time
//        }
//        val endTime = Date()
//        Log.v("START TIME: ", startTime.toString())
//        Log.v("END TIME: ", endTime.toString())
//
//        val googleSignInAccount: GoogleSignInAccount =
//            GoogleSignIn.getAccountForExtension(activity, fitnessOptions)
//
////        readAggregateStepDelta(googleSignInAccount, startTime, endTime, activity)
////        readAggregateDistanceDelta(googleSignInAccount, startTime, endTime, activity)
////        readStepCountDelta(googleSignInAccount, startTime, endTime, activity)
////        readStepCountCumulative(googleSignInAccount, startTime, endTime, activity)
////        readStepCountCadence(googleSignInAccount, startTime, endTime, activity)
////        readDistanceDelta(googleSignInAccount, startTime, endTime, activity)
////        readDistanaceCumlative(googleSignInAccount, startTime, endTime, activity)
//    }
//
//    fun readAggregateStepDelta(
//        googleSignInAccount: GoogleSignInAccount,
//        startTime: Date,
//        endTime: Date,
//        activity: FragmentActivity,
//        onComplete: (value: Value) -> Unit
//    ) {
//        val request = DataReadRequest.Builder()
//            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
//            .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .bucketByTime(1, TimeUnit.DAYS) // ???????????????1???????????????
//            .build()
//
//        Fitness.getHistoryClient(activity, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//                val buckets = it.buckets // ??????????????????buckets????????????????????????????????????
//                buckets.forEach { bucket ->
//
//                    val start = Date(bucket.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(bucket.getEndTime(TimeUnit.MILLISECONDS))
//                    val dataSet = bucket.getDataSet(DataType.AGGREGATE_STEP_COUNT_DELTA)
//                    if (dataSet?.dataPoints.isNullOrEmpty()) {
//                        onComplete(Value(0))
//                    }else {
//                        dataSet?.dataPoints?.forEach { point ->
//                            val start2 = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                            val end2 = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//                            val value = point.getValue(Field.FIELD_STEPS)
//                            Log.d("Aggregate Steps", "$start $end $start2 $end2 $value")
//
//                            onComplete(value)
//                        }
//                    }
//                }
//            }
//    }
//
//    fun readAggregateDistanceDelta(
//        googleSignInAccount: GoogleSignInAccount,
//        startTime: Date,
//        endTime: Date,
//        activity: FragmentActivity,
//        onComplete: (value: Value) -> Unit
//    ) {
//        val request = DataReadRequest.Builder()
//            .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .bucketByTime(1, TimeUnit.DAYS) // ???????????????1???????????????
//            .build()
//
//        Fitness.getHistoryClient(activity, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//                val buckets = it.buckets // ??????????????????buckets????????????????????????????????????
//                buckets.forEach { bucket ->
//                    val start = Date(bucket.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(bucket.getEndTime(TimeUnit.MILLISECONDS))
//                    val dataSet = bucket.getDataSet(DataType.AGGREGATE_DISTANCE_DELTA)
//                    if (dataSet?.dataPoints.isNullOrEmpty()) {
//                        onComplete(Value(0))
//                    }else {
//                        dataSet?.dataPoints?.forEach { point ->
//                            val start2 = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                            val end2 = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//                            val value = point.getValue(Field.FIELD_DISTANCE)
//                            Log.d("Aggregate Distance", "$start $end $start2 $end2 $value")
//
//                            onComplete(value)
//                        }
//                    }
//                }
//            }
//    }
//
//    fun readStepCountDelta(
//        googleSignInAccount: GoogleSignInAccount,
//        startTime: Date,
//        endTime: Date,
//        activity: FragmentActivity
//    ) {
//        val request = DataReadRequest.Builder()
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .read(DataType.TYPE_STEP_COUNT_DELTA)
//            .build()
//
//        Fitness.getHistoryClient(activity, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//
//                // DataType??????????????????????????????????????????
//                val dataSet = it.getDataSet(DataType.TYPE_STEP_COUNT_DELTA)
//
//                // ???????????????????????????dataPoints????????????????????????????????????????????????
//                dataSet.dataPoints.forEach { point ->
//
//                    // getStartTime/getEndTime ??????????????????????????????????????????????????????
//                    val start = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//
//                    // getValue????????????Field.FIELD_STEPS??????????????????????????????????????????????????????
//                    val value = point.getValue(Field.FIELD_STEPS)
//                    Log.d("STEP DELTA", "$start $end $value")
//                }
//            }
//    }
//
//    fun readStepCountCumulative(
//        googleSignInAccount: GoogleSignInAccount,
//        startTime: Date,
//        endTime: Date,
//        activity: FragmentActivity
//    ) {
//        val request = DataReadRequest.Builder()
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .read(DataType.TYPE_STEP_COUNT_CUMULATIVE)
//            .build()
//
//        Fitness.getHistoryClient(activity, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//
//                // DataType??????????????????????????????????????????
//                val dataSet = it.getDataSet(DataType.TYPE_STEP_COUNT_CUMULATIVE)
//
//                // ???????????????????????????dataPoints????????????????????????????????????????????????
//                dataSet.dataPoints.forEach { point ->
//
//                    // getStartTime/getEndTime ??????????????????????????????????????????????????????
//                    val start = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//
//                    // getValue????????????Field.FIELD_STEPS??????????????????????????????????????????????????????
//                    val value = point.getValue(Field.FIELD_STEPS)
//                    Log.d("STEP CUMULATIVE", "$start $end $value")
//                }
//            }
//    }
//
//    fun readStepCountCadence(
//        googleSignInAccount: GoogleSignInAccount,
//        startTime: Date,
//        endTime: Date,
//        activity: FragmentActivity
//    ) {
//        val request = DataReadRequest.Builder()
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .read(DataType.TYPE_STEP_COUNT_CADENCE)
//            .build()
//
//        Fitness.getHistoryClient(activity, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//
//                // DataType??????????????????????????????????????????
//                val dataSet = it.getDataSet(DataType.TYPE_STEP_COUNT_CADENCE)
//
//                // ???????????????????????????dataPoints????????????????????????????????????????????????
//                dataSet.dataPoints.forEach { point ->
//
//                    // getStartTime/getEndTime ??????????????????????????????????????????????????????
//                    val start = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//
//                    // getValue????????????Field.FIELD_STEPS??????????????????????????????????????????????????????
//                    val value = point.getValue(Field.FIELD_STEPS)
//                    Log.d("STEP Cadence", "$start $end $value")
//                }
//            }
//    }
//
//    fun readDistanceDelta(
//        googleSignInAccount: GoogleSignInAccount,
//        startTime: Date,
//        endTime: Date,
//        activity: FragmentActivity
//    ) {
//        val request = DataReadRequest.Builder()
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .read(DataType.TYPE_DISTANCE_DELTA)
//            .build()
//
//        Fitness.getHistoryClient(activity, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//
//                // DataType??????????????????????????????????????????
//                val dataSet = it.getDataSet(DataType.TYPE_DISTANCE_DELTA)
//
//                // ???????????????????????????dataPoints????????????????????????????????????????????????
//                dataSet.dataPoints.forEach { point ->
//
//                    // getStartTime/getEndTime ??????????????????????????????????????????????????????
//                    val start = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//
//                    // getValue????????????Field.FIELD_STEPS??????????????????????????????????????????????????????
//                    val value = point.getValue(Field.FIELD_DISTANCE)
//                    Log.d("DISTANCE DELTA", "$start $end $value")
//                }
//            }
//    }
//
//    fun readDistanaceCumlative(
//        googleSignInAccount: GoogleSignInAccount,
//        startTime: Date,
//        endTime: Date,
//        activity: FragmentActivity
//    ) {
//        val request = DataReadRequest.Builder()
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .read(DataType.TYPE_DISTANCE_CUMULATIVE)
//            .build()
//
//        Fitness.getHistoryClient(activity, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//
//                // DataType??????????????????????????????????????????
//                val dataSet = it.getDataSet(DataType.TYPE_DISTANCE_CUMULATIVE)
//
//                // ???????????????????????????dataPoints????????????????????????????????????????????????
//                dataSet.dataPoints.forEach { point ->
//
//                    // getStartTime/getEndTime ??????????????????????????????????????????????????????
//                    val start = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//
//                    // getValue????????????Field.FIELD_STEPS??????????????????????????????????????????????????????
//                    val value = point.getValue(Field.FIELD_DISTANCE)
//                    Log.d("Distance Cumulative", "$start $end $value")
//                }
//            }
//    }
}