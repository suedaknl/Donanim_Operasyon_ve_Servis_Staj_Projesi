package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.signature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.awaitEachGesture

@Composable
fun SignaturePad(
    controller: SignatureController,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 8f
) {
    val trigger = controller.pathUpdateTrigger

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onSizeChanged { size ->
                controller.canvasSize = size
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()

                    controller.startStroke(
                        x = down.position.x,
                        y = down.position.y,
                        pressure = down.pressure
                    )

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()

                        if (change != null && change.pressed) {
                            controller.addPoint(
                                x = change.position.x,
                                y = change.position.y,
                                pressure = change.pressure
                            )
                            change.consume()
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        trigger.let {
            controller.paths.forEach { path ->
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
fun rememberSignatureController(): SignatureController {
    return remember { SignatureController() }
}