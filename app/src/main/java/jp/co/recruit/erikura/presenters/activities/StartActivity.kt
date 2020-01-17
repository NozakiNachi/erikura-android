package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityStartBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity

class StartActivity : AppCompatActivity(), StartEventHandlers {
    lateinit var video: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        // エラーメッセージを受け取る
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if(errorMessages != null){
            Api(this).displayErrorAlert(errorMessages.asList())
        }

        val binding: ActivityStartBinding = DataBindingUtil.setContentView(this, R.layout.activity_start)
        binding.lifecycleOwner = this
        binding.handlers = this

        video = findViewById(R.id.v)

        // ビデオの設定
        video.setVideoURI(Uri.parse("android.resource://" + this.packageName + "/" + R.raw.movie))
        video.start()
        // ループ再生処理
        video.setOnCompletionListener {
            video.seekTo(0)
            video.start()
        }

//        // 会員登録ボタンの設定
//        val registerButton: Button = findViewById(R.id.registerButton)
//        val intent: Intent = Intent(this@StartActivity, RegisterEmailActivity::class.java)
//        registerButton.setOnClickListener {
//            startActivity(intent)
//        }
//
//        // ログインボタンの設定
//        val loginButton: Button = findViewById(R.id.loginButton)
//        val intentLogin: Intent = Intent(this@StartActivity, LoginActivity::class.java)
//        loginButton.setOnClickListener {
//            startActivity(intentLogin)
//        }
//
//        // リンクの設定
//        val tv: TextView = findViewById(R.id.textView)
//        val mMethod: MovementMethod = LinkMovementMethod.getInstance()
//        tv.movementMethod = mMethod
//        val url = "http://www.google.co.jp"
//        val link = HtmlCompat.fromHtml("<a href=\"$url\">スキップして近くの仕事を探す</a>", HtmlCompat.FROM_HTML_MODE_COMPACT)
//        tv.text = link

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

        if (Api.isLogin) {
            // すでにログイン済の場合には以降の処理はスキップして、地図画面に遷移します
            startActivity(Intent(this, MapViewActivity::class.java))
            return
        }
    }

    override fun onResume() {
        super.onResume()
        video.resume()
        video.start()
    }

    override fun onPause() {
        super.onPause()
        video.stopPlayback()
    }

    override fun onClickRegisterButton(view: View) {
        val intent = Intent(this, RegisterEmailActivity::class.java)
        startActivity(intent)
    }

    override fun onClickLoginButton(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    override fun onClickStartWithoutLogin(view: View) {
        // 地図画面へ遷移
        val intent = Intent(this, MapViewActivity::class.java)
        startActivity(intent)
    }

//    private val REQUEST_OAUTH_REQUEST_CODE = 1
//
//    private val fitnessOptions: GoogleSignInOptionsExtension
//        get() =
//            FitnessOptions.builder()
//                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.AGGREGATE_HEIGHT_SUMMARY, FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_STEP_COUNT_CADENCE, FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_HEIGHT, FitnessOptions.ACCESS_READ)
//                .build()
//
//    fun hasGoogleFitPermissions(): Boolean {
//        return GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        // OAuthのリクエストについて結果取得
//        if (REQUEST_OAUTH_REQUEST_CODE == requestCode) {
//            if (resultCode == Activity.RESULT_OK) {
//                // データ取得の開始
//                startFitnessSubscription()
//            }
//            else {
//                // エラー表示
//                Toast.makeText(applicationContext, "GoogleFitのデータ取得を許可してください", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    fun startFitnessSubscription() {
//        Toast.makeText(applicationContext, "GoogleFit データ取得を開始します", Toast.LENGTH_LONG).show()
//
//        val startTime = Calendar.getInstance().run {
//            add(Calendar.DATE, -7)
//            time
//        }
//        val endTime = Date()
//        Log.v("START TIME: ", startTime.toString())
//        Log.v("END TIME: ", endTime.toString())
//
//        val googleSignInAccount: GoogleSignInAccount =
//            GoogleSignIn.getAccountForExtension(this, fitnessOptions);
//
//        val response: Task<DataReadResponse> =
//            Fitness.getHistoryClient(this, googleSignInAccount)
//                .readData(
//                    DataReadRequest.Builder()
//                        .read(DataType.TYPE_STEP_COUNT_DELTA)
//                        .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//                        .build())
//
//        response.addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                val dataSet: DataSet = task.result!!.getDataSet(DataType.TYPE_STEP_COUNT_DELTA)
//
//                dataSet.dataPoints.forEach { point ->
//                    val start = point.getStartTime(TimeUnit.MILLISECONDS)
//                    val end = point.getEndTime(TimeUnit.MILLISECONDS)
//                    val value = point.getValue(Field.FIELD_STEPS)
//
//                    Log.v("POINT: START", Date(start).toString())
//                    Log.v("POINT: END", Date(end).toString())
//                    Log.v("POINT: VAL", value.toString())
//                }
//
//
//                Log.v("TASK", dataSet.toString())
//            }
//            else {
//                Log.v("TASK", task.exception?.message ?: "ERROR", task.exception)
//            }
//        }
//    }

//    fun startFitnessSubscription() {
//        Toast.makeText(applicationContext, "GoogleFit データ取得を開始します", Toast.LENGTH_LONG).show()
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
//            GoogleSignIn.getAccountForExtension(this, fitnessOptions)
//
//        readAggregateStepDelta(googleSignInAccount, startTime, endTime)
//        readAggregateDistanceDelta(googleSignInAccount, startTime, endTime)
//        readStepCountDelta(googleSignInAccount, startTime, endTime)
//        readStepCountCumulative(googleSignInAccount, startTime, endTime)
//        readStepCountCadence(googleSignInAccount, startTime, endTime)
//        readDistanceDelta(googleSignInAccount, startTime, endTime)
//        readDistanaceCumlative(googleSignInAccount, startTime, endTime)
//    }
//
//    fun readAggregateStepDelta(googleSignInAccount: GoogleSignInAccount, startTime: Date, endTime: Date) {
//        val request = DataReadRequest.Builder()
//            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
//            .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .bucketByTime(1, TimeUnit.DAYS) // 集計間隔を1日毎に指定
//            .build()
//
//        Fitness.getHistoryClient(this, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//                val buckets = it.buckets // 集計データはbucketsというところに入ってくる
//                buckets.forEach { bucket ->
//
//                    val start = Date(bucket.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(bucket.getEndTime(TimeUnit.MILLISECONDS))
//                    val dataSet = bucket.getDataSet(DataType.AGGREGATE_STEP_COUNT_DELTA)
//                    dataSet?.dataPoints?.forEach { point ->
//                        val start2 = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                        val end2 = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//                        val value = point.getValue(Field.FIELD_STEPS)
//                        Log.d("Aggregate Steps", "$start $end $start2 $end2 $value")
//                    }
//                }
//            }
//    }
//
//    fun readAggregateDistanceDelta(googleSignInAccount: GoogleSignInAccount, startTime: Date, endTime: Date) {
//        val request = DataReadRequest.Builder()
//            .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .bucketByTime(1, TimeUnit.DAYS) // 集計間隔を1日毎に指定
//            .build()
//
//        Fitness.getHistoryClient(this, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//                val buckets = it.buckets // 集計データはbucketsというところに入ってくる
//                buckets.forEach { bucket ->
//                    val start = Date(bucket.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(bucket.getEndTime(TimeUnit.MILLISECONDS))
//                    val dataSet = bucket.getDataSet(DataType.AGGREGATE_DISTANCE_DELTA)
//                    dataSet?.dataPoints?.forEach { point ->
//                        val start2 = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                        val end2 = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//                        val value = point.getValue(Field.FIELD_DISTANCE)
//                        Log.d("Aggregate Distance", "$start $end $start2 $end2 $value")
//                    }
//                }
//            }
//    }
//
//    fun readStepCountDelta(googleSignInAccount: GoogleSignInAccount, startTime: Date, endTime: Date) {
//        val request = DataReadRequest.Builder()
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .read(DataType.TYPE_STEP_COUNT_DELTA)
//            .build()
//
//        Fitness.getHistoryClient(this, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//
//                // DataTypeを指定し、データセットを取得
//                val dataSet = it.getDataSet(DataType.TYPE_STEP_COUNT_DELTA)
//
//                // データセットの中のdataPointsの中に歩数情報リストが含まれてる
//                dataSet.dataPoints.forEach { point ->
//
//                    // getStartTime/getEndTime でその情報がどの時間のものかが分かる
//                    val start = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//
//                    // getValueの引数にField.FIELD_STEPSを指定することで、歩数値が取得できる
//                    val value = point.getValue(Field.FIELD_STEPS)
//                    Log.d("STEP DELTA", "$start $end $value")
//                }
//            }
//    }
//
//    fun readStepCountCumulative(googleSignInAccount: GoogleSignInAccount, startTime: Date, endTime: Date) {
//        val request = DataReadRequest.Builder()
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .read(DataType.TYPE_STEP_COUNT_CUMULATIVE)
//            .build()
//
//        Fitness.getHistoryClient(this, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//
//                // DataTypeを指定し、データセットを取得
//                val dataSet = it.getDataSet(DataType.TYPE_STEP_COUNT_CUMULATIVE)
//
//                // データセットの中のdataPointsの中に歩数情報リストが含まれてる
//                dataSet.dataPoints.forEach { point ->
//
//                    // getStartTime/getEndTime でその情報がどの時間のものかが分かる
//                    val start = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//
//                    // getValueの引数にField.FIELD_STEPSを指定することで、歩数値が取得できる
//                    val value = point.getValue(Field.FIELD_STEPS)
//                    Log.d("STEP CUMULATIVE", "$start $end $value")
//                }
//            }
//    }
//
//    fun readStepCountCadence(googleSignInAccount: GoogleSignInAccount, startTime: Date, endTime: Date) {
//        val request = DataReadRequest.Builder()
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .read(DataType.TYPE_STEP_COUNT_CADENCE)
//            .build()
//
//        Fitness.getHistoryClient(this, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//
//                // DataTypeを指定し、データセットを取得
//                val dataSet = it.getDataSet(DataType.TYPE_STEP_COUNT_CADENCE)
//
//                // データセットの中のdataPointsの中に歩数情報リストが含まれてる
//                dataSet.dataPoints.forEach { point ->
//
//                    // getStartTime/getEndTime でその情報がどの時間のものかが分かる
//                    val start = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//
//                    // getValueの引数にField.FIELD_STEPSを指定することで、歩数値が取得できる
//                    val value = point.getValue(Field.FIELD_STEPS)
//                    Log.d("STEP Cadence", "$start $end $value")
//                }
//            }
//    }
//
//    fun readDistanceDelta(googleSignInAccount: GoogleSignInAccount, startTime: Date, endTime: Date) {
//        val request = DataReadRequest.Builder()
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .read(DataType.TYPE_DISTANCE_DELTA)
//            .build()
//
//        Fitness.getHistoryClient(this, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//
//                // DataTypeを指定し、データセットを取得
//                val dataSet = it.getDataSet(DataType.TYPE_DISTANCE_DELTA)
//
//                // データセットの中のdataPointsの中に歩数情報リストが含まれてる
//                dataSet.dataPoints.forEach { point ->
//
//                    // getStartTime/getEndTime でその情報がどの時間のものかが分かる
//                    val start = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//
//                    // getValueの引数にField.FIELD_STEPSを指定することで、歩数値が取得できる
//                    val value = point.getValue(Field.FIELD_DISTANCE)
//                    Log.d("DISTANCE DELTA", "$start $end $value")
//                }
//            }
//    }
//
//    fun readDistanaceCumlative(googleSignInAccount: GoogleSignInAccount, startTime: Date, endTime: Date) {
//        val request = DataReadRequest.Builder()
//            .setTimeRange(startTime.time, endTime.time, TimeUnit.MILLISECONDS)
//            .read(DataType.TYPE_DISTANCE_CUMULATIVE)
//            .build()
//
//        Fitness.getHistoryClient(this, googleSignInAccount)
//            .readData(request)
//            .addOnSuccessListener {
//
//                // DataTypeを指定し、データセットを取得
//                val dataSet = it.getDataSet(DataType.TYPE_DISTANCE_CUMULATIVE)
//
//                // データセットの中のdataPointsの中に歩数情報リストが含まれてる
//                dataSet.dataPoints.forEach { point ->
//
//                    // getStartTime/getEndTime でその情報がどの時間のものかが分かる
//                    val start = Date(point.getStartTime(TimeUnit.MILLISECONDS))
//                    val end = Date(point.getEndTime(TimeUnit.MILLISECONDS))
//
//                    // getValueの引数にField.FIELD_STEPSを指定することで、歩数値が取得できる
//                    val value = point.getValue(Field.FIELD_DISTANCE)
//                    Log.d("Distance Cumulative", "$start $end $value")
//                }
//            }
//    }
}

interface StartEventHandlers {
    fun onClickRegisterButton(view: View)
    fun onClickLoginButton(view: View)
    fun onClickStartWithoutLogin(view: View)
}