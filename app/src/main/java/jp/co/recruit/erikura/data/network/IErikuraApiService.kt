package jp.co.recruit.erikura.data.network

import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
import jp.co.recruit.erikura.business.models.*
import okhttp3.RequestBody
import retrofit2.http.*
import java.util.*

interface IErikuraApiService {
    @POST("users")
    fun registerEmail(@Body request: RegisterEmailRequest): Observable<ApiResponse<IdResponse>>

    @POST("users/confirm")
    fun registerConfirm(@Body request: ConfirmationTokenRequest): Observable<ApiResponse<IdResponse>>

    @GET("users")
    fun user(): Observable<ApiResponse<User>>

    @PATCH("users/initial_update")
    fun initialUpdateUser(@Body request: User): Observable<ApiResponse<InitialUpdateResponse>>

    @PATCH("users")
    fun updateUser(@Body request: User): Observable<ApiResponse<IdResponse>>

    @POST("login")
    fun login(@Body request: LoginRequest): Observable<ApiResponse<LoginResponse>>

    @DELETE("logout")
    fun logout(): Observable<ApiResponse<ResultResponse>>

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
    ): Observable<ApiResponse<JobsResponse>>

    @GET("reports")
    fun reloadReport(@Query("job_id") jobId: Int): Observable<ApiResponse<Report>>

    @GET("jobs/{jobId}")
    fun reloadJob(@Path("jobId") jobId: Int): Observable<ApiResponse<Job>>

    @GET("jobs/own")
    fun ownJobs(
        @Query("status") status: String,
        @Query("reported_at_from") reportedAtFrom: Date? = null,
        @Query("reported_at_to") reportedAtTo: Date? = null
    ): Observable<ApiResponse<JobsResponse>>

    @GET("jobs/kind")
    fun jobKinds(): Observable<ApiResponse<List<JobKind>>>

    @GET("jobs/recommended")
    fun recommendedJobs(@Query("job_id") jobId: Int): Observable<ApiResponse<JobsResponse>>

    @POST("entries")
    fun entry(@Body request: EntryRequest): Observable<ApiResponse<EntryIdResponse>>

    @GET("entries/cancellation_reasons")
    fun cancelReasons(): Observable<ApiResponse<CancelReasonsResponse>>

    @DELETE("entires")
    fun cancel(@Body request: CancelRequest): Observable<ApiResponse<EntryIdResponse>>

    @POST("entries/start")
    fun startJob(@Body request: StartJobRequest): Observable<ApiResponse<EntryIdResponse>>

    @POST("entries/finish")
    fun stopJob(@Body request: StopJobRequest): Observable<ApiResponse<EntryIdResponse>>

    @PATCH("entries/abort")
    fun abortJob(@Body request: AbortJobRequest): Observable<ApiResponse<EntryIdResponse>>

    @GET("places/{placeId}")
    fun place(@Path("placeId") placeId: Int): Observable<ApiResponse<Place>>

    @GET("place_favorites/show")
    fun placeFavoriteShow(@Query("place_id") placeId: Int): Observable<ApiResponse<Boolean>>

    @POST("place_favorites")
    fun placeFavoriteCreate(@Body request: FavoriteRequest): Observable<ApiResponse<Boolean>>

    @DELETE("place_favorites/destory")
    fun placeFavoriteDelete(@Body request: FavoriteRequest): Observable<ApiResponse<Boolean>>

    @PATCH("reports")
    fun report(@Body request: ReportRequest): Observable<ApiResponse<ReportIdResponse>>

    // FIXME: download => OKHTTP で直接ダウンロードするのがいいのか

    @POST("reorts/image_upload")
    fun imageUpload(@Part("photo") photo: RequestBody): Observable<ApiResponse<PhotoTokenResponse>>

    @DELETE("reports")
    fun deleteReport(@Body request: DeleteReportRequest): Observable<ApiResponse<ReportIdResponse>>

    // FIXME: geocoding

    @GET("informations")
    fun informations(): Observable<ApiResponse<InformationResponse>>

    @GET("users/notification")
    fun notificationSetting(): Observable<ApiResponse<NotificationSetting>>

    @PATCH("users/notification")
    fun updateNotificationSetting(@Body request: NotificationSetting): Observable<ApiResponse<UserIdResponse>>

    @GET("utils/address")
    fun postalCode(@Query("postal_code") postalCode: String): Observable<ApiResponse<PostalCodeResponse>>

    @GET("utils/bank_information")
    fun bank(@Query("keyword") keyword: String): Observable<ApiResponse<List<List<String>>>>

    @GET("utils/branch_information")
    fun branch(@Query("keyword") keyword: String, @Query("bank_no") bankNo: String): Observable<ApiResponse<List<List<String>>>>

    @GET("users/payments")
    fun payment(): Observable<ApiResponse<Payment>>

    @POST("users/payments")
    fun updatePayment(@Body request: Payment): Observable<ApiResponse<UserIdResponse>>

    @POST("users/push_endpoint")
    fun pushEndpoint(@Body request: PushEndpointRequest): Observable<ApiResponse<ResultResponse>>

    @GET("utils/client_version")
    fun clientVersion(): Observable<ApiResponse<RequiredClientVersion>>

    // FIXME: erikuraConfig
//    @GET("utils/erikura_config")
//    fun erikuraConfig(): Observable<Response<>>

//    def erikura_config
//    render_success({
//        reward_range: ErikuraConfig.reward_range.split(/\R/).map(&:to_i),
//        working_time_range: ErikuraConfig.working_time_range.split(/\R/).map(&:to_i)
//    })
}

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
    var reasonCode: Int
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
    var distance: Int,
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