package com.amishsxt.drawcast.annotation

import androidx.lifecycle.ViewModel
import com.amishsxt.drawcast.annotation.models.Annotation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AnnotationViewModel : ViewModel() {

    private val _annotations = MutableStateFlow<List<Annotation>>(emptyList())
    val annotations: StateFlow<List<Annotation>> = _annotations.asStateFlow()

    fun addAnnotation(annotation: Annotation) {
        _annotations.value = _annotations.value + annotation
    }

    fun undo() {
        if (_annotations.value.isNotEmpty()) {
            _annotations.value = _annotations.value.dropLast(1)
        }
    }

    fun clearAll() {
        _annotations.value = emptyList()
    }
}
