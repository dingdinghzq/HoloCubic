package com.jasonhong.holocubic

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Response

const val MAX_MESSAGES = 10
class ChatGPTViewModel : ViewModel() {
    private val _chatGPTReply = MutableLiveData<String>()
    val chatGPTReply: LiveData<String> = _chatGPTReply

    // Replace "YOUR_API_KEY" with your actual API key
    private val chatGPTService = createRetrofitInstance(AccessKeys.OpenAI)
    private val conversationHistory = ArrayDeque<Message>()
    private val staticHead = listOf<Message>(Message("system", "I am ChatGPT, how can I help you?"),
        Message("user", "Respond with concise messages, no more than 100 words. If the intent is to end the chat, just respond '<STOP>' "))


    private fun addMessageToConversation(role: String, content: String) {
        if (conversationHistory.size >= MAX_MESSAGES) {
            conversationHistory.removeFirst()
        }
        conversationHistory.add(Message(role, content))
    }


    fun sendMessageToChatGPT(inputText: String) {
        Log.i("ChatGPT", "SendMessage::$inputText")
        addMessageToConversation("user", inputText)

        viewModelScope.launch {
            val response: Response<ChatGPTResponse> = chatGPTService.sendMessage(
                ChatGPTRequest("gpt-3.5-turbo", staticHead+conversationHistory))
            if (response.isSuccessful) {
                response.body()?.let { chatGPTResponse ->
                    val chatGPTReply = chatGPTResponse.choices.firstOrNull()?.message?.content?.trim()
                    if (chatGPTReply != null) {
                        addMessageToConversation("assistant", chatGPTReply)
                        Log.i("ChatGPT", "ReceiveResponse::$chatGPTReply")
                        if (chatGPTReply == "<STOP>")
                        {
                            conversationHistory.clear()
                        }
                        _chatGPTReply.postValue(chatGPTReply!!)
                    }
                }
            }
        }
    }
}
