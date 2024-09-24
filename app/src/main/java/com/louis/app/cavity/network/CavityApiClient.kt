package com.louis.app.cavity.network

import com.louis.app.cavity.BuildConfig
import com.louis.app.cavity.domain.repository.PrefsRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object CavityApiClient {
    private const val API_URL = "https://cavity.fr"
    // hostname -I | awk '{print $1}'  -> to get backend ip.
    // Update .../debug/res/xml/network_security_config.xml accordingly
    private const val DEV_API_URL = "http://192.168.1.13:5000"

    private val moshiConverter: MoshiConverterFactory by lazy {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        MoshiConverterFactory.create(moshi)
    }

    fun buildRetrofitInstance(locale: String, prefsRepository: PrefsRepository): Retrofit {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor {
                val token = prefsRepository.getApiToken()
                val request = it.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Accept-Language", locale)
                    .build()

                it.proceed(request)
            }
            .build()

        val apiUrl = if (BuildConfig.DEBUG) DEV_API_URL else API_URL

        return Retrofit.Builder()
            .baseUrl(apiUrl)
            .client(httpClient)
            .addConverterFactory(moshiConverter.withNullSerialization())
            .build()
    }
}
