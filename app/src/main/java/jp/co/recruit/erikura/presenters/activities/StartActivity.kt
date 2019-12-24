package jp.co.recruit.erikura.presenters.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import jp.co.recruit.erikura.R
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.TextView
import android.widget.VideoView
import android.text.method.MovementMethod
import android.text.Html
import android.util.Log
import android.widget.Toast
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
import jp.co.recruit.erikura.data.network.Api
import java.util.*
import java.util.concurrent.TimeUnit

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        // ビデオの設定
        val v: VideoView = findViewById(R.id.v)
        v.setVideoURI(Uri.parse("android.resource://" + this.packageName + "/" + R.raw.movie))
        v.start()
        // ループ再生処理
        v.setOnCompletionListener {
            v.seekTo(0)
            v.start()
        }

        // 会員登録ボタンの設定
        val registerButton: Button = findViewById(R.id.registerButton)
        val intent: Intent = Intent(this@StartActivity, RegisterEmailActivity::class.java)
        registerButton.setOnClickListener {
            startActivity(intent)
        }

        // ログインボタンの設定
        val loginButton: Button = findViewById(R.id.loginButton)
        val intentLogin: Intent = Intent(this@StartActivity, LoginActivity::class.java)
        loginButton.setOnClickListener {
            startActivity(intentLogin)
        }

        // リンクの設定
        val tv: TextView = findViewById(R.id.textView)
        val mMethod: MovementMethod = LinkMovementMethod.getInstance()
        tv.movementMethod = mMethod
        val url = "http://www.google.co.jp"
        val link = Html.fromHtml("<a href=\"$url\">スキップして近くの仕事を探す</a>")
        tv.text = link
        tv.setOnClickListener {
            // 地図画面へ遷移
        }


//        if (!hasGoogleFitPermissions()) {
//            // 権限のリクエスト
//            GoogleSignIn.requestPermissions(
//                this,
//                REQUEST_OAUTH_REQUEST_CODE,
//                GoogleSignIn.getLastSignedInAccount(this),
//                fitnessOptions
//            )
//        }
//        else {
//            // データ取得の開始
//            startFitnessSubscription()
//        }
//
//        Api(this).login("user003@example.com", "pass0000") {
//            Log.d("DEBUG", "ログイン成功")
////            Api(this).displayErrorAlert(listOf("ログインに成功しました。"), caption = "成功！！！")
//        }
    }


    private val REQUEST_OAUTH_REQUEST_CODE = 1

    private val fitnessOptions: GoogleSignInOptionsExtension
        get() =
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
