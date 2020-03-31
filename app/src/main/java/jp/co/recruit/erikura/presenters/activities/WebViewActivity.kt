package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import jp.co.recruit.erikura.R


class WebViewActivity : BaseActivity() {
    private val webView: WebView get() = findViewById(R.id.webview_webview)
    private val progressBar: ProgressBar get() = findViewById(R.id.webview_progress)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        if (savedInstanceState == null) {
            // Intent から URL を受け取っている
            val url = intent.dataString
            Log.d("WebView: ", "URL=${url}")

            webView.requestFocus()
            webView.settings.apply {
                // javascript を有効化
                javaScriptEnabled = true
                // ズームを可能にします
                builtInZoomControls = true
            }
            webView.webViewClient = object: WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    progressBar.visibility = View.VISIBLE
                    super.onPageStarted(view, url, favicon)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar.visibility = View.INVISIBLE
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url
                    val scheme = url?.scheme
                    // http, https の場合はデフォルトの動作を実行します
                    if (scheme == "http" || scheme == "https" || scheme == null) {
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                    else {
                        // 上記以外の場合は、外部Activityを開きます
                        Intent(Intent.ACTION_VIEW).apply {
                            data = request?.url
                            startActivity(this, ActivityOptions.makeSceneTransitionAnimation(this@WebViewActivity).toBundle())
                        }
                        return true
                    }
                }
            }
            // PDFリンクでダウンロードされた場合に外部Activityを表示します
            webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                val i = Intent(Intent.ACTION_QUICK_VIEW).apply {
                    data = Uri.parse(url)
                }
                startActivity(i, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
            webView.loadUrl(url)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
