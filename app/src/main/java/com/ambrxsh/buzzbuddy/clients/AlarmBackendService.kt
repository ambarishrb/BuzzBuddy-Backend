package com.ambrxsh.buzzbuddy.clients

import com.ambrxsh.buzzbuddy.dtos.AlarmDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AlarmBackendService {

    @GET("/api/alarms")
    suspend fun listAlarms(): List<AlarmDto>

    @POST("/api/alarms")
    suspend fun createAlarm(@Body alarm: AlarmDto): AlarmDto

    @PUT("/api/alarms/{id}")
    suspend fun updateAlarm(@Path("id") id: Int, @Body alarm: AlarmDto): AlarmDto

    @DELETE("/api/alarms/{id}")
    suspend fun deleteAlarm(@Path("id") id: Int): Response<Unit>
}
