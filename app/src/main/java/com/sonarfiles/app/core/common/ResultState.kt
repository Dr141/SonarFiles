package com.sonarfiles.app.core.common

sealed interface ResultState<out T> {
    data object Idle : ResultState<Nothing>
    data object Loading : ResultState<Nothing>
    data class Success<out T>(val data: T) : ResultState<T>
    data class Error(val error: NetworkError) : ResultState<Nothing>

    val isSuccess: Boolean
        get() = this is Success

    val isLoading: Boolean
        get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
}
