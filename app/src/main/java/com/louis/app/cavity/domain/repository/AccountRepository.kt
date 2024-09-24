package com.louis.app.cavity.domain.repository

import android.app.Application
import com.louis.app.cavity.R
import com.louis.app.cavity.domain.error.ErrorReporter
import com.louis.app.cavity.domain.error.SentryErrorReporter
import com.louis.app.cavity.model.*
import com.louis.app.cavity.network.CavityApiClient
import com.louis.app.cavity.network.CavityApiService
import com.louis.app.cavity.network.response.ApiResponse
import com.louis.app.cavity.network.response.LoginResponse
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit

class AccountRepository private constructor(private val app: Application) {
    companion object {
        @Volatile
        private var instance: AccountRepository? = null

        fun getInstance(app: Application) =
            instance ?: synchronized(this) {
                instance ?: AccountRepository(app).also { instance = it }
            }
    }

    private val errorReporter = SentryErrorReporter.getInstance(app)

    private val cavityApi by lazy {
        val locale = app.getString(R.string.locale)
        val prefsRepository = PrefsRepository.getInstance(app)

        CavityApiClient.buildRetrofitInstance(locale, prefsRepository)
            .also { retrofit = it }
            .create(CavityApiService::class.java)
    }

    private var retrofit: Retrofit? = null

    suspend fun login(email: String, password: String): ApiResponse<LoginResponse> {
        val parameters = mapOf("email" to email, "password" to password)
        return doApiCall { cavityApi.login(parameters) }
    }

    suspend fun register(email: String, password: String): ApiResponse<Unit> {
        val parameters = mapOf("email" to email, "password" to password)
        return doApiCall { cavityApi.register(parameters) }
    }

    suspend fun confirmAccount(
        email: String,
        registrationCode: String
    ): ApiResponse<LoginResponse> {
        val parameters = mapOf("email" to email, "registrationCode" to registrationCode)
        return doApiCall { cavityApi.confirmAccount(parameters) }
    }

    suspend fun deleteAccount(email: String, password: String): ApiResponse<Unit> {
        return doApiCall {
            cavityApi.deleteAccount(
                mapOf(
                    "email" to email,
                    "password" to password
                )
            )
        }
    }

    suspend fun postAccountLastUser(deviceName: String): ApiResponse<Unit> {
        return doApiCall { cavityApi.postAccountLastUser(mapOf("lastUser" to deviceName)) }
    }

    suspend fun recoverPassword(email: String): ApiResponse<Unit> {
        return doApiCall { cavityApi.recoverAccount(mapOf("email" to email)) }
    }

    suspend fun postCounties(counties: List<County>): ApiResponse<Unit> {
        return doApiCall { cavityApi.postCounties(counties) }
    }

    suspend fun postWines(wines: List<Wine>): ApiResponse<Unit> {
        return doApiCall { cavityApi.postWines(wines) }
    }

    suspend fun postBottles(bottles: List<Bottle>): ApiResponse<Unit> {
        return doApiCall { cavityApi.postBottles(bottles) }
    }

    suspend fun postFriends(friends: List<Friend>): ApiResponse<Unit> {
        return doApiCall { cavityApi.postFriends(friends) }
    }

    suspend fun postGrapes(grapes: List<Grape>): ApiResponse<Unit> {
        return doApiCall { cavityApi.postGrapes(grapes) }
    }

    suspend fun postReviews(reviews: List<Review>): ApiResponse<Unit> {
        return doApiCall { cavityApi.postReviews(reviews) }
    }

    suspend fun postHistoryEntries(entries: List<HistoryEntry>): ApiResponse<Unit> {
        return doApiCall { cavityApi.postHistoryEntries(entries) }
    }

    suspend fun postTastings(tastings: List<Tasting>): ApiResponse<Unit> {
        return doApiCall { cavityApi.postTastings(tastings) }
    }

    suspend fun postTastingActions(actions: List<TastingAction>): ApiResponse<Unit> {
        return doApiCall { cavityApi.postTastingActions(actions) }
    }

    suspend fun postFReviews(fReviews: List<FReview>): ApiResponse<Unit> {
        return doApiCall { cavityApi.postFReview(fReviews) }
    }

    suspend fun postQGrapes(qGrapes: List<QGrape>): ApiResponse<Unit> {
        return doApiCall { cavityApi.postQGrapes(qGrapes) }
    }

    suspend fun postTastingFriendsXRefs(tastingFriendXRefs: List<TastingXFriend>):
            ApiResponse<Unit> {
        return doApiCall { cavityApi.postTastingFriendsXRef(tastingFriendXRefs) }
    }

    suspend fun postHistoryFriendsXRefs(historyFriendXRefs: List<HistoryXFriend>):
            ApiResponse<Unit> {
        return doApiCall { cavityApi.postHistoryFriendsXRef(historyFriendXRefs) }
    }

    suspend fun getAccount(): ApiResponse<LoginResponse> {
        return doApiCall { cavityApi.getAccount() }
    }

    suspend fun getCounties(): ApiResponse<List<County>> {
        return doApiCall { cavityApi.getCounties() }
    }

    suspend fun getWines(): ApiResponse<List<Wine>> {
        return doApiCall { cavityApi.getWines() }
    }

    suspend fun getBottles(): ApiResponse<List<Bottle>> {
        return doApiCall { cavityApi.getBottles() }
    }

    suspend fun getFriends(): ApiResponse<List<Friend>> {
        return doApiCall { cavityApi.getFriends() }
    }

    suspend fun getGrapes(): ApiResponse<List<Grape>> {
        return doApiCall { cavityApi.getGrapes() }
    }

    suspend fun getReviews(): ApiResponse<List<Review>> {
        return doApiCall { cavityApi.getReviews() }
    }

    suspend fun getHistoryEntries(): ApiResponse<List<HistoryEntry>> {
        return doApiCall { cavityApi.getHistoryEntries() }
    }

    suspend fun getTastings(): ApiResponse<List<Tasting>> {
        return doApiCall { cavityApi.getTastings() }
    }

    suspend fun getTastingActions(): ApiResponse<List<TastingAction>> {
        return doApiCall { cavityApi.getTastingActions() }
    }

    suspend fun getFReviews(): ApiResponse<List<FReview>> {
        return doApiCall { cavityApi.getFReviews() }
    }

    suspend fun getQGrapes(): ApiResponse<List<QGrape>> {
        return doApiCall { cavityApi.getQGrapes() }
    }

    suspend fun getTastingXFriend(): ApiResponse<List<TastingXFriend>> {
        return doApiCall { cavityApi.getTastingFriendsXRef() }
    }

    suspend fun getHistoryXFriend(): ApiResponse<List<HistoryXFriend>> {
        return doApiCall { cavityApi.getHistoryFriendsXRef() }
    }

    suspend fun deleteCounties(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteCounties() }
    }

    suspend fun deleteWines(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteWines() }
    }

    suspend fun deleteBottles(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteBottles() }
    }

    suspend fun deleteFriends(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteFriends() }
    }

    suspend fun deleteGrapes(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteGrapes() }
    }

    suspend fun deleteReviews(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteReviews() }
    }

    suspend fun deleteHistoryEntries(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteHistoryEntries() }
    }

    suspend fun deleteTastings(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteTastings() }
    }

    suspend fun deleteTastingActions(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteTastingActions() }
    }

    suspend fun deleteFReviews(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteFReviews() }
    }

    suspend fun deleteQGrapes(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteQGrapes() }
    }

    suspend fun deleteTastingXFriend(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteTastingFriendsXRef() }
    }

    suspend fun deleteHistoryXFriend(): ApiResponse<Unit> {
        return doApiCall { cavityApi.deleteHistoryFriendsXRef() }
    }

    private suspend fun <T> doApiCall(apiCall: suspend () -> T): ApiResponse<T> {
        return try {
            ApiResponse.Success(apiCall.invoke())
        } catch (t: Throwable) {
            when (t) {
                is HttpException -> when (t.code()) {
                    401 -> ApiResponse.UnauthorizedError.also {
                        errorReporter.removeScopeTag(ErrorReporter.USERNAME_ERROR_TAG)
                    }

                    412 -> ApiResponse.UnregisteredError
                    else -> parseError(t.response())
                }

                else -> ApiResponse.UnknownError.also {
                    errorReporter.captureException(t)
                }
            }
        }
    }

    private fun parseError(response: Response<*>?): ApiResponse.Failure {
        return try {
            val converter: Converter<ResponseBody, ApiResponse.Failure>? = retrofit
                ?.responseBodyConverter(
                    ApiResponse.Failure::class.java,
                    arrayOfNulls<Annotation>(0)
                )

            val message = response?.errorBody()?.let {
                try {
                    converter?.convert(it)?.message
                } catch (e: Exception) {
                    null
                }
            } ?: app.getString(R.string.base_error)

            ApiResponse.Failure(message)
        } catch (e: IllegalArgumentException) {
            ApiResponse.Failure(app.getString(R.string.base_error))
        }
    }
}
