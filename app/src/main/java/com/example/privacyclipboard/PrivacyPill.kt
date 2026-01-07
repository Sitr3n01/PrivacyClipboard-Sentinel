package com.example.privacyclipboard // Use seu pacote real

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
// Adicionamos um parâmetro novo: uma função para chamar quando terminar
fun PrivacyPill(appName: String, onAnimationFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    // Lógica de tempo controlada pelo Compose
    LaunchedEffect(Unit) {
        visible = true // Dispara animação de entrada (desce)
        delay(2500)    // Espera 2.5 segundos na tela
        visible = false // Dispara animação de saída (sobe)
        delay(500)     // Espera a animação de saída terminar
        onAnimationFinished() // Avisa o Service que pode fechar
    }

    AnimatedVisibility(
        visible = visible,
        // ENTRADA: Desliza de cima + Aparece suavemente
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> -fullHeight }, // Começa exatamente acima da posição final
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(animationSpec = tween(300)),

        // SAÍDA: Desliza para cima + Desaparece suavemente
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> -fullHeight }, // Sobe de volta para onde veio
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(200))
    ) {
        PrivacyPillContent(appName)
    }
}

@Composable
fun PrivacyPillContent(appName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .padding(top = 18.dp)
            .wrapContentWidth(),
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(scale)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$appName leu seu clipboard",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}