package tgs.app.medusa.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startSpeechRecognition()
        } else {
            Toast.makeText(this, "Izin mikrofon diperlukan untuk fitur ini", Toast.LENGTH_SHORT)
                .show()
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

        binding.imgMedusa.setOnClickListener {
            checkPermissionAndStart()
        }
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startSpeechRecognition()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
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
                        // Regex yang diperbarui:
                        // (\d+|one|two|three|four|five|six|seven|eight|nine|ten) -> Mencari angka digit ATAU kata angka
                        // \s* -> spasi opsional
                        // second(s)? -> kata second atau seconds
                        val regex = Regex("""(\d+|one|to|two|three|four|five|six|seven|eight|nine|ten)\s*second(s)?""", RegexOption.IGNORE_CASE)
                        val match = regex.find(result)

                        if (match != null) {
                            val secondValue = match.value // Contoh: "one second" atau "1 second"
                            Log.i("speech", "Waktu yang diambil: $secondValue")

                            // Ambil angka/kata angkanya saja
                            val rawValue = match.groupValues[1]

                            // Konversi kata menjadi angka (Opsional, jika Anda butuh Int untuk kalkulasi)
                            val numericValue = when(rawValue.lowercase()) {
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
                                else -> rawValue // Jika sudah berupa digit "1", "2", dll
                            }

                            Log.i("speech", "Angka detiknya: $numericValue")

                            binding.txtRange.text = result

                            lifecycleScope.launch {
                                var timeLeft = numericValue.toInt()
                                while (timeLeft > 0) {
                                    binding.txtStatus.text = "COUNTDOWN: $timeLeft"
                                    delay(1000) // Tunggu 1 detik
                                    timeLeft--
                                }
                                binding.rayMedusa.visibility = View.VISIBLE
                                binding.txtStatus.text = "ACTIVATED"
                                val sfx = MediaPlayer.create(this@PetrificationActivity, R.raw.sfx_medusa)
                                sfx.start()
                                Log.i("speech", "Countdown selesai")

                                delay(10000)
                                binding.rayMedusa.visibility = View.INVISIBLE
                                binding.txtStatus.text = "IDLE"
                                sfx.stop()
//                                restartRecognition()
                            }
                        } else {
                            Log.i("speech", "Format waktu tidak ditemukan dalam kalimat")
                            binding.txtRange.text = result
                            binding.txtStatus.text = "TIME NOT FOUND"
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

    override fun onDestroy() {
        Speech.getInstance().shutdown()
        super.onDestroy()
    }
}