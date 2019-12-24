package jp.co.recruit.erikura.presenters.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Task
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import java.util.*
import java.util.concurrent.TimeUnit

class StartActivity : AppCompatActivity() {
    private val REQUEST_OAUTH_REQUEST_CODE = 1

    private val fitnessOptions: GoogleSignInOptionsExtension get() =
        FitnessOptions.builder()
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_HEIGHT_SUMMARY, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_CADENCE, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_HEIGHT, FitnessOptions.ACCESS_READ)
            .build()

    fun hasGoogleFitPermissions(): Boolean {
        return GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        if (!hasGoogleFitPermissions()) {
            // 権限のリクエスト
            GoogleSignIn.requestPermissions(
                this,
                REQUEST_OAUTH_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(this),
                fitnessOptions
            )
        }
        else {
            // データ取得の開始
            startFitnessSubscription()
        }

        Api(this).login("user003@example.com", "pass0000") {
            Log.d("DEBUG", "ログイン成功")
//            Api(this).displayErrorAlert(listOf("ログインに成功しました。"), caption = "成功！！！")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // OAuthのリクエストについて結果取得
        if (REQUEST_OAUTH_REQUEST_CODE == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                // データ取得の開始
                startFitnessSubscription()
            }
            else {
                // エラー表示
                Toast.makeText(applicationContext, "GoogleFitのデータ取得を許可してください", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun startFitnessSubscription() {
        Toast.makeText(applicationContext, "GoogleFit データ取得を開始します", Toast.LENGTH_LONG).show()

        val startTime = Calendar.getInstance().run {
            add(Calendar.DATE, -7)
            time
        }
        val endTime = Date()
        Log.v("START TIME: ", startTime.toString())
        Log.v("END TIME: ", endTime.toString())

        val googleSignInAccount: GoogleSignInAccount =
            GoogleSignIn.getAccountForExtension(this, fitnessOptions);

        val response: Task<DataReadResponse> =
            Fitness.getHistoryClient(this, googleSignInAccount)
                .readData(
                    DataReadRequest.Builder()
                        .read(DataType.TYPE_STEP_COUNT_DELTA)
                        .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
                        .build())

        response.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val dataSet: DataSet = task.result!!.getDataSet(DataType.TYPE_STEP_COUNT_DELTA)

                dataSet.dataPoints.forEach { point ->
                    val start = point.getStartTime(TimeUnit.MILLISECONDS)
                    val end = point.getEndTime(TimeUnit.MILLISECONDS)
                    val value = point.getValue(Field.FIELD_STEPS)

                    Log.v("POINT: START", Date(start).toString())
                    Log.v("POINT: END", Date(end).toString())
                    Log.v("POINT: VAL", value.toString())
                }


                Log.v("TASK", dataSet.toString())
            }
            else {
                Log.v("TASK", task.exception?.message ?: "ERROR", task.exception)
            }
        }
    }
}
