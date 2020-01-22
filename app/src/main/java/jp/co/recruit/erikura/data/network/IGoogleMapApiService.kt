package jp.co.recruit.erikura.data.network

import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface IGoogleMapApiService {
    companion object {
        const val baseURL: String =  "https://maps.googleapis.com/maps/api/"
    }

    @GET("geocode/json")
    fun geocode(@Query("key") apiKey: String, @Query("address") keyword: String): Observable<GeocodingResponse>
}

data class GeocodingResponse(
    @SerializedName("status")
    val statusString: String,
    val results: List<Result>
) {
    enum class Status {
        OK(),
        ZERO_RESULTS(),
        OVER_DAILY_LIMIT(),
        OVER_QUERY_LIMIT(),
        REQUEST_DENIED(),
        INVALID_REQUEST(),
        UNKNOWN_ERROR()
    }

    val status: Status get() = Status.valueOf(statusString)

    data class Result(
        val addressComponents: List<AddressComponent>,
        val formattedAddress: String,
        val geometry: Geometry,
        val placeId: String,
        val plusCode: Map<String, String>,
        val types: List<String>
    )

    data class AddressComponent(
        val longName: String,
        val shortName: String,
        val types: List<String>
    )

    data class Geometry(
        val location: Location,
        val locationType: String,
        val viewport: Viewport
    )

    data class Viewport(
        val northeast: Location,
        val southwest: Location
    )

    data class Location(
        val lat: Double,
        val lng: Double
    )
}
