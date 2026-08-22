package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.converter

import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.model.SignaturePoint
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.model.SignatureStroke
import org.json.JSONArray
import org.json.JSONObject

object SignatureConverter {

    fun toJson(strokes: List<SignatureStroke>): String {
        val strokesArray = JSONArray()

        strokes.forEach { stroke ->
            val pointsArray = JSONArray()

            stroke.points.forEach { point ->
                val pointObject = JSONObject().apply {
                    put("x", point.x.toDouble())
                    put("y", point.y.toDouble())
                    put("pressure", point.pressure.toDouble())
                }

                pointsArray.put(pointObject)
            }

            val strokeObject = JSONObject().apply {
                put("points", pointsArray)
            }

            strokesArray.put(strokeObject)
        }

        return strokesArray.toString()
    }

    fun fromJson(json: String): List<SignatureStroke> {
        if (json.isBlank()) return emptyList()

        return try {
            val strokesArray = JSONArray(json)
            val strokes = mutableListOf<SignatureStroke>()

            for (i in 0 until strokesArray.length()) {
                val strokeObject = strokesArray.getJSONObject(i)
                val pointsArray = strokeObject.getJSONArray("points")

                val points = mutableListOf<SignaturePoint>()

                for (j in 0 until pointsArray.length()) {
                    val pointObject = pointsArray.getJSONObject(j)

                    points.add(
                        SignaturePoint(
                            x = pointObject.getDouble("x").toFloat(),
                            y = pointObject.getDouble("y").toFloat(),
                            pressure = pointObject
                                .optDouble("pressure", 1.0)
                                .toFloat()
                        )
                    )
                }

                strokes.add(
                    SignatureStroke(points = points)
                )
            }

            strokes
        } catch (e: Exception) {
            emptyList()
        }
    }
}