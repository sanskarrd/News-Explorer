package com.example.newsexplorer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsexplorer.data.model.Article
import com.example.newsexplorer.data.repository.NewsRepository
import com.example.newsexplorer.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository = NewsRepository()
) : ViewModel() {
    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles
    private val _category = MutableStateFlow<String?>(null)
    val category: StateFlow<String?> = _category

    init {
        refresh()
    }

    fun setCategory(newCategory: String?) {
        _category.value = newCategory
        refresh()
    }

    fun refresh(country: String = "us") {
        viewModelScope.launch {
            runCatching {
                val response = repository.getTopHeadlines(
                    country = country,
                    category = _category.value,
                    apiKey = BuildConfig.NEWS_API_KEY
                )
                response.articles
            }.onSuccess { fetched -> _articles.value = fetched }
                .onFailure { _articles.value = emptyList() }
        }
    }
}
