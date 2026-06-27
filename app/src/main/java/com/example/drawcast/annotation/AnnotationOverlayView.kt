package com.example.drawcast.annotation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.drawcast.annotation.models.Annotation

class AnnotationOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var annotations: List<Annotation> = emptyList()

    fun setAnnotations(annotations: List<Annotation>) {
        this.annotations = annotations
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        annotations.forEach { annotation ->
            when (annotation) {
                is Annotation.FreeDraw -> drawFreeDraw(canvas, annotation)
                is Annotation.Arrow -> drawArrow(canvas, annotation)
                is Annotation.Circle -> drawCircle(canvas, annotation)
            }
        }
    }

    private fun drawFreeDraw(canvas: Canvas, annotation: Annotation.FreeDraw) {
        if (annotation.points.size < 2) return
        paint.color = annotation.color
        paint.strokeWidth = annotation.width
        val points = annotation.points
        for (i in 0 until points.size - 1) {
            val x1 = points[i].x * width
            val y1 = points[i].y * height
            val x2 = points[i + 1].x * width
            val y2 = points[i + 1].y * height
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
    }

    private fun drawArrow(canvas: Canvas, annotation: Annotation.Arrow) {
        // TODO: draw arrow with arrowhead
    }

    private fun drawCircle(canvas: Canvas, annotation: Annotation.Circle) {
        // TODO: draw circle with normalized coords
    }
}
