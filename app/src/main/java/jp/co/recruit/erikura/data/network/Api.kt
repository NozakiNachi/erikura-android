package jp.co.recruit.erikura.data.network

import android.app.Activity
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
import okhttp3.OkHttpClient
import okhttp3.Request as HttpRequest
import okhttp3.Response as HttpResponse
import org.apache.commons.io.IOUtils
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.URL

class Api(var activity: Activity) {
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
        erikuraApiService.login(LoginRequest(email = email, password = password))
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.hasError) {
                        activity.runOnUiThread {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(it.errors)
                        }
                    }
                    else {
                        val session = UserSession(userId = it.body.userId, token = it.body.accessToken)
                        userSession = session
                        activity.runOnUiThread { onComplete(session) }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    activity.runOnUiThread {
                        (onError ?: { msgs -> displayErrorAlert(msgs) })(
                            listOf(throwable.message ?: activity.getString(R.string.common_messages_apiError))
                        )
                    }
                }
            )
    }

    fun registerEmail(email: String, onError: ((messages: List<String>?) -> Unit)?=null, onComplete: (id: Int) -> Unit) {
        erikuraApiService.registerEmail(RegisterEmailRequest(email = email))
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.hasError) {
                        activity.runOnUiThread {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(it.errors)
                        }
                    }
                    else {
                        val id = it.body.id
                        activity.runOnUiThread { onComplete(id) }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    activity.runOnUiThread {
                        (onError ?: { msgs -> displayErrorAlert(msgs) })(
                            listOf(throwable.message ?: activity.getString(R.string.common_messages_apiError))
                        )
                    }
                }
            )
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
        erikuraApiService.jobKinds()
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.hasError) {
                        activity.runOnUiThread {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(it.errors)
                        }
                    }
                    else {
                        val jobKinds = it.body
                        activity.runOnUiThread { onComplete(jobKinds) }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    activity.runOnUiThread {
                        (onError ?: { msgs -> displayErrorAlert(msgs) })(
                            listOf(throwable.message ?: activity.getString(R.string.common_messages_apiError))
                        )
                    }
                }
            )
    }

    fun registerConfirm(confirmationToken: String, onError: ((messages: List<String>?) -> Unit)?=null, onComplete: (id: Int) -> Unit) {
        erikuraApiService.registerConfirm(ConfirmationTokenRequest(confirmationToken = confirmationToken))
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.hasError) {
                        activity.runOnUiThread {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(it.errors)
                        }
                    }
                    else {
                        val id = it.body.id
                        activity.runOnUiThread { onComplete(id) }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    activity.runOnUiThread {
                        (onError ?: { msgs -> displayErrorAlert(msgs) })(
                            listOf(throwable.message ?: activity.getString(R.string.common_messages_apiError))
                        )
                    }
                }
            )
    }

    fun postalCode(postalCode: String, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (prefecture: String?, city: String?, street: String?) -> Unit) {
        erikuraApiService.postalCode(postalCode)
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.hasError) {
                        activity.runOnUiThread {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(it.errors)
                        }
                    }
                    else {
                        val prefecture = it.body.prefecture
                        val city = it.body.city
                        val street = it.body.street
                        activity.runOnUiThread { onComplete(prefecture, city, street) }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    activity.runOnUiThread {
                        (onError ?: { msgs -> displayErrorAlert(msgs) })(
                            listOf(throwable.message ?: activity.getString(R.string.common_messages_apiError))
                        )
                    }
                }
            )
    }

    fun initialUpdateUser(user: User, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (session: UserSession) -> Unit)  {
        erikuraApiService.initialUpdateUser(user)
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.hasError) {
                        activity.runOnUiThread {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(it.errors)
                        }
                    }
                    else {
                        val session = UserSession(userId = it.body.userId, token = it.body.accessToken)
                        userSession = session
                        activity.runOnUiThread { onComplete(session) }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    activity.runOnUiThread {
                        (onError ?: { msgs -> displayErrorAlert(msgs) })(
                            listOf(throwable.message ?: activity.getString(R.string.common_messages_apiError))
                        )
                    }
                }
            )
    }

    fun reloadJob(job: Job, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (job: Job) -> Unit) {
        erikuraApiService.reloadJob(job.id)
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.hasError) {
                        activity.runOnUiThread {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(it.errors)
                        }
                    }
                    else {
                        val job = it.body
                        activity.runOnUiThread { onComplete(job) }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    activity.runOnUiThread {
                        (onError ?: { msgs -> displayErrorAlert(msgs) })(
                            listOf(throwable.message ?: activity.getString(R.string.common_messages_apiError))
                        )
                    }
                }
            )
    }

    fun place(placeId: Int, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (place: Place) -> Unit){
        erikuraApiService.place(placeId)
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.hasError) {
                        activity.runOnUiThread {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(it.errors)
                        }
                    }
                    else {
                        val place = it.body
                        activity.runOnUiThread { onComplete(place) }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    activity.runOnUiThread {
                        (onError ?: { msgs -> displayErrorAlert(msgs) })(
                            listOf(throwable.message ?: activity.getString(R.string.common_messages_apiError))
                        )
                    }
                }
            )
    }

    fun placeFavorite(placeId: Int, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (result: Boolean) -> Unit) {
        erikuraApiService.placeFavoriteCreate(FavoriteRequest(placeId))
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.hasError) {
                        activity.runOnUiThread {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(it.errors)
                        }
                    }
                    else {
                        val result = it.body
                        activity.runOnUiThread { onComplete(result) }
                    }
                },
                onError = { throwable ->

                    Log.v("ERROR", throwable.message, throwable)
                    activity.runOnUiThread {
                        (onError ?: { msgs -> displayErrorAlert(msgs) })(
                            listOf(throwable.message ?: activity.getString(R.string.common_messages_apiError))
                        )
                    }
                }
            )
    }

    fun placeFavoriteDelete(placeId: Int, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (result: Boolean) -> Unit) {
        erikuraApiService.placeFavoriteDelete(FavoriteRequest(placeId))
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.hasError) {
                        activity.runOnUiThread {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(it.errors)
                        }
                    }
                    else {
                        val result = it.body
                        activity.runOnUiThread { onComplete(result) }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    activity.runOnUiThread {
                        (onError ?: { msgs -> displayErrorAlert(msgs) })(
                            listOf(throwable.message ?: activity.getString(R.string.common_messages_apiError))
                        )
                    }
                }
            )
    }

    fun placeFavoriteShow(placeId: Int, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (result: Boolean) -> Unit) {
        erikuraApiService.placeFavoriteShow(placeId)
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.hasError) {
                        activity.runOnUiThread {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(it.errors)
                        }
                    }
                    else {
                        val result = it.body
                        activity.runOnUiThread { onComplete(result) }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    activity.runOnUiThread {
                        (onError ?: { msgs -> displayErrorAlert(msgs) })(
                            listOf(throwable.message ?: activity.getString(R.string.common_messages_apiError))
                        )
                    }
                }
            )
    }

    fun downloadResource(url: URL, destination: File, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (file: File) -> Unit) {
        // OkHttp3 クライアントを作成します
        val client = ErikuraApiServiceBuilder().httpBuilder.build()
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
            .subscribeBy(
                onNext = { response ->
                    if (response.isSuccessful) {
                        response.body?.let { body ->
                            destination.outputStream().use { os ->
                                IOUtils.copy(body.byteStream(), os)
                            }
                        }
                        activity.runOnUiThread{
                            onComplete(destination)
                        }
                    }
                    else {
                        activity.runOnUiThread {
                            onError?.let {
                                it(listOf("Download Error"))
                            }
                        }
                    }
                },
                onError = { e ->
                    Log.e("Download Error", e.message, e)
                    activity.runOnUiThread {
                        onError?.let {
                            it(listOf(e.message ?: "Download Error"))
                        }
                    }
                }
            )
    }

    fun geocode(keyword: String, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (file: LatLng) -> Unit) {
        googleMapApiService.geocode(BuildConfig.GEOCODING_API_KEY, keyword)
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = { response ->
                    when(response.status) {
                        GeocodingResponse.Status.OK -> {
                            if (response.results.size > 0) {
                                val location = response.results.first().geometry.location
                                activity.runOnUiThread {
                                    onComplete(LatLng(location.lat, location.lng))
                                }
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
                    activity.runOnUiThread {
                        (onError ?: { msgs -> displayErrorAlert(msgs) })(
                            listOf(throwable.message ?: "キーワードでの検索に失敗しました")
                        )
                    }
                }
            )
    }

    private fun <T> executeObservable(observable: Observable<Response<ApiResponse<T>>>, defaultError: String? = null, onError: ((messages: List<String>?) -> Unit)?, onComplete: (response: T) -> Unit) {
        val defaultError = defaultError ?: activity.getString(R.string.common_messages_apiError)
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
                            processError(apiResponse.errors ?: listOf(defaultError), onError)
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
                                Toast.makeText(activity, "401 Unauthorized", Toast.LENGTH_LONG).show()
                            }
                            500 -> {
                                Log.v("ERROR RESPONSE", response.errorBody().toString())
                                processError(listOf(defaultError), onError)
                            }
                            else -> {
                                Log.v("ERROR RESPONSE", response.errorBody().toString())
                                processError(listOf(defaultError), onError)
                            }
                        }
                    }
                },
                onError = { throwable ->
                    Log.v("ERROR", throwable.message, throwable)
                    processError(listOf(throwable.message ?: defaultError), onError)
                }
            )
    }

    private fun showProgressAlert() {
        if (progressAlert == null) {
            progressAlert = AlertDialog.Builder(activity).apply {
                setView(LayoutInflater.from(activity).inflate(R.layout.dialog_progress, null, false))
                setCancelable(false)
            }.create()


        }
        val dm = activity.resources.displayMetrics
        progressAlert?.show()
        progressAlert?.window?.setLayout(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100.0f, dm).toInt(),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100.0f, dm).toInt())
    }

    private fun hideProgressAlert() {
        progressAlert?.hide()
    }

    private fun processError(messages: List<String>, onError: ((messages: List<String>?) -> Unit)?) {
        (onError ?: { msgs -> displayErrorAlert(msgs) })(messages)
    }

    fun displayErrorAlert(messages: List<String>? = null, caption: String? = null) {
        activity.runOnUiThread {
            val alertDialog = AlertDialog.Builder(activity)
                .apply {
                    setTitle(caption ?: activity.getString(R.string.common_captions_apiError))
                    setMessage(
                        messages?.joinToString("\n") ?: activity.getString(R.string.common_messages_apiError)
                    )
                    setPositiveButton(R.string.common_buttons_close) { _, _ -> }
                }.create()
            alertDialog.show()
        }
    }
}
