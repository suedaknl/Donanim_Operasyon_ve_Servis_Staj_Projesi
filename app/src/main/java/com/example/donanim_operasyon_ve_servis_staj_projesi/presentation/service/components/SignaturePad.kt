package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.common // Paket adını kendi yapına göre uyarla

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import java.io.File
import java.io.FileOutputStream

// --- 1. SIGNATURE CONTROLLER (State ve Mantık Yönetimi) ---
// ViewModel'dan bağımsız, sadece bu bileşene özel bir state yöneticisi
class SignatureController {
    private val _paths = mutableListOf<Path>()
    val paths: List<Path> get() = _paths

    // Canvas'ın anlık boyutunu tutar
    var canvasSize by mutableStateOf(IntSize.Zero)
        internal set

    // Compose'a yeniden çizim (recomposition) yapmasını söyleyen tetikleyici
    var pathUpdateTrigger by mutableIntStateOf(0)
        private set

    // İmzanın boş olup olmadığını anlar
    var isEmpty by mutableStateOf(true)
        private set

    fun addPath(path: Path) {
        _paths.add(path)
        isEmpty = false
        pathUpdateTrigger++
    }

    fun updateLastPath(x: Float, y: Float) {
        if (_paths.isNotEmpty()) {
            _paths.last().lineTo(x, y)
            pathUpdateTrigger++
        }
    }

    fun clear() {
        _paths.clear()
        isEmpty = true
        pathUpdateTrigger++
    }

    // Compose Path'lerini Android Bitmap'e dönüştürür (Arka planda çizim yapar)
    fun getSignatureBitmap(): Bitmap? {
        if (isEmpty || canvasSize.width <= 0 || canvasSize.height <= 0) return null

        // Şeffaf yerine beyaz arka planlı bir Bitmap oluşturuyoruz
        val bitmap = Bitmap.createBitmap(canvasSize.width, canvasSize.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeCap = android.graphics.Paint.Cap.ROUND
        }

        // Kullanıcının çizdiği tüm yolları Bitmap'e aktar
        _paths.forEach { path ->
            canvas.drawPath(path.asAndroidPath(), paint)
        }

        return bitmap
    }
}

// Controller'ı Compose lifecycle'ı boyunca hayatta tutar (Recomposition'da silinmez)
@Composable
fun rememberSignatureController(): SignatureController {
    return remember { SignatureController() }
}

// --- 2. SIGNATURE PAD COMPOSABLE (Kullanıcı Arayüzü) ---
@Composable
fun SignaturePad(
    controller: SignatureController,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 8f
) {
    // pathUpdateTrigger okunarak recomposition zorlanıyor
    val trigger = controller.pathUpdateTrigger

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onSizeChanged { size ->
                controller.canvasSize = size
            }
            // Dokunma ve kaydırma hareketlerini yakalayıp scroll yapılmasını engeller (consume)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val path = Path().apply { moveTo(down.position.x, down.position.y) }
                    controller.addPath(path)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            controller.updateLastPath(change.position.x, change.position.y)
                            change.consume()
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        // Tetikleyiciyi kullandığımız için lint uyarısını yoksayabilir veya loglayabiliriz
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

// --- 3. FILE HELPER (Bitmap'i PNG'ye Çevirip Kaydetme) ---
// Fotoğraf sistemindeki gibi bağımsız, BLOB yerine URI döndüren yardımcı metot
fun saveSignatureToInternalStorage(context: Context, bitmap: Bitmap): String? {
    return try {
        // Benzersiz dosya adı oluştur
        val filename = "signature_${System.currentTimeMillis()}.png"

        // Uygulamanın dahili dosyalarına (internal storage) kaydet
        val file = File(context.filesDir, filename)

        FileOutputStream(file).use { outStream ->
            // Kayıpsız PNG formatında yaz
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        }

        // Veritabanına yazılacak olan Local URI'yi (Dosya yolunu) döndür
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}