package com.example.data.remote

import android.graphics.Bitmap
import com.example.data.model.AIAnalysisResult
import com.example.data.model.UserItem

interface AIService {
    suspend fun analyzeText(text: String): AIAnalysisResult
    suspend fun analyzeImage(bitmap: Bitmap, promptHint: String? = null): AIAnalysisResult
    suspend fun answerQuestion(query: String, contextItems: List<UserItem>): String
}
