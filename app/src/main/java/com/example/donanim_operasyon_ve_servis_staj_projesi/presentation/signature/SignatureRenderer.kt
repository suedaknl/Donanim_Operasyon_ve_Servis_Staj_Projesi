package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.signature

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.model.SignatureStroke

object SignatureRenderer {

    fun renderBitmapFromStrokes(
        strokes: List<SignatureStroke>,
        width: Int,
        height: Int,
        strokeWidth: Float = 8f
    ): Bitmap? {
        if (strokes.isEmpty() || width <= 0 || height <= 0) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            isAntiAlias = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        strokes.forEach { stroke ->
            if (stroke.points.isNotEmpty()) {
                val path = Path()
                stroke.points.forEachIndexed { index, point ->
                    val absX = point.x * width
                    val absY = point.y * height

                    if (index == 0) {
                        path.moveTo(absX, absY)
                    } else {
                        path.lineTo(absX, absY)
                    }
                }
                canvas.drawPath(path, paint)
            }
        }

        return bitmap
    }
}