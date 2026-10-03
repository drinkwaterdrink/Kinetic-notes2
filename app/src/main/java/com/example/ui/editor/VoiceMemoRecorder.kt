package com.example.ui.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KineticDarkBorder
import com.example.ui.theme.KineticDarkSurface
import com.example.ui.theme.KineticDarkSurfaceVariant
import com.example.ui.theme.KineticPrimary
import com.example.ui.theme.KineticSecondary
import com.example.ui.theme.KineticTextMuted
import com.example.ui.theme.KineticTextPrimary
import com.example.ui.theme.KineticTextSecondary
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun VoiceMemoRecorder(
    audioDurationMs: Long,
    onRecordingFinished: (durationMs: Long) -> Unit,
    onTranscribeRequested: () -> Unit,
    isAiLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentRecordedMs by remember { mutableLongStateOf(audioDurationMs.coerceAtLeast(0L)) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }

    // Waveform amplitudes
    val amplitudes = remember {
        val list = mutableListOf<Float>()
        val rnd = Random(42)
        repeat(40) {
            list.add(0.2f + rnd.nextFloat() * 0.75f)
        }
        list
    }

    // Live recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            currentRecordedMs = 0L
            while (isRecording) {
                delay(100)
                currentRecordedMs += 100
            }
        }
    }

    // Playback progression
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val total = if (currentRecordedMs > 0) currentRecordedMs else 30000L
            val step = 100L
            while (isPlaying && playbackProgress < 1f) {
                delay(step)
                playbackProgress += step.toFloat() / total
            }
            if (playbackProgress >= 1f) {
                isPlaying = false
                playbackProgress = 0f
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .testTag("voice_memo_recorder")
    ) {
        // Waveform Visualizer Surface
        Surface(
            color = KineticDarkSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, KineticDarkBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    val barWidth = 6.dp.toPx()
                    val gap = 4.dp.toPx()
                    val totalBars = amplitudes.size
                    val totalWidth = totalBars * (barWidth + gap)
                    val startOffset = (size.width - totalWidth) / 2f

                    amplitudes.forEachIndexed { index, amp ->
                        val effectiveAmp = if (isRecording) {
                            (amp + (Random.nextFloat() * 0.4f - 0.2f)).coerceIn(0.15f, 1.0f)
                        } else {
                            amp
                        }

                        val barHeight = size.height * effectiveAmp
                        val top = (size.height - barHeight) / 2f
                        val left = startOffset + index * (barWidth + gap)

                        val isPassed = (index.toFloat() / totalBars) <= playbackProgress

                        val barColor = when {
                            isRecording -> Color(0xFFF43F5E) // Red active recording
                            isPassed -> KineticSecondary     // Cyan played portion
                            else -> KineticPrimary.copy(alpha = 0.5f) // Electric indigo remaining
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }

                // Waveform HUD Overlay
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFF43F5E), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RECORDING • ${currentRecordedMs / 1000}s",
                            color = Color(0xFFF43F5E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    } else {
                        Text(
                            text = if (currentRecordedMs > 0) "RECORDED MEMO (${currentRecordedMs / 1000}s)" else "READY TO RECORD",
                            color = KineticTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Record & Playback Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Playback Toggle (if memo recorded)
            if (currentRecordedMs > 0) {
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            isPlaying = false
                        } else {
                            if (playbackProgress >= 1f) playbackProgress = 0f
                            isPlaying = true
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(KineticDarkSurfaceVariant, CircleShape)
                        .border(1.dp, KineticDarkBorder, CircleShape)
                        .testTag("btn_audio_play")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = KineticSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))
            }

            // Record / Stop Main Button
            FloatingActionButton(
                onClick = {
                    if (isRecording) {
                        isRecording = false
                        onRecordingFinished(currentRecordedMs)
                    } else {
                        isRecording = true
                        isPlaying = false
                    }
                },
                containerColor = if (isRecording) Color(0xFFF43F5E) else KineticPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(68.dp)
                    .testTag("btn_audio_record")
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Record Memo",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Gemini AI Transcription Button
        Button(
            onClick = onTranscribeRequested,
            enabled = !isAiLoading && currentRecordedMs > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = KineticPrimary,
                contentColor = Color.White,
                disabledContainerColor = KineticDarkSurfaceVariant,
                disabledContentColor = KineticTextMuted
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_ai_transcribe")
        ) {
            if (isAiLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini 2.5 Flash Transcribing...", fontSize = 13.sp)
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemini AI",
                    tint = KineticSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini Transcribe & Extract Checklist", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
