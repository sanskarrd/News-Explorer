package com.example.newsexplorer.data.repository

import com.example.newsexplorer.data.api.RetrofitInstance
import com.example.newsexplorer.data.model.NewsResponse

class NewsRepository {

    // Fetch top headlines from API
    suspend fun getTopHeadlines(country: String, category: String?, apiKey: String): NewsResponse {
        return RetrofitInstance.api.getTopHeadlines(country, category, apiKey)
    }
}
