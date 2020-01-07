package jp.co.recruit.erikura.data.network

import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.presenters.activities.SendEmailEventHandlers

class Api(var activity: AppCompatActivity) {
    companion object {
        var userSession: UserSession? = null

        val erikuraApiService: IErikuraApiService get() {
            return ErikuraApplication.instance.erikuraComponent.erikuraApiService()
        }
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
