package com.jasonhong.holocubic

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Response

class ChatGPTViewModel : ViewModel() {
    private val _chatGPTReply = MutableLiveData<String>()
    val chatGPTReply: LiveData<String> = _chatGPTReply

    // Replace "YOUR_API_KEY" with your actual API key
    private val chatGPTService = createRetrofitInstance(AccessKeys.OpenAI)

    fun sendMessageToChatGPT(inputText: String) {
        val messages = listOf(Message("system", "I am ChatGPT, how can I help you?"), Message("user", inputText))

        viewModelScope.launch {
            val response: Response<ChatGPTResponse> = chatGPTService.sendMessage(ChatGPTRequest("gpt-3.5-turbo", messages))
            if (response.isSuccessful) {
                response.body()?.let { chatGPTResponse ->
                    val chatGPTReply = chatGPTResponse.choices.firstOrNull()?.message?.content?.trim()
                    if (chatGPTReply != null) {
                        _chatGPTReply.postValue(chatGPTReply)
                    }
                }
            }
        }
    }
}
