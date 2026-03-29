package com.sismptm.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

@Composable
fun HikerIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color.Transparent)) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerX = canvasWidth / 2
        val centerY = canvasHeight / 2

        // Gradiente de fondo oscuro
        drawRect(
            color = Color(0xFF1A1A2E),
            size = size
        )

        // Forma blob orgánica
        drawBlobShape(centerX, centerY, 120.dp.toPx(), Color(0xFF16213E))

        // Dibujar la luna creciente
        drawCrescentMoon(
            centerX = centerX - 100.dp.toPx(),
            centerY = centerY - 100.dp.toPx(),
            radius = 25.dp.toPx()
        )

        // Dibujar estrellas
        drawStars(centerX, centerY)

        // Dibujar el hiker
        drawHiker(
            centerX = centerX,
            centerY = centerY + 20.dp.toPx()
        )
    }
}

private fun DrawScope.drawBlobShape(
    centerX: Float,
    centerY: Float,
    radius: Float,
    color: Color
) {
    val path = Path().apply {
        // Crear una forma orgánica irregular (blob)
        moveTo(centerX, centerY - radius)
        
        // Puntos que crean la forma blob
        cubicTo(
            centerX + radius * 0.5f, centerY - radius * 0.7f,
            centerX + radius * 0.8f, centerY - radius * 0.3f,
            centerX + radius * 0.7f, centerY + radius * 0.3f
        )
        
        cubicTo(
            centerX + radius * 0.9f, centerY + radius * 0.6f,
            centerX + radius * 0.5f, centerY + radius * 0.9f,
            centerX + radius * 0.1f, centerY + radius * 0.95f
        )
        
        cubicTo(
            centerX - radius * 0.4f, centerY + radius * 0.8f,
            centerX - radius * 0.7f, centerY + radius * 0.6f,
            centerX - radius * 0.75f, centerY + radius * 0.2f
        )
        
        cubicTo(
            centerX - radius * 0.9f, centerY - radius * 0.2f,
            centerX - radius * 0.6f, centerY - radius * 0.6f,
            centerX, centerY - radius
        )
    }
    
    drawPath(path, color = color, style = Fill)
}

private fun DrawScope.drawCrescentMoon(
    centerX: Float,
    centerY: Float,
    radius: Float
) {
    // Luna creciente (círculo principal menos un círculo pequeño)
    drawCircle(
        color = Color(0xFFFDD835),
        radius = radius,
        center = Offset(centerX, centerY)
    )
    
    // Cortar parte de la luna para hacer la forma creciente
    drawCircle(
        color = Color(0xFF1A1A2E),
        radius = radius * 0.6f,
        center = Offset(centerX + radius * 0.5f, centerY)
    )
}

private fun DrawScope.drawStars(centerX: Float, centerY: Float) {
    val starPositions = listOf(
        Offset(centerX - 80.dp.toPx(), centerY - 120.dp.toPx()),
        Offset(centerX + 100.dp.toPx(), centerY - 110.dp.toPx()),
        Offset(centerX - 50.dp.toPx(), centerY - 140.dp.toPx()),
        Offset(centerX + 60.dp.toPx(), centerY - 70.dp.toPx())
    )
    
    starPositions.forEach { position ->
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = position
        )
    }
}

private fun DrawScope.drawHiker(
    centerX: Float,
    centerY: Float
) {
    // Cabeza
    drawCircle(
        color = Color(0xFFD4A574),
        radius = 10.dp.toPx(),
        center = Offset(centerX, centerY - 50.dp.toPx())
    )
    
    // Cuerpo
    drawLine(
        color = Color(0xFF2D1B00),
        start = Offset(centerX, centerY - 40.dp.toPx()),
        end = Offset(centerX, centerY - 15.dp.toPx()),
        strokeWidth = 6.dp.toPx()
    )
    
    // Brazo izquierdo
    drawLine(
        color = Color(0xFFD4A574),
        start = Offset(centerX, centerY - 35.dp.toPx()),
        end = Offset(centerX - 20.dp.toPx(), centerY - 25.dp.toPx()),
        strokeWidth = 4.dp.toPx()
    )
    
    // Brazo derecho (sosteniendo bastón)
    drawLine(
        color = Color(0xFFD4A574),
        start = Offset(centerX, centerY - 35.dp.toPx()),
        end = Offset(centerX + 20.dp.toPx(), centerY - 25.dp.toPx()),
        strokeWidth = 4.dp.toPx()
    )
    
    // Bastón
    drawLine(
        color = Color(0xFF8B6F47),
        start = Offset(centerX + 20.dp.toPx(), centerY - 25.dp.toPx()),
        end = Offset(centerX + 25.dp.toPx(), centerY + 20.dp.toPx()),
        strokeWidth = 3.dp.toPx()
    )
    
    // Pierna izquierda
    drawLine(
        color = Color(0xFF2D1B00),
        start = Offset(centerX, centerY - 15.dp.toPx()),
        end = Offset(centerX - 12.dp.toPx(), centerY + 20.dp.toPx()),
        strokeWidth = 5.dp.toPx()
    )
    
    // Pierna derecha
    drawLine(
        color = Color(0xFF2D1B00),
        start = Offset(centerX, centerY - 15.dp.toPx()),
        end = Offset(centerX + 12.dp.toPx(), centerY + 20.dp.toPx()),
        strokeWidth = 5.dp.toPx()
    )
}
