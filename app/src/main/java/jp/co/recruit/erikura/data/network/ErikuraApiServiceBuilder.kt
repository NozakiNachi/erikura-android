package jp.co.recruit.erikura.data.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ErikuraApiServiceBuilder {
    val apiBaseURL: String get() {
        return BuildConfig.SERVER_BASE_URL + "api/v1/"
    }

    val httpBuilder: OkHttpClient.Builder get() {
        return OkHttpClient.Builder().apply {
            // 独自ヘッダを追加します
            addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .method(original.method, original.body)
                // FIXME: version の取得方法を修正
                requestBuilder
                    .header("Content-Type", "application/json")
                    .header("User-Agent-Version", "v0.0.0.1")
                    .header("User-Agent-Type", "android")

                Api.userSession?.let {
                    requestBuilder.header("Authorization", it.token)
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
        }
    }

    fun create(): IErikuraApiService {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
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
