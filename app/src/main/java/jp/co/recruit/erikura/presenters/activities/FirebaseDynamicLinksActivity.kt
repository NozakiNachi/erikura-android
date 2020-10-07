package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.registration.RegisterPasswordActivity

class FirebaseDynamicLinksActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                var deepLink: Uri? = null
                if (pendingDynamicLinkData != null) {
                    deepLink = pendingDynamicLinkData.link
                }

                var intent = Intent(this, StartActivity::class.java)
                if (deepLink != null) {
                    if (deepLink.toString().contains("jobs/")) {
                        // 案件詳細画面へ遷移します
                        val job = Job(id= deepLink.lastPathSegment?.toInt() ?: 0)
                        job.uninitialized = true
                        intent = Intent(this, JobDetailsActivity::class.java)
                        intent.putExtra("job", job)
                        intent.putExtra("jobRestored", false)
                    }else {
                        // 本登録画面へ遷移します
                        intent = Intent(this, RegisterPasswordActivity::class.java)
                        intent.data = deepLink
                    }
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener(this) { e ->
                Log.w("JobDetailsActivity onCreate", "getDynamicLink:onFailure", e)
            }
    }
}