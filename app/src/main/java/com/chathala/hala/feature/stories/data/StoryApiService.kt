package com.chathala.hala.feature.stories.data

import com.chathala.hala.feature.auth.data.SimpleResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** مسارات القصص على `api/mobile` (انظر MatchHalaApi/routes/mobile/stories.js). */
interface StoryApiService {

    @Multipart
    @POST("api/mobile/stories")
    suspend fun publishImage(
        @Header("Authorization") bearer: String,
        @Part image: MultipartBody.Part
    ): StoryEnvelope<StoryPublishData>

    @POST("api/mobile/stories/text")
    suspend fun publishText(
        @Header("Authorization") bearer: String,
        @Body body: TextStoryRequest
    ): StoryEnvelope<StoryPublishData>

    @GET("api/mobile/stories/me")
    suspend fun myStories(@Header("Authorization") bearer: String): StoryEnvelope<MyStoriesData>

    @GET("api/mobile/users/{userId}/stories")
    suspend fun userStories(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: String
    ): StoryEnvelope<UserStoriesData>

    @POST("api/mobile/stories/status")
    suspend fun statuses(
        @Header("Authorization") bearer: String,
        @Body body: StoryStatusRequest
    ): StoryEnvelope<StoryStatusesData>

    @POST("api/mobile/stories/{id}/view")
    suspend fun view(
        @Header("Authorization") bearer: String,
        @Path("id") storyId: String
    ): StoryEnvelope<StoryRecordedData>

    @POST("api/mobile/stories/{id}/screenshot")
    suspend fun screenshot(
        @Header("Authorization") bearer: String,
        @Path("id") storyId: String
    ): StoryEnvelope<StoryRecordedData>

    @GET("api/mobile/stories/{id}/viewers")
    suspend fun viewers(
        @Header("Authorization") bearer: String,
        @Path("id") storyId: String
    ): StoryEnvelope<StoryViewersData>

    @DELETE("api/mobile/stories/{id}")
    suspend fun delete(
        @Header("Authorization") bearer: String,
        @Path("id") storyId: String
    ): SimpleResponse

    @POST("api/mobile/stories/mute/{userId}")
    suspend fun mute(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: String
    ): StoryEnvelope<StoryMuteData>

    @DELETE("api/mobile/stories/mute/{userId}")
    suspend fun unmute(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: String
    ): StoryEnvelope<StoryMuteData>
}
