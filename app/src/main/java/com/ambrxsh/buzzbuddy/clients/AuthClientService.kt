package com.ambrxsh.buzzbuddy.clients

import com.ambrxsh.buzzbuddy.dtos.ChangePasswordRequestDto
import com.ambrxsh.buzzbuddy.dtos.LoginRequestDto
import com.ambrxsh.buzzbuddy.dtos.LoginResponseDto
import com.ambrxsh.buzzbuddy.dtos.LogoutRequestDto
import com.ambrxsh.buzzbuddy.dtos.OkResponse
import com.ambrxsh.buzzbuddy.dtos.PasswordResetConfirmDto
import com.ambrxsh.buzzbuddy.dtos.PasswordResetRequestDto
import com.ambrxsh.buzzbuddy.dtos.RefreshRequestDto
import com.ambrxsh.buzzbuddy.dtos.RegisterRequestDto
import com.ambrxsh.buzzbuddy.dtos.RegisterResponseDto
import com.ambrxsh.buzzbuddy.dtos.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthClientService {

    @POST("/api/login")
    suspend fun login(@Body loginReq: LoginRequestDto): LoginResponseDto

    @POST("/api/register")
    suspend fun register(@Body registerReq: RegisterRequestDto): RegisterResponseDto

    @POST("/api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): LoginResponseDto

    @POST("/api/auth/logout")
    suspend fun logout(@Body body: LogoutRequestDto): Response<Unit>

    @POST("/api/auth/password-reset/request")
    suspend fun requestPasswordReset(@Body body: PasswordResetRequestDto): OkResponse

    @POST("/api/auth/password-reset/confirm")
    suspend fun confirmPasswordReset(@Body body: PasswordResetConfirmDto): OkResponse

    @GET("/api/account/me")
    suspend fun me(): UserDto

    @PUT("/api/account/password")
    suspend fun changePassword(@Body body: ChangePasswordRequestDto): OkResponse

    @DELETE("/api/account")
    suspend fun deleteAccount(): Response<Unit>
}
