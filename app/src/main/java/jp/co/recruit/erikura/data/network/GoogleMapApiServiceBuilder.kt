package jp.co.recruit.erikura.data.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class GoogleMapApiServiceBuilder {
    val baseURL: String get() = IGoogleMapApiService.baseURL

    val httpBuilder: OkHttpClient.Builder get() {
        return OkHttpClient.Builder().apply {
            // 独自ヘッダを追加します
            addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .method(original.method, original.body)
                requestBuilder
                    .header("Content-Type", "application/json")
                val request = requestBuilder.build()
                return@Interceptor chain.proceed(request)
            })
            // リクエストのログを有効にします
            addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            // タイムアウトを設定します
            readTimeout(30, TimeUnit.SECONDS)
            connectTimeout(30, TimeUnit.SECONDS)
        }
    }

    fun create(): IGoogleMapApiService {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .serializeNulls()
            .create()

        val retrofit = Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(baseURL)
            .client(httpBuilder.build())
            .build()
        return retrofit.create(IGoogleMapApiService::class.java)
    }
}
