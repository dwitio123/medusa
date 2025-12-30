package tgs.app.medusa.ui.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.gotev.speech.GoogleVoiceTypingDisabledException
import net.gotev.speech.Speech
import net.gotev.speech.SpeechDelegate
import net.gotev.speech.SpeechRecognitionNotAvailable
import tgs.app.medusa.R
import tgs.app.medusa.databinding.ActivityPetrificationBinding

class PetrificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetrificationBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false

        if (audioGranted && cameraGranted) {
            startSpeechRecognition()
        } else {
            Toast.makeText(this, "Izin Mikrofon & Kamera diperlukan", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPetrificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Speech.init(this@PetrificationActivity, packageName)

        checkPermissionAndStart()

//        binding.imgMedusa.setOnClickListener {
//            checkPermissionAndStart()
//        }

        val colors = intArrayOf(
            getColor(this, android.R.color.white),
            getColor(this, android.R.color.white),
            getColor(this, android.R.color.white),
            getColor(this, android.R.color.white),
            getColor(this, android.R.color.white)
        )
        binding.progress.setColors(colors)
    }
    private fun checkPermissionAndStart() {
        val audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        if (audioPermission == PackageManager.PERMISSION_GRANTED && cameraPermission == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition()
        } else {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            ))
        }
    }

    private fun startSpeechRecognition() {
        try {
            Speech.getInstance().startListening(binding.progress,object : SpeechDelegate {
                override fun onStartOfSpeech() {
                    Log.i("speech", "speech recognition is now active")
                }

                override fun onSpeechRmsChanged(value: Float) {
//                     Log.d("speech", "rms is now: $value")
                }

                override fun onSpeechPartialResults(results: MutableList<String?>) {
                    val str = StringBuilder()
                    for (res in results) {
                        str.append(res).append(" ")
                    }
                    Log.i("speech", "partial result: ${str.toString().trim()}")
                }

                override fun onSpeechResult(result: String?) {
                    Log.i("speech", "Full Result: $result")

                    if (result.isNullOrEmpty()) {
                        restartRecognition()
                        return
                    }

                    if (result != null) {
                        // Regex untuk menangkap Angka Meter dan Angka Detik
                        // Group 1: Angka/Kata untuk Meter
                        // Group 2: Angka/Kata untuk Second
                        val numberPattern = """(\d+|one|two|to|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|twenty|thirty|forty|fifty)"""

                        val regex = Regex(
                            """$numberPattern\s*meter(?:s)?\s*$numberPattern\s*second(?:s)?""",
                            RegexOption.IGNORE_CASE
                        )

                        val match = regex.find(result)

                        if (match != null) {
                            // Ambil nilai mentah dari grup regex
                            val rawMeter = match.groupValues[1].lowercase()
                            val rawSecond = match.groupValues[2].lowercase()

                            // Fungsi lokal untuk konversi kata ke string angka
                            fun wordToNumber(value: String): String {
                                return when (value) {
                                    "one" -> "1"
                                    "to", "two" -> "2"
                                    "three" -> "3"
                                    "four" -> "4"
                                    "five" -> "5"
                                    "six" -> "6"
                                    "seven" -> "7"
                                    "eight" -> "8"
                                    "nine" -> "9"
                                    "ten" -> "10"
                                    "eleven" -> "11"
                                    "twelve" -> "12"
                                    "twenty" -> "20"
                                    "thirty" -> "30"
                                    "forty" -> "40"
                                    "fifty" -> "50"
                                    else -> value
                                }
                            }

                            val meterValue = wordToNumber(rawMeter)
                            val secondValue = wordToNumber(rawSecond)

                            Log.i("speech", "Detected: $meterValue Meters, $secondValue Seconds")

                            // Tampilkan hasil di UI
                            binding.txtRange.text = "$meterValue Meter $secondValue Second"

                            lifecycleScope.launch {
                                var timeLeft = secondValue.toIntOrNull() ?: 0
                                while (timeLeft > 0) {
                                    binding.txtStatus.text = "COUNTDOWN: $timeLeft"
                                    delay(1000)
                                    timeLeft--
                                }

                                // Efek Aktivasi
//                                binding.rayMedusa.visibility = View.VISIBLE
                                animateRayActivation()
                                binding.txtStatus.text = "ACTIVATED"
                                binding.txtStatus.setTextColor(getColor(this@PetrificationActivity, R.color.red))

                                launch {
                                    flashlightFadeEffect(10000) // 10 detik fade
                                }

                                val sfx = MediaPlayer.create(this@PetrificationActivity, R.raw.sfx_medusa)
                                sfx.start()

                                delay(10000) // Durasi Medusa aktif

                                // Reset ke IDLE
                                turnOnFlashlight(false)
                                binding.rayMedusa.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(500).withEndAction {
                                    binding.rayMedusa.visibility = View.INVISIBLE
                                }.start()
                                binding.txtRange.text = "0 Meter 0 Second"
                                binding.txtStatus.text = "IDLE"
                                binding.txtStatus.setTextColor(getColor(this@PetrificationActivity, R.color.white))
                                sfx.release()
                                restartRecognition()
                            }
                        } else {
                            Log.i("speech", "Format tidak sesuai (Harus: X meter X second)")
                            binding.txtRange.text = "TRY AGAIN"
                            restartRecognition()
                        }
                    }
                }
            })
        } catch (exc: SpeechRecognitionNotAvailable) {
            Log.e("speech", "Speech recognition is not available on this device!")
        } catch (exc: GoogleVoiceTypingDisabledException) {
            Log.e("speech", "Google voice typing must be enabled!")
        }
    }

    // Tambahkan fungsi pembantu untuk restart yang aman
    private fun restartRecognition() {
        lifecycleScope.launch {
            delay(500) // Jeda singkat agar service tidak bentrok
            try {
                startSpeechRecognition()
            } catch (e: Exception) {
                Log.e("speech", "Restart failed: ${e.message}")
            }
        }
    }

    private fun animateRayActivation() {
        binding.rayMedusa.apply {
            visibility = View.VISIBLE
            scaleX = 0f
            scaleY = 0f
            alpha = 1f

            animate()
                .scaleX(10f)
                .scaleY(10f)
                .alpha(1f)
                .setDuration(10000)
                .withEndAction {
                    // Opsional: Animasi "denyut" setelah membesar
                    animatePulse()
                }
                .start()
        }
    }

    private fun animatePulse() {
        binding.rayMedusa.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(800)
            .withEndAction {
                binding.rayMedusa.animate()
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(800)
                    .start()
            }
            .start()
    }

        private fun turnOnFlashlight(active: Boolean) {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                val cameraId = cameraManager.cameraIdList[0]
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(cameraId, active)
                }
            } catch (e: Exception) {
                Log.e("Flashlight", "Error: ${e.message}")
            }
        }

    private suspend fun flashlightFadeEffect(durationMs: Long) {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)

            // Cek apakah HP mendukung kontrol intensitas (API 33+)
            val isStrengthSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    (characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1) > 1

            if (isStrengthSupported) {
                val maxLevel = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                val steps = 20
                val interval = durationMs / steps

                for (i in 1..steps) {
                    val currentLevel = (maxLevel * i) / steps
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        cameraManager.turnOnTorchWithStrengthLevel(cameraId, currentLevel)
                    }
                    delay(interval)
                }
            } else {
                // FALLBACK: Jika tidak mendukung tingkat kecerahan,
                // senter langsung menyala penuh
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(cameraId, true)
                }
                delay(durationMs)
            }
        } catch (e: Exception) {
            Log.e("Flashlight", "Fade In Error: ${e.message}")
        }
    }



    override fun onDestroy() {
        Speech.getInstance().shutdown()
        super.onDestroy()
    }
}