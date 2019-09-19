package com.raywenderlich.emitron.data.video

import com.raywenderlich.emitron.model.Content
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Service to make API calls for videos
 */
interface VideoApi {

  /**
   * Get video stream data
   */
  @GET("videos/{id}/stream")
  suspend fun getVideoStream(
    @Path("id") id: String
  ): Content

  companion object {

    /**
     * Factory function for [VideoApi]
     */
    fun create(retroFit: Retrofit): VideoApi = retroFit.create(
      VideoApi::class.java
    )
  }
}
