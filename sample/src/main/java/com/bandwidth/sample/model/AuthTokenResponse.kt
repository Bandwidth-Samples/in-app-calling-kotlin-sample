package com.bandwidth.sample.model

import com.google.gson.annotations.SerializedName

data class AuthTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("access_token_id") val accessTokenId: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: String
)
