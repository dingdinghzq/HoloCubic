package com.jasonhong.holocubic

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class ChatGPTRequest(val model: String, val messages: List<Message>)

data class Message(val role: String, val content: String)

data class ChatGPTResponse(
    val id: String,
    val `object`: String,
    val created: Int,
    val model: String,
    val usage: Usage,
    val choices: List<Choice>
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class Choice(
    val message: Message,
    val finish_reason: String,
    val index: Int
)




interface ChatGPTService {
    @POST("v1/chat/completions")
    suspend fun sendMessage(@Body request: ChatGPTRequest): Response<ChatGPTResponse>
}

fun createRetrofitInstance(apiKey: String): ChatGPTService {
    val authInterceptor = Interceptor { chain ->
        val newRequest = chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        chain.proceed(newRequest)
    }

    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    return retrofit.create(ChatGPTService::class.java)
}
