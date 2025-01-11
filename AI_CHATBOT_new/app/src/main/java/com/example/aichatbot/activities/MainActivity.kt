package com.example.aichatbot.activities

import com.example.aichatbot.adapters.ChatAdapter
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.ClipboardManager
import android.util.Log
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aichatbot.database.ChatDb
import com.example.aichatbot.database.ChatTable
import com.example.aichatbot.R
import com.example.aichatbot.database.ChatDao
import com.example.aichatbot.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import com.example.aichatbot.utils.DataProvider.isInternetConnected
import com.example.aichatbot.utils.DataProvider.showMessage
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var chatDao: ChatDao

    private lateinit var database: ChatDb
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var sendButton: ImageView
    private lateinit var pasteButton: ImageView
    private lateinit var inputMessage: EditText
    private lateinit var chatbotSection: ConstraintLayout
    private lateinit var clearChatButton: Button

    private val apiKey = "Your API Key here"
    private val apiUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        clearChatButton = findViewById(R.id.clearChatButton)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        sendButton = findViewById(R.id.sendButton)
        pasteButton = findViewById(R.id.PasteButton)
        inputMessage = findViewById(R.id.inputMessage)
        chatbotSection = findViewById(R.id.chatbotSection)

        database = ChatDb.getDatabase(this)
        chatDao = database.chatDao()

        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        val recyclerView = binding.chatRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = chatAdapter
        lifecycleScope.launch {
            chatDao.getAllMessagesAsFlow().collect { chatList ->
                val sortedList = chatList.sortedBy { it.timestamp }
                chatAdapter.submitList(sortedList) {
                    if (sortedList.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        clearChatButton.visibility = View.GONE
                        binding.chatbotSection.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        clearChatButton.visibility = View.VISIBLE
                        binding.chatbotSection.visibility = View.GONE
                        recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                    }
                }
            }
        }
        chatAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        })
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            if (isInternetConnected(this)) {
                val promptMessage = binding.inputMessage.text.toString()
                if (promptMessage.isNotEmpty()) {
                    val userMessage = ChatTable(
                        message = promptMessage,
                        isSender = true,
                        timestamp = System.currentTimeMillis()
                    )
                    lifecycleScope.launch {
                        insertUserChat(userMessage)
                    }
                    sendRequestToGemini(promptMessage)
                    binding.inputMessage.text.clear()

                } else {
                    showMessage(this, "Please enter a message")
                }

            } else {
                showMessage(this, "Internet Not Connected")
            }

        }

        pasteButton.setOnClickListener {
            try {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val pastedText = clip.getItemAt(0).text.toString()
                    inputMessage.setText(pastedText)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to paste text", Toast.LENGTH_SHORT).show()
            }
        }

        clearChatButton.setOnClickListener {
            lifecycleScope.launch {
                clearChatHistory()
            }
        }
    }

    private suspend fun clearChatHistory() {
        chatDao.clearMessages()
        chatAdapter.submitList(emptyList())
        chatRecyclerView.visibility = View.GONE
        clearChatButton.visibility = View.GONE
        chatbotSection.visibility = View.VISIBLE
        Toast.makeText(this, "Chat cleared!", Toast.LENGTH_SHORT).show()
    }

    private fun sendRequestToGemini(prompt: String) {
        binding.sendButton.visibility = View.GONE
        binding.typingSection.visibility = View.VISIBLE
        val jsonObject = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }
        val jsonMediaType = "application/json".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    val errorMessage = "Model is Overloaded, Request failed, try again later!"
                    val modelMessage = ChatTable(
                        message = errorMessage,
                        isSender = false,
                        timestamp = System.currentTimeMillis()
                    )
                    lifecycleScope.launch {
                        insertUserChat(modelMessage)
                        binding.sendButton.visibility = View.VISIBLE
                        binding.typingSection.visibility = View.GONE
                    }
                    Log.e("API_ERROR", "Request failed", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("API_RESPONSE", "Response: $responseBody")
                if (response.isSuccessful && responseBody != null) {
                    runOnUiThread {
                        val modelResponse = parseResponse(responseBody)
                        val modelMessage = ChatTable(
                            message = modelResponse,
                            isSender = false,
                            timestamp = System.currentTimeMillis()
                        )
                        lifecycleScope.launch {
                            insertUserChat(modelMessage)
                            binding.sendButton.visibility = View.VISIBLE
                            binding.typingSection.visibility = View.GONE
                        }
                    }
                } else {
                    runOnUiThread {
                        val errorMessage = "Model is Overloaded, try again later!"
                        val modelMessage = ChatTable(
                            message = errorMessage,
                            isSender = false,
                            timestamp = System.currentTimeMillis()
                        )
                        lifecycleScope.launch {
                            insertUserChat(modelMessage)
                            binding.sendButton.visibility = View.VISIBLE
                            binding.typingSection.visibility = View.GONE
                        }
                        Log.e("API_ERROR", "Response unsuccessful: ${response.message}")
                    }
                }
            }
        })
    }

    private suspend fun insertUserChat(userChatEntity: ChatTable) {
        chatDao.insertMessage(userChatEntity)
    }

    private fun parseResponse(responseBody: String): String {
        return try {
            val jsonResponse = JSONObject(responseBody)
            val candidatesArray = jsonResponse.optJSONArray("candidates")

            if (candidatesArray != null && candidatesArray.length() > 0) {
                val firstCandidate = candidatesArray.getJSONObject(0)
                val contentObject = firstCandidate.getJSONObject("content")
                val partsArray = contentObject.optJSONArray("parts")

                if (partsArray != null && partsArray.length() > 0) {
                    val text = partsArray.getJSONObject(0).getString("text")
                    parseAndFormatResponse(text)
                } else {
                    "No parts found in the response."
                }
            } else {
                "No candidates found in the response."
            }
        } catch (e: Exception) {
            Log.e("PARSE_ERROR", "Failed to parse response", e)
            "Failed to parse response."
        }
    }

    private fun parseAndFormatResponse(response: String): String {
        return response.replace("*", "").trimEnd('\n')
    }
}
