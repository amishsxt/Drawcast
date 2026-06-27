package com.amishsxt.drawcast.annotation.models

sealed class DataMessage {
    data class AnnotationAdded(val annotation: Annotation) : DataMessage()
    data class AnnotationUndone(val id: String) : DataMessage()
    object ClearAll : DataMessage()
}
