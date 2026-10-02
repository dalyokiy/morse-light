package com.example.myapplication

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Азбука Морзе: русские + английские буквы + цифры
val MORSE_MAP = mapOf<Char, String>(
    'а' to ".-", 'б' to "-...", 'в' to ".--", 'г' to "--.", 'д' to "-..",
    'е' to ".", 'ж' to "...-", 'з' to "--..", 'и' to "..", 'й' to ".---",
    'к' to "-.-", 'л' to ".-..", 'м' to "--", 'н' to "-.", 'о' to "---",
    'п' to ".--.", 'р' to ".-.", 'с' to "...", 'т' to "-", 'у' to "..-",
    'ф' to "..-.", 'х' to "....", 'ц' to "-.-.", 'ч' to "---.", 'ш' to "----",
    'щ' to "--.-", 'ъ' to "--.--", 'ы' to "-.--", 'ь' to "-..-", 'э' to "..-..",
    'ю' to "..--", 'я' to ".-.-",
    'a' to ".-", 'b' to "-...", 'c' to "-.-.", 'd' to "-..", 'e' to ".",
    'f' to "..-.", 'g' to "--.", 'h' to "....", 'i' to "..", 'j' to ".---",
    'k' to "-.-", 'l' to ".-..", 'm' to "--", 'n' to "-.", 'o' to "---",
    'p' to ".--.", 'q' to "--.-", 'r' to ".-.", 's' to "...", 't' to "-",
    'u' to "..-", 'v' to "...-", 'w' to ".--", 'x' to "-..-", 'y' to "-.--",
    'z' to "--..",
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
    '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
    '8' to "---..", '9' to "----."
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MorseScreen() }
    }
}

@Composable
fun MorseScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf("SOS") }
    var isOn by remember { mutableStateOf(false) }
    var isTransmitting by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(15f) }
    var status by remember { mutableStateOf("Готов к передаче") }

    // Доступ к фонарику
    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    val cameraId = remember {
        cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    fun torch(on: Boolean) {
        cameraId?.let { cameraManager.setTorchMode(it, on) }
        isOn = on
    }

    // Передача сообщения морзянкой
    fun transmit(text: String) {
        if (isTransmitting) return
        scope.launch {
            isTransmitting = true
            status = "Передача..."
            val unit = (1200 / speed).toLong() // длительность "точки"
            for (ch in text.lowercase()) {
                if (ch == ' ') { delay(unit * 7); continue } // пауза между словами
                val code = MORSE_MAP[ch] ?: continue
                for (s in code) {
                    status = if (s == '.') "• точка" else "— тире"
                    torch(true)
                    delay(if (s == '.') unit else unit * 3)
                    torch(false)
                    delay(unit)
                }
                delay(unit * 2) // пауза между буквами
            }
            torch(false)
            status = "Готов к передаче"
            isTransmitting = false
        }
    }

    // === СТИЛЬНЫЙ ИНТЕРФЕЙС ===
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F1226), Color(0xFF1B2440))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("MORSE LIGHT", color = Color(0xFFFFD54F), fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(24.dp))

            // Светящийся индикатор
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        color = if (isOn) Color(0xFFFFEB3B) else Color(0xFF2A3352),
                        shape = CircleShape
                    )
            )
            Spacer(Modifier.height(12.dp))
            Text(status, color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Сообщение", color = Color(0xFF8890B0)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFFD54F),
                    cursorColor = Color(0xFFFFD54F)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text("Скорость: ${speed.toInt()} слов/мин", color = Color(0xFF8890B0))
            Slider(
                value = speed,
                onValueChange = { speed = it },
                valueRange = 5f..30f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFD54F),
                    activeTrackColor = Color(0xFFFFD54F)
                )
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { transmit(message) },
                    enabled = !isTransmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F),
                        contentColor = Color(0xFF0F1226)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("ПЕРЕДАТЬ", fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { transmit("SOS") },
                    enabled = !isTransmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("SOS", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { torch(!isOn) },
                enabled = !isTransmitting,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD54F))
            ) { Text(if (isOn) "Выключить фонарик" else "Включить фонарик") }
        }
    }
}