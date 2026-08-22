package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.signature

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntSize
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.model.SignaturePoint
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.model.SignatureStroke

class SignatureController {

    private val _paths = mutableListOf<Path>()
    val paths: List<Path> get() = _paths

    private val _strokes = mutableListOf<MutableList<SignaturePoint>>()

    val strokes: List<SignatureStroke>
        get() = _strokes.map { stroke ->
            SignatureStroke(points = stroke.toList())
        }

    var canvasSize by mutableStateOf(IntSize.Zero)
        internal set

    var pathUpdateTrigger by mutableIntStateOf(0)
        private set

    var isEmpty by mutableStateOf(true)
        private set

    fun startStroke(x: Float, y: Float, pressure: Float) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return

        val path = Path().apply {
            moveTo(x, y)
        }
        _paths.add(path)

        val firstPoint = SignaturePoint(
            x = (x / canvasSize.width.toFloat()).coerceIn(0f, 1f),
            y = (y / canvasSize.height.toFloat()).coerceIn(0f, 1f),
            pressure = normalizePressure(pressure)
        )

        _strokes.add(mutableListOf(firstPoint))
        isEmpty = false
        pathUpdateTrigger++
    }

    fun addPoint(x: Float, y: Float, pressure: Float) {
        if (_paths.isEmpty() || _strokes.isEmpty() || canvasSize.width <= 0 || canvasSize.height <= 0) return

        _paths.last().lineTo(x, y)

        val point = SignaturePoint(
            x = (x / canvasSize.width.toFloat()).coerceIn(0f, 1f),
            y = (y / canvasSize.height.toFloat()).coerceIn(0f, 1f),
            pressure = normalizePressure(pressure)
        )

        _strokes.last().add(point)
        pathUpdateTrigger++
    }

    private fun normalizePressure(pressure: Float): Float {
        return if (pressure > 0f) pressure.coerceIn(0f, 1f) else 1f
    }

    fun clear() {
        _paths.clear()
        _strokes.clear()
        isEmpty = true
        pathUpdateTrigger++
    }
}