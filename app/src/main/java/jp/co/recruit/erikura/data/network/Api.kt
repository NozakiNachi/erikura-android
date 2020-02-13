package jp.co.recruit.erikura.data.network

import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.maps.model.LatLng
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.*
import jp.co.recruit.erikura.presenters.util.LocationManager
import okhttp3.Request as HttpRequest
import okhttp3.Response as HttpResponse
import org.apache.commons.io.IOUtils
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat

class Api(var context: Context) {
    companion object {
        var userSession: UserSession? = null

        val erikuraApiService: IErikuraApiService get() {
            return ErikuraApplication.instance.erikuraComponent.erikuraApiService()
        }
        val googleMapApiService: IGoogleMapApiService get() {
            return ErikuraApplication.instance.erikuraComponent.googleMapApiService()
        }

        val isLogin: Boolean get() = (userSession != null)
    }

    private var progressAlert: AlertDialog? = null

// FIXME: 送信中のリクエストのキャンセルってどうするのか？
//    client.dispatcher().cancelAll()

//    func isLogin() -> Bool {
//        return userSession != nil
//    }

//    // 再認証有効無効チェック
//    func resignInRequired() -> Bool {
//        if let expire = self.resignInSessionExpire, expire > Date() {
//            return false
//        }else {
//            return true
//        }
//    }

    fun login(email: String, password: String, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (session: UserSession) -> Unit) {
        executeObservable(
            erikuraApiService.login(LoginRequest(email = email, password = password)),
            onError =  onError
        ) { body ->
            val session = UserSession(userId = body.userId, token = body.accessToken)
            userSession = session
            onComplete(session)
        }
    }

    fun logout(onError: ((messages: List<String>?) -> Unit)? = null, onComplete: () -> Unit) {
        executeObservable(
            erikuraApiService.logout(),
            onError =  onError
        ) { body ->
            if(body.accessToken == null){
                userSession = null
            }else{
                // エラーメッセージを出す
            }
            onComplete()
        }
    }

    fun registerEmail(email: String, onError: ((messages: List<String>?) -> Unit)?=null, onComplete: (id: Int) -> Unit) {
        executeObservable(
            erikuraApiService.registerEmail(RegisterEmailRequest(email = email)),
            onError = onError
        ) { body ->
            val id = body.id
            onComplete(id)
        }
    }

    fun searchJobs(query: JobQuery, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (jobs: List<Job>) -> Unit) {
        val observable = erikuraApiService.searchJob(
            period = query.period.value,
            latitude = query.latitude ?: LocationManager.defaultLatLng.latitude,
            longitude = query.longitude ?: LocationManager.defaultLatLng.longitude,
            sortBy = query.sortBy.value,
            minimumReward = query.minimumReward,
            maximumReward = query.maximumReward,
            minimumWorkingTime = query.minimumWorkingTime,
            maximumWorkingTime = query.maximumWorkingTime,
            jobKind = query.jobKind?.id)
        executeObservable(observable, onError = onError) {
            val jobs = it.jobs
            onComplete(jobs)
        }
    }

    fun jobKinds(onError: ((messages: List<String>?) -> Unit)?=null, onComplete: (jobKinds: List<JobKind>) -> Unit) {
        executeObservable(
            erikuraApiService.jobKinds(),
            onError = onError
        ) { jobKinds ->
            onComplete(jobKinds)
        }
    }

    fun registerConfirm(confirmationToken: String, onError: ((messages: List<String>?) -> Unit)?=null, onComplete: (id: Int) -> Unit) {
        executeObservable(
            erikuraApiService.registerConfirm(ConfirmationTokenRequest(confirmationToken = confirmationToken)),
            onError = onError
        ) { body ->
            val id = body.id
            onComplete(id)
        }
    }

    fun postalCode(postalCode: String, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (prefecture: String?, city: String?, street: String?) -> Unit) {
        executeObservable(
            erikuraApiService.postalCode(postalCode),
            onError = onError
        ) { body ->
            val prefecture = body.prefecture
            val city = body.city
            val street = body.street
            onComplete(prefecture, city, street)
        }
    }

    fun initialUpdateUser(user: User, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (session: UserSession) -> Unit)  {
        executeObservable(
            erikuraApiService.initialUpdateUser(user),
            onError = onError
        ) { body ->
            val session = UserSession(userId = body.userId, token = body.accessToken)
            userSession = session
            onComplete(session)
        }
    }

    fun user(onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (user: User) -> Unit) {
        executeObservable(
            erikuraApiService.user(),
            onError = onError
        ) { user ->
            onComplete(user)
        }
    }

    fun reloadJob(job: Job, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (job: Job) -> Unit) {
        executeObservable(
            erikuraApiService.reloadJob(job.id),
            onError = onError
        ) { reloadedJob ->
            onComplete(reloadedJob)
        }
    }

    fun entry(job: Job, comment: String?, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (entryId: Int) -> Unit){
        executeObservable(
            erikuraApiService.entry(EntryRequest(job.id, comment)),
            onError = onError
        ) {body ->
            val id = body.entryId
            onComplete(id)
        }
    }

    fun cancel(job: Job, reasonCode: Int?, comment: String?, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (entryId: Int) -> Unit){
        executeObservable(
            erikuraApiService.cancel(CancelRequest(job.id, reasonCode?: 0, comment?: "")),
            onError = onError
        ){ body ->
            val id = body.entryId
            onComplete(id)
        }
    }

    fun startJob(job: Job, latLng: LatLng, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (entryId: Int) -> Unit){
        executeObservable(
            erikuraApiService.startJob(
                StartJobRequest(
                    jobId = job.id,
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    steps = 0,
                    distance = 0,
                    floorAsc = 0,
                    floorDesc = 0
            )),
            onError = onError
        ){ body ->
            val id = body.entryId
            onComplete(id)
        }
    }

    fun abortJob(job: Job, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (entryId: Int) -> Unit){
        executeObservable(
            erikuraApiService.abortJob(AbortJobRequest(job.id, job.entryId?: 0)),
            onError = onError
        ){ body ->
            val id = body.entryId
            onComplete(id)
        }
    }

    fun stopJob(job: Job, latLng: LatLng, steps: Int, distance: Double, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (entryId: Int) -> Unit){
        executeObservable(
            erikuraApiService.stopJob(
                StopJobRequest(
                    jobId = job.id,
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    steps = steps,
                    distance = distance,
                    floorAsc = 0,
                    floorDesc = 0
                )
            ),
            onError = onError
        ){ body ->
            val id = body.entryId
            onComplete(id)
        }
    }

    fun recommendedJobs(job: Job, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (jobs: List<Job>) -> Unit) {
        executeObservable(
            erikuraApiService.recommendedJobs(job.id),
            onError = onError
        ) { body ->
            onComplete(body.jobs)
        }
    }

    fun place(placeId: Int, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (place: Place) -> Unit){
        executeObservable(
            erikuraApiService.place(placeId),
            onError = onError
        ) { place ->
            onComplete(place)
        }
    }

    fun placeFavorite(placeId: Int, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (result: Boolean) -> Unit) {
        executeObservable(
            erikuraApiService.placeFavoriteCreate(FavoriteRequest(placeId)),
            onError = onError
        ) { body ->
            val result = body.result
            onComplete(result)
        }
    }

    fun placeFavoriteDelete(placeId: Int, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (result: Boolean) -> Unit) {
        executeObservable(
            erikuraApiService.placeFavoriteDelete(FavoriteRequest(placeId)),
            onError = onError
        ) { body ->
            val result = body.result
            onComplete(result)
        }
    }

    fun placeFavoriteShow(placeId: Int, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (result: Boolean) -> Unit) {
        executeObservable(
            erikuraApiService.placeFavoriteShow(placeId),
            onError = onError
        ) { body ->
            val result = body.result
            onComplete(result)
        }
    }

    fun cancelReasons( onError: ((message: List<String>?) -> Unit)? = null, onComplete: (cancelReasons: List<CancelReason>) -> Unit) {
        executeObservable(
            erikuraApiService.cancelReasons(),
            onError = onError
        ) { body ->
            onComplete(body.cancellationReasons)
        }
    }

    fun downloadResource(url: URL, destination: File, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (file: File) -> Unit) {
        // OkHttp3 クライアントを作成します
        var client = ErikuraApiServiceBuilder().httpBuilder.build()
        if(url.toString().equals(ErikuraApplication.instance.getString(R.string.jobDetails_manualImageURL))) {
            client = ErikuraApiServiceBuilder().httpBuilderForAWS.build()
        }

        val observable: Observable<HttpResponse> = Observable.create {
            try {
                val request = HttpRequest.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                it.onNext(response)
                it.onComplete()
            }
            catch (e: IOException) {
                Log.e("Error in downloading resource", e.message, e)
                it.onError(e)
            }
        }

        observable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { response ->
                    if (response.isSuccessful) {
                        response.body?.let { body ->
                            destination.outputStream().use { os ->
                                IOUtils.copy(body.byteStream(), os)
                            }
                        }
                        onComplete(destination)
                    }
                    else {
                        onError?.let {
                            it(listOf("Download Error"))
                        }
                    }
                },
                onError = { e ->
                    Log.e("Download Error", e.message, e)
                    onError?.let {
                        it(listOf(e.message ?: "Download Error"))
                    }
                }
            )
    }

    fun geocode(keyword: String, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (file: LatLng) -> Unit) {
        googleMapApiService.geocode(BuildConfig.GEOCODING_API_KEY, keyword)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { response ->
                    when(response.status) {
                        GeocodingResponse.Status.OK -> {
                            if (response.results.size > 0) {
                                val location = response.results.first().geometry.location
                                onComplete(LatLng(location.lat, location.lng))
                            }
                        }
                        GeocodingResponse.Status.ZERO_RESULTS -> {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(listOf("検索ワードに合致する住所・駅名が見つかりませんでした。"))
                        }
                        else -> {
                            Log.v("Geocoding error:", response.status.name)
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(listOf("キーワードでの検索に失敗しました"))
                        }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    (onError ?: { msgs -> displayErrorAlert(msgs) })(
                        listOf(throwable.message ?: "キーワードでの検索に失敗しました")
                    )
                }
            )
    }

    fun erikuraConfig(onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (map: ErikuraConfigMap) -> Unit) {
        executeObservable(erikuraApiService.erikuraConfig(), onError = onError) { result ->
            onComplete(result)
        }
    }

    fun ownJob(query: OwnJobQuery, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (jobs: List<Job>) -> Unit) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        executeObservable(erikuraApiService.ownJobs(
            status = query.status.code,
            reportedAtFrom = query.reportedFrom?.let{ sdf.format(it) },
            reportedAtTo = query.reportedTo?.let{ sdf.format(it) }
        ), onError = onError) { response ->
            onComplete(response.jobs)
        }
    }

    private fun <T> executeObservable(observable: Observable<Response<ApiResponse<T>>>, defaultError: String? = null, onError: ((messages: List<String>?) -> Unit)?, onComplete: (response: T) -> Unit) {
        val defaultErrorMessage = defaultError ?: context.getString(R.string.common_messages_apiError)
        showProgressAlert()
        observable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    hideProgressAlert()
                },
                onNext = { response: Response<ApiResponse<T>> ->
                    if (response.isSuccessful) {
                        // isSuccessfull の判定をしているので、body は常に取得できる想定です
                        val apiResponse: ApiResponse<T> = response.body()!!
                        if (apiResponse.hasError) {
                            processError(apiResponse.errors ?: listOf(defaultErrorMessage), onError)
                        }
                        else {
                            onComplete(apiResponse.body)
                        }
                    }
                    else {
                        when(response.code()) {
                            401 -> {
                                // FIXME: 認証必須画面への遷移を行います
                                // FIXME: Activityの NEW_TASK | CLEAR_TOP での遷移で良いか検討してください
                                Toast.makeText(context, "401 Unauthorized:\nログイン必須画面を表示します", Toast.LENGTH_LONG).show()
                            }
                            500 -> {
                                Log.v("ERROR RESPONSE", response.errorBody().toString())
                                processError(listOf(defaultErrorMessage), onError)
                            }
                            else -> {
                                Log.v("ERROR RESPONSE", response.errorBody().toString())
                                processError(listOf(defaultErrorMessage), onError)
                            }
                        }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    processError(listOf(throwable.message ?: defaultErrorMessage), onError)
                }
            )
    }

    private fun showProgressAlert() {
        // 同一のAPIインスタンスから呼ばれた場合だけでも、スピナー表示を共通化することを考える
        if (progressAlert == null) {
            progressAlert = AlertDialog.Builder(context).apply {
                setView(LayoutInflater.from(context).inflate(R.layout.dialog_progress, null, false))
                setCancelable(false)
            }.create()

            val dm = context.resources.displayMetrics
            progressAlert?.show()
            progressAlert?.window?.setLayout(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100.0f, dm).toInt(),
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100.0f, dm).toInt())
        }
    }

    private fun hideProgressAlert() {
        progressAlert?.dismiss()
        progressAlert = null
    }

    private fun processError(messages: List<String>, onError: ((messages: List<String>?) -> Unit)?) {
        (onError ?: { msgs -> displayErrorAlert(msgs) })(messages)
    }

    fun displayErrorAlert(messages: List<String>? = null, caption: String? = null) {
        AndroidSchedulers.mainThread().scheduleDirect {
            val alertDialog = AlertDialog.Builder(context)
                .apply {
                    setTitle(caption ?: context.getString(R.string.common_captions_apiError))
                    setMessage(
                        messages?.joinToString("\n") ?: context.getString(R.string.common_messages_apiError)
                    )
                    setPositiveButton(R.string.common_buttons_close) { _, _ -> }
                }.create()
            alertDialog.show()
        }
    }
}
