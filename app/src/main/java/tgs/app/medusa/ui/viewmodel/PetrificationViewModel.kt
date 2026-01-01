package tgs.app.medusa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tgs.app.medusa.domain.model.PetrificationCommand
import tgs.app.medusa.domain.usecase.ParsePetrificationCommandUseCase

class PetrificationViewModel(
    private val parseCommandUseCase: ParsePetrificationCommandUseCase = ParsePetrificationCommandUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetrificationUiState())
    val uiState: StateFlow<PetrificationUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<PetrificationEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var countdownJob: Job? = null

    fun onSpeechResult(result: String?) {
        if (result.isNullOrBlank()) {
            emitEvent(PetrificationEvent.RestartRecognition)
            return
        }

        val command = parseCommandUseCase.execute(result)
        if (command == null) {
            _uiState.value = _uiState.value.copy(
                rangeText = "TRY AGAIN",
                status = PetrificationStatus.Idle,
                countdown = null
            )
            emitEvent(PetrificationEvent.RestartRecognition)
            return
        }

        updateRange(command)
        startCountdown(command.seconds)
    }

    fun onActivationComplete() {
        _uiState.value = _uiState.value.copy(
            rangeText = "0 Meter 0 Second",
            status = PetrificationStatus.Idle,
            countdown = null
        )
    }

    private fun updateRange(command: PetrificationCommand) {
        _uiState.value = _uiState.value.copy(
            rangeText = "${command.meters} Meter ${command.seconds} Second",
            status = PetrificationStatus.Idle,
            countdown = null
        )
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var timeLeft = seconds
            while (timeLeft > 0) {
                _uiState.value = _uiState.value.copy(
                    status = PetrificationStatus.Countdown,
                    countdown = timeLeft
                )
                delay(1000)
                timeLeft--
            }
            _uiState.value = _uiState.value.copy(
                status = PetrificationStatus.Activated,
                countdown = null
            )
            emitEvent(PetrificationEvent.Activate(durationMs = ACTIVATION_DURATION_MS))
        }
    }

    private fun emitEvent(event: PetrificationEvent) {
        viewModelScope.launch {
            eventChannel.send(event)
        }
    }

    companion object {
        private const val ACTIVATION_DURATION_MS = 10_000L
    }
}

sealed interface PetrificationEvent {
    data object RestartRecognition : PetrificationEvent

    data class Activate(val durationMs: Long) : PetrificationEvent
}

data class PetrificationUiState(
    val rangeText: String = "0 Meter 0 Second",
    val status: PetrificationStatus = PetrificationStatus.Idle,
    val countdown: Int? = null
)

enum class PetrificationStatus {
    Idle,
    Countdown,
    Activated
}
