package jp.co.recruit.erikura.data.network

import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
import jp.co.recruit.erikura.business.models.*
import okhttp3.RequestBody
import retrofit2.http.*
import java.util.*

interface IErikuraApiService {
    @POST("users")
    fun registerEmail(@Body request: RegisterEmailRequest): Observable<Response<IdResponse>>

    @POST("users/confirm")
    fun registerConfirm(@Body request: ConfirmationTokenRequest): Observable<Response<IdResponse>>

    @GET("users")
    fun user(): Observable<Response<User>>

    @PATCH("users/initial_update")
    fun initialUpdateUser(@Body request: User): Observable<Response<InitialUpdateResponse>>

    @PATCH("users")
    fun updateUser(@Body request: User): Observable<Response<IdResponse>>

    @POST("login")
    fun login(@Body request: LoginRequest): Observable<Response<LoginResponse>>

    @DELETE("logout")
    fun logout(): Observable<Response<ResultResponse>>

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
    ): Observable<Response<JobsResponse>>

    @GET("reports")
    fun reloadReport(@Query("job_id") jobId: Int): Observable<Response<Report>>

    @GET("jobs/own")
    fun ownJobs(
        @Query("status") status: String,
        @Query("reported_at_from") reportedAtFrom: Date? = null,
        @Query("reported_at_to") reportedAtTo: Date? = null
    ): Observable<Response<JobsResponse>>

    @GET("jobs/kind")
    fun jobKinds(): Observable<Response<List<JobKind>>>

    @GET("jobs/recommended")
    fun recommendedJobs(@Query("job_id") jobId: Int): Observable<Response<JobsResponse>>

    @POST("entries")
    fun entry(@Body request: EntryRequest): Observable<Response<EntryIdResponse>>

    @GET("entries/cancellation_reasons")
    fun cancelReasons(): Observable<Response<CancelReasonsResponse>>

    @DELETE("entires")
    fun cancel(@Body request: CancelRequest): Observable<Response<EntryIdResponse>>

    @POST("entries/start")
    fun startJob(@Body request: StartJobRequest): Observable<Response<EntryIdResponse>>

    @POST("entries/finish")
    fun stopJob(@Body request: StopJobRequest): Observable<Response<EntryIdResponse>>

    @PATCH("entries/abort")
    fun abortJob(@Body request: AbortJobRequest): Observable<Response<EntryIdResponse>>

    @GET("places/{placeId}")
    fun place(@Path("placeId") placeId: Int): Observable<Response<Place>>

    @PATCH("reports")
    fun report(@Body request: ReportRequest): Observable<Response<ReportIdResponse>>

    // FIXME: download => OKHTTP で直接ダウンロードするのがいいのか

    @POST("reorts/image_upload")
    fun imageUpload(@Part("photo") photo: RequestBody): Observable<Response<PhotoTokenResponse>>

    @DELETE("reports")
    fun deleteReport(@Body request: DeleteReportRequest): Observable<Response<ReportIdResponse>>

    // FIXME: geocoding

    @GET("informations")
    fun informations(): Observable<Response<InformationResponse>>

    @GET("users/notification")
    fun notificationSetting(): Observable<Response<NotificationSetting>>

    @PATCH("users/notification")
    fun updateNotificationSetting(@Body request: NotificationSetting): Observable<Response<UserIdResponse>>

    @GET("utils/address")
    fun postalCode(@Query("postal_code") postalCode: String): Observable<Response<PostalCodeResponse>>

    @GET("utils/bank_information")
    fun bank(@Query("keyword") keyword: String): Observable<Response<List<List<String>>>>

    @GET("utils/branch_information")
    fun branch(@Query("keyword") keyword: String, @Query("bank_no") bankNo: String): Observable<Response<List<List<String>>>>

    @GET("users/payments")
    fun payment(): Observable<Response<Payment>>

    @POST("users/payments")
    fun updatePayment(@Body request: Payment): Observable<Response<UserIdResponse>>

    @POST("users/push_endpoint")
    fun pushEndpoint(@Body request: PushEndpointRequest): Observable<Response<ResultResponse>>

    @GET("utils/client_version")
    fun clientVersion(): Observable<Response<RequiredClientVersion>>

    // FIXME: erikuraConfig
//    @GET("utils/erikura_config")
//    fun erikuraConfig(): Observable<Response<>>

//    def erikura_config
//    render_success({
//        reward_range: ErikuraConfig.reward_range.split(/\R/).map(&:to_i),
//        working_time_range: ErikuraConfig.working_time_range.split(/\R/).map(&:to_i)
//    })
}

data class Response<BODY>(
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