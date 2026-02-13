package mivs.liturgicalcalendar.remote

import androidx.annotation.Keep
import mivs.liturgicalcalendar.remote.NewsResponse
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @Keep
    @GET("news")
    suspend fun getNewsFeed(): Response<List<NewsResponse>>
}