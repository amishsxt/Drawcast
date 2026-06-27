package com.amishsxt.drawcast.annotation.models

import android.graphics.PointF

sealed class Annotation {
    data class FreeDraw(
        val id: String,
        val points: List<PointF>,
        val color: Int,
        val width: Float
    ) : Annotation()

    data class Arrow(
        val id: String,
        val start: PointF,
        val end: PointF
    ) : Annotation()

    data class Circle(
        val id: String,
        val center: PointF,
        val radius: Float
    ) : Annotation()
}
