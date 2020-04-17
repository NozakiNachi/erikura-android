package jp.co.recruit.erikura.data.network

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import com.google.android.gms.maps.model.LatLng
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.*
import jp.co.recruit.erikura.presenters.activities.errors.LoginRequiredActivity
import jp.co.recruit.erikura.presenters.util.LocationManager
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.Request as HttpRequest
import okhttp3.Response as HttpResponse
import org.apache.commons.io.IOUtils
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.*


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
//
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

            this.user { user ->
                // ログインのトラッキングの送出
                Tracking.logEvent(event= "login", params= bundleOf())
                Tracking.identify(user= user, status= "login")
            }
        }
    }

    fun logout(onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (deletedSession: UserSession?) -> Unit) {
        executeObservable(
            erikuraApiService.logout(),
            onError =  onError
        ) { body ->
            val deletedSession = userSession
            if(body.accessToken == null){
                userSession = null
                // 保存してある認証情報をクリアします
                UserSession.clear()
            }else{
                // エラーメッセージを出す
            }
            onComplete(deletedSession)
        }
    }

    fun resignIn(email: String, password: String, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (session: UserSession) -> Unit) {
        executeObservable(
            erikuraApiService.login(LoginRequest(email = email, password = password)),
            onError =  onError
        ) { body ->
            val now = Date().time
            val resignTime = (now % (1000 * 60 * 60)) / (1000 * 60) +10
            val session = UserSession(userId = body.userId, token = body.accessToken, resignInExpiredAt = resignTime)
            userSession = session
            onComplete(session)
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

    fun notificationSetting(onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (notificationSetting: NotificationSetting) -> Unit) {
        executeObservable(
            erikuraApiService.notificationSetting(),
            onError = onError
        ) { notificationSetting ->
            onComplete(notificationSetting)
        }
    }

    fun updateNotificationSetting(notificationSetting: NotificationSetting, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: () -> Unit)  {
        executeObservable(
            erikuraApiService.updateNotificationSetting(notificationSetting),
            onError = onError
        ) { body ->
            onComplete()
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

    fun bank(bankName: String, showProgress: Boolean = true, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (List<Bank>) -> Unit) {
        executeObservable(erikuraApiService.bank(bankName), onError = onError, showProgress = showProgress) { body ->
            val banks = body.map {
                Bank(code = it.first(), name = it.last())
            }
            onComplete(banks)
        }
    }

    fun branch(branchOfficeName: String, bankNumber:String, showProgress: Boolean = true, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (branches: List<BankBranch>) -> Unit) {
        executeObservable(erikuraApiService.branch(branchOfficeName, bankNumber), showProgress = showProgress, onError =  onError) { body ->
            val branches = body.map {
                BankBranch(code = it.first(), name = it.last())
            }
            onComplete(branches)
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

    fun updateUser(user: User, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (id: Int) -> Unit)  {
        executeObservable(
            erikuraApiService.updateUser(user),
            onError = onError
        ) { body ->
            val id = body.id
            onComplete(id)
        }
    }

    fun updatePayment(payment: Payment, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (userId: Int) -> Unit)  {
        executeObservable(
            erikuraApiService.updatePayment(payment),
            onError = onError
        ) { body ->
            var userId = body.userId
            onComplete(userId)
        }
    }

    fun user(onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (user: User) -> Unit) {
        executeObservable(
            erikuraApiService.user(),
            onError = onError
        ) { user ->
            userSession?.let {
                it.user = user
            }
            onComplete(user)
        }
    }

    fun payment(onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (payment: Payment) -> Unit) {
        executeObservable(
            erikuraApiService.payment(),
            onError = onError
        ) { payment ->
            onComplete(payment)
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

    fun reloadReport(job: Job, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (report: Report) -> Unit) {
        executeObservable(
            erikuraApiService.reloadReport(job.id),
            onError = onError
        ) { reloadedReport ->
            onComplete(reloadedReport)
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

    fun startJob(job: Job, latLng: LatLng, steps: Int?, distance: Double?, floorAsc: Int?, floorDesc: Int?, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (entryId: Int) -> Unit){
        executeObservable(
            erikuraApiService.startJob(
                StartJobRequest(
                    jobId = job.id,
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    steps = steps,
                    distance = distance,
                    floorAsc = floorAsc,
                    floorDesc = floorDesc
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

    fun stopJob(job: Job, latLng: LatLng, steps: Int?, distance: Double?, floorAsc: Int?, floorDesc: Int?, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (entryId: Int) -> Unit){
        executeObservable(
            erikuraApiService.stopJob(
                StopJobRequest(
                    jobId = job.id,
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    steps = steps,
                    distance = distance,
                    floorAsc = floorAsc,
                    floorDesc = floorDesc
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
            val resultId = body.result
            onComplete(resultId)
        }
    }

    fun favoritePlaces(onError: ((message: List<String>?) -> Unit)? = null, onComplete: (result: List<Place>) -> Unit){
        executeObservable(
            erikuraApiService.favoritePlaces(),
            onError = onError
        ) { body ->
            val placeList = body.places
            onComplete(placeList)
        }
    }

    fun report(job: Job, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (reportId: Int) -> Unit) {
        job.report?.let { report ->
            val outputSummaries = report.outputSummaries.filter{ it.needsToSendAPI }.map { outputSummary ->
                OutputSummaryRequest(
                    outputSummary.id,
                    outputSummary.place?: "",
                    outputSummary.evaluation?: "",
                    outputSummary.latitude?: 0.0,
                    outputSummary.longitude?: 0.0,
                    outputSummary.photoTakedAt?: Date(),
                    outputSummary.comment?: "",
                    outputSummary.beforeCleaningPhotoToken?: "",
                    outputSummary.willDelete
                )
            }

            val params = ReportRequest(
                report.id,
                job.id,
                outputSummaries,
                report.workingMinute,
                report.additionalComment,
                report.additionalReportPhotoToken,
                report.evaluation?: "unanswered",
                report.comment,
                report.additionalReportPhotoWillDelete
            )

            if (report.id == null) {
                executeObservable(
                    erikuraApiService.createReport(params),
                    onError = onError
                ) { body ->
                    val reportId = body.reportId
                    onComplete(reportId)
                }
            }else {
                executeObservable(
                    erikuraApiService.updateReport(params),
                    onError = onError
                ) { body ->
                    val reportId = body.reportId
                    onComplete(reportId)
                }
            }
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

    fun imageUpload(item: MediaItem, bytes: ByteArray, onError: ((message: List<String>?) -> Unit)? = null, onComplete: (token: String) -> Unit){
        val photo = RequestBody.create(item.mimeType.toMediaTypeOrNull(), bytes)
        val requestBody: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart(
            "photo",
            "photo.jpg",
            photo
        ).build()

        executeObservable(
            erikuraApiService.imageUpload(requestBody),false,
            onError = onError
        ) { body ->
            onComplete(body.photoToken)
        }
    }

    fun downloadResource(url: URL, destination: File, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (file: File) -> Unit) {
        // OkHttp3 クライアントを作成します
        var client = ErikuraApiServiceBuilder().httpBuilder.build()
//        if(url.toString().equals(ErikuraApplication.instance.getString(R.string.jobDetails_manualImageURL))) {
//            client = ErikuraApiServiceBuilder().httpBuilderForAWS.build()
//        }
        val regex = Regex(ErikuraApplication.instance.getString(R.string.amazon_url))
        if(regex.containsMatchIn(url.toString())) {
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

    fun clientVersion(onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (clientVersion: RequiredClientVersion) -> Unit) {
        executeObservable(
            erikuraApiService.clientVersion(),
            onError = onError, showProgress = false
        ) { clientVersion ->
            onComplete(clientVersion)
        }
    }

    fun erikuraConfig(onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (map: ErikuraConfigMap) -> Unit) {
        executeObservable(erikuraApiService.erikuraConfig(), onError = onError, showProgress = false) { result ->
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

    fun informations(onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (informations: List<Information>) -> Unit) {
        executeObservable(erikuraApiService.informations(), onError = onError) { response ->
            onComplete(response.information)
        }
    }

    fun pushEndpoint(fcmToken: String, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (result: Boolean) -> Unit) {
        executeObservable(erikuraApiService.pushEndpoint(PushEndpointRequest(ios_fcm_token = fcmToken)), showProgress = false, onError = onError) { response ->
            onComplete(response.result)
        }
    }

    fun cancelAllRequests() {
        activeObservables.forEach { observable ->
            (observable as? Disposable)?.dispose()
        }
        activeObservables.clear()
    }

    private val activeObservables = mutableSetOf<Observable<*>>()

    private fun <T> executeObservable(observable: Observable<Response<ApiResponse<T>>>, showProgress: Boolean = true, defaultError: String? = null, onError: ((messages: List<String>?) -> Unit)?, onComplete: (response: T) -> Unit) {
        val defaultErrorMessage = defaultError ?: context.getString(R.string.common_messages_apiError)
        if (showProgress)
            showProgressAlert()

        val complete: () -> Unit = {
            activeObservables.remove(observable)
            if (showProgress)
                hideProgressAlert()
        }
        activeObservables.add(observable)
        observable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { response: Response<ApiResponse<T>> ->
                    complete.invoke()
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
                                // 元画面は表示できないはずなので閉じておきます
                                (context as? Activity)?.finish()
                                // ログイン必須画面に遷移させます
                                Intent(context, LoginRequiredActivity::class.java).let {
                                    it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    context.startActivity(it)
                                }
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
                    processError(listOf("通信エラーが発生しました。\n電波状況の良い状況で再度お試しください。"), onError)
                    complete.invoke()
                }
            )
    }

    fun showProgressAlert() {
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

    fun hideProgressAlert() {
        if (!isDestroyed()) {
            progressAlert?.dismiss()
        }
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
            if (!isDestroyed()) {
                // ページ参照のトラッキングの送出
                Tracking.logEvent(event= "error_modal", params= bundleOf())
                Tracking.track(name= "error_modal")

                alertDialog.show()
            }
        }
    }

    private fun isDestroyed(): Boolean {
        return (context as? Activity)?.isDestroyed ?: true
    }

    fun deleteReport(JobId: Int, onError: ((message: List<String>?) -> Unit)? = null, onComplete: () -> Unit) {
        executeObservable(
            erikuraApiService.deleteReport(DeleteReportRequest(JobId)),
            onError = onError
        ) { body ->
            val reportId = body.reportId
            onComplete()
        }
    }
}
