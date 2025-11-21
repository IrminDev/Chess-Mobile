package com.github.irmin.chess.viewmodel

import androidx.lifecycle.ViewModel
import com.github.irmin.chess.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel para manejar el tema de la aplicación
 */
class ThemeViewModel : ViewModel() {
    private val _currentTheme = MutableStateFlow(AppTheme.MAROON)  // Guinda por defecto
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    /**
     * Cambia al tema guinda
     */
    fun setMaroonTheme() {
        _currentTheme.value = AppTheme.MAROON
    }

    /**
     * Cambia al tema azul
     */
    fun setBlueTheme() {
        _currentTheme.value = AppTheme.BLUE
    }

    /**
     * Alterna entre los temas disponibles
     */
    fun toggleTheme() {
        _currentTheme.value = when (_currentTheme.value) {
            AppTheme.MAROON -> AppTheme.BLUE
            AppTheme.BLUE -> AppTheme.MAROON
        }
    }
}

