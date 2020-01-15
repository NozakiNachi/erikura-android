package jp.co.recruit.erikura.data.network

import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.presenters.activities.SendEmailEventHandlers
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.net.URL

class Api(var activity: AppCompatActivity) {
    companion object {
        var userSession: UserSession? = null

        val erikuraApiService: IErikuraApiService get() {
            return ErikuraApplication.instance.erikuraComponent.erikuraApiService()
        }

        val isLogin: Boolean get() = (userSession != null)
    }

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
        erikuraApiService.searchJob(
            period = query.period.value,
            latitude = query.latitude ?: MapViewActivity.defaultLatLng.latitude,
            longitude = query.longitude ?: MapViewActivity.defaultLatLng.longitude,
            sortBy = query.sortBy.value,
            minimumReward = query.minimumReward,
            maximumReward = query.maximumReward,
            minimumWorkingTime = query.minimumWorkingTime,
            maximumWorkingTime = query.maximumWorkingTime,
            jobKind = query.jobKind?.id)
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    if (it.hasError) {
                        activity.runOnUiThread {
                            (onError ?: { msgs -> displayErrorAlert(msgs) })(it.errors)
                        }
                    }
                    else {
                        val jobs = it.body.jobs
                        activity.runOnUiThread {
                            onComplete(jobs)
                        }
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
                        val street = it.body.prefecture
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

    fun downloadResource(url: URL, destination: File, onError: ((messages: List<String>?) -> Unit)? = null, onComplete: (file: File) -> Unit) {
        // OkHttp3 クライアントを作成します
        val client = ErikuraApiServiceBuilder().httpBuilder.build()
        val observable: Observable<Response> = Observable.create {
            try {
                val request = Request.Builder().url(url).get().build()
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
