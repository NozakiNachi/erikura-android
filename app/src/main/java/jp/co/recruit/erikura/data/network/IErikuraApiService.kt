package jp.co.recruit.erikura.data.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
import jp.co.recruit.erikura.business.models.*
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.HashMap

interface IErikuraApiService {
    @POST("users")
    fun registerEmail(@Body request: RegisterEmailRequest): ApiObservable<IdResponse>

    @POST("users/confirm")
    fun registerConfirm(@Body request: ConfirmationTokenRequest): ApiObservable<IdResponse>

    @GET("users")
    fun user(): ApiObservable<User>

    @PATCH("users/initial_update")
    fun initialUpdateUser(@Body request: User): ApiObservable<InitialUpdateResponse>

    @PATCH("users")
    fun updateUser(@Body request: User): ApiObservable<IdResponse>

    @POST("login")
    fun login(@Body request: LoginRequest): ApiObservable<LoginResponse>

    @DELETE("logout")
    fun logout(): ApiObservable<LogoutResponse>

    @GET("jobs")
    fun searchJob(
        @Query("period") period: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("sort_by") sortBy: String,
        @Query("minimumWorkingTime") minimumWorkingTime: Int? = null,
        @Query("maximumWorkingTime") maximumWorkingTime: Int? = null,
        @Query("minimumReward") minimumReward: Int? = null,
        @Query("maximumReward") maximumReward: Int? = null,
        @Query("job_kind") jobKind: Int? = null
    ): ApiObservable<JobsResponse>

    @GET("reports")
    fun reloadReport(@Query("job_id") jobId: Int): ApiObservable<Report>

    @GET("jobs/{jobId}")
    fun reloadJob(@Path("jobId") jobId: Int): ApiObservable<Job>

    @GET("jobs/own")
    fun ownJobs(
        @Query("status") status: String,
        @Query("reported_at_from") reportedAtFrom: Date? = null,
        @Query("reported_at_to") reportedAtTo: Date? = null
    ): ApiObservable<JobsResponse>

    @GET("jobs/kind")
    fun jobKinds(): ApiObservable<List<JobKind>>

    @GET("jobs/recommend")
    fun recommendedJobs(@Query("job_id") jobId: Int): ApiObservable<JobsResponse>

    @POST("entries")
    fun entry(@Body request: EntryRequest): ApiObservable<EntryIdResponse>

    @GET("entries/cancellation_reasons")
    fun cancelReasons(): ApiObservable<CancelReasonsResponse>

    @HTTP(method = "DELETE", path = "entries", hasBody = true)
    fun cancel(@Body request: CancelRequest): ApiObservable<EntryIdResponse>

    @POST("entries/start")
    fun startJob(@Body request: StartJobRequest): ApiObservable<EntryIdResponse>

    @POST("entries/finish")
    fun stopJob(@Body request: StopJobRequest): ApiObservable<EntryIdResponse>

    @PATCH("entries/abort")
    fun abortJob(@Body request: AbortJobRequest): ApiObservable<EntryIdResponse>

    @GET("places/{placeId}")
    fun place(@Path("placeId") placeId: Int): ApiObservable<Place>

    @GET("place_favorites/show")
    fun placeFavoriteShow(@Query("place_id") placeId: Int): ApiObservable<ResultResponse>

    @POST("place_favorites")
    fun placeFavoriteCreate(@Body request: FavoriteRequest): ApiObservable<ResultResponse>

    @HTTP(method = "DELETE", path = "place_favorites/destroy", hasBody = true)
    fun placeFavoriteDelete(@Body request: FavoriteRequest): ApiObservable<ResultResponse>

    @PATCH("reports")
    fun report(@Body request: ReportRequest): ApiObservable<ReportIdResponse>

    // FIXME: download => OKHTTP で直接ダウンロードするのがいいのか

    @POST("reorts/image_upload")
    fun imageUpload(@Part("photo") photo: RequestBody): ApiObservable<PhotoTokenResponse>

    @HTTP(method = "DELETE", path = "reports", hasBody = true)
    fun deleteReport(@Body request: DeleteReportRequest): ApiObservable<ReportIdResponse>

    // FIXME: geocoding

    @GET("informations")
    fun informations(): ApiObservable<InformationResponse>

    @GET("users/notification")
    fun notificationSetting(): ApiObservable<NotificationSetting>

    @PATCH("users/notification")
    fun updateNotificationSetting(@Body request: NotificationSetting): ApiObservable<UserIdResponse>

    @GET("utils/address")
    fun postalCode(@Query("postal_code") postalCode: String): ApiObservable<PostalCodeResponse>

    @GET("utils/bank_information")
    fun bank(@Query("keyword") keyword: String): ApiObservable<List<List<String>>>

    @GET("utils/branch_information")
    fun branch(@Query("keyword") keyword: String, @Query("bank_no") bankNo: String): ApiObservable<List<List<String>>>

    @GET("users/payments")
    fun payment(): ApiObservable<Payment>

    @POST("users/payments")
    fun updatePayment(@Body request: Payment): ApiObservable<UserIdResponse>

    @POST("users/push_endpoint")
    fun pushEndpoint(@Body request: PushEndpointRequest): ApiObservable<ResultResponse>

    @GET("utils/client_version")
    fun clientVersion(): ApiObservable<RequiredClientVersion>

    // FIXME: erikuraConfig
    @GET("utils/erikura_config")
    fun erikuraConfig(): ApiObservable<ErikuraConfigMap>

//    def erikura_config
//    render_success({
//        reward_range: ErikuraConfig.reward_range.split(/\R/).map(&:to_i),
//        working_time_range: ErikuraConfig.working_time_range.split(/\R/).map(&:to_i)
//    })
}

typealias ApiObservable<T> = Observable<Response<ApiResponse<T>>>

data class ApiResponse<BODY>(
    var body: BODY,
    var errors: List<String>? = null
) {
    // エラーが存在するかを判定します
    val hasError: Boolean get() = !(this.errors?.isEmpty() ?: true)
}

data class IdResponse(
    var id: Int
)

data class InitialUpdateResponse(
    @SerializedName("id")
    var userId: Int,
    var accessToken: String
)

data class LoginResponse(
    var userId: Int,
    var accessToken: String
)

data class LogoutResponse(
    var userId: Int,
    var accessToken: String
)

data class ResultResponse(
    var result: Boolean
)

data class JobsResponse(
    var jobs: List<Job>
)

data class EntryIdResponse(
    var entryId: Int
)

data class CancelReasonsResponse(
    var cancellationReasons: List<CancelReason>
)

data class ReportIdResponse(
    var reportId: Int
)

data class PhotoTokenResponse(
    var photoToken: String
)

data class InformationResponse(
    var information: List<Information>
)

data class UserIdResponse(
    var userId: Int
)

data class PostalCodeResponse(
    var prefecture: String?,
    var city: String?,
    var street: String?
)

data class RegisterEmailRequest(
    var email: String
)

data class ConfirmationTokenRequest(
    var confirmationToken: String
)

data class LoginRequest(
    var email: String,
    var password: String
)

data class EntryRequest(
    var jobId: Int,
    var comment: String?
)

data class CancelRequest(
    var jobId: Int,
    var reasonCode: Int,
    var otherReason: String
)

data class StartJobRequest(
    var jobId: Int,
    var latitude: Double,
    var longitude: Double,
    var steps: Int,
    var distance: Int,
    var floorAsc: Int,
    var floorDesc: Int
)

data class StopJobRequest(
    var jobId: Int,
    var latitude: Double,
    var longitude: Double,
    var steps: Int,
    var distance: Double,
    var floorAsc: Int,
    var floorDesc: Int
)

data class AbortJobRequest(
    var jobId: Int,
    var entryId: Int
)

data class ReportRequest(
    var jobId: Int,
    var outputSummariesAttributes: List<OutputSummaryRequest>,
    var workingMinutes: Int?,
    var additionalComment: String?,
    var additionReportPhotoToken: String?,
    var evaluation: String,
    var comment: String?
)

data class OutputSummaryRequest(
    var id: Int?,
    var place: String,
    var evaluation: String,
    var latitude: Double,
    var longitude: Double,
    var photoTakedAt: Date,
    var comment: String,
    var beforeCleaningPhotoToken: String,
    var willDelete: Boolean
)

data class DeleteReportRequest(
    var jobId: Int
)

data class PushEndpointRequest(
    var ios_fcm_token: String
)

data class FavoriteRequest(
    var placeId: Int
)

sealed class ErikuraConfigValue {
    data class DoubleList(val values: List<Double>): ErikuraConfigValue()
}

class ErikuraConfigMap: HashMap<String, ErikuraConfigValue>()

class ErikuraConfigDeserializer: JsonDeserializer<ErikuraConfigMap> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ErikuraConfigMap {
        val jsonObject = json?.asJsonObject
        val result = ErikuraConfigMap()

        jsonObject?.entrySet()?.forEach { entry ->
            val deserializerMap: Map<String, (JsonElement?) -> ErikuraConfigValue> = mapOf(
                Pair(ErikuraConfig.REWARD_RANGE_KEY, { json -> deserializeDoubleList(json, context) }),
                Pair(ErikuraConfig.WORKING_TIME_RANGE_KEY, { json -> deserializeDoubleList(json, context) })
            )
            deserializerMap[entry.key]?.let { deserializer ->
                result.put(entry.key, deserializer(entry.value))
            }
        }
        return result
    }

    private fun deserializeDoubleList(json: JsonElement?, context: JsonDeserializationContext?): ErikuraConfigValue.DoubleList {
        return ErikuraConfigValue.DoubleList(
            context?.deserialize(json, List::class.java) ?: listOf()
        )
    }
}
