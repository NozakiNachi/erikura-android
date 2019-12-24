package jp.co.recruit.erikura.presenters.activities

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
    }
}
