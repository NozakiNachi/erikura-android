package jp.co.recruit.erikura.data.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class ErikuraApiServiceBuilder {
    val apiBaseURL: String get() {
        return BuildConfig.SERVER_BASE_URL + "app/api/v1/"
    }

    val httpBuilder: OkHttpClient.Builder get() {
        return buildClient(true)
    }

    val httpBuilderForAWS: OkHttpClient.Builder get() {
        return buildClient(false)
    }

    constructor()

    private fun buildClient(isIconDownload: Boolean): OkHttpClient.Builder {
        return OkHttpClient.Builder().apply {
            // 独自ヘッダを追加します
            addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .method(original.method, original.body)
                requestBuilder
                    .header("Content-Type", "application/json")
                    .header("User-Agent-Version", "v${ErikuraApplication.versionName}")
                    .header("User-Agent-Type", "android")

                if (isIconDownload) {
                    Api.userSession?.let {
                        requestBuilder.header("Authorization", it.token)
                    }
                }
                val request = requestBuilder.build()
                return@Interceptor chain.proceed(request)
            })
            // リクエストのログを有効にします
            addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            // タイムアウトを設定します
            readTimeout(30, TimeUnit.SECONDS)
            connectTimeout(30, TimeUnit.SECONDS)
            // FIXME: キャッシュが無効になっていることを確認
            // FIXME: cookie が無効になっていることを書くに

            // テスト時に自己署名の証明書を受け付けるようにするには下記をコメントアウトする
//            setupAllTrustSSLSocketFactory(this)
        }
    }

    fun setupAllTrustSSLSocketFactory(builder: OkHttpClient.Builder) {
        val x509TrustManager = object: X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }

        val trustManagers = arrayOf<TrustManager>(x509TrustManager)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagers, null)

        builder.sslSocketFactory(sslContext.socketFactory, x509TrustManager)
        builder.hostnameVerifier(object: HostnameVerifier{
            override fun verify(hostname: String?, session: SSLSession?): Boolean {
                return true
            }
        })
    }

    fun create(): IErikuraApiService {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(ErikuraConfigMap::class.java, ErikuraConfigDeserializer())
            .serializeNulls()
            .create()

        val retrofit = Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(apiBaseURL)
            .client(httpBuilder.build())
            .build()
        return retrofit.create(IErikuraApiService::class.java)
    }
}
