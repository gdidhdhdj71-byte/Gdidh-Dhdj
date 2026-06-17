package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Alarm
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

class ClockViewModel(application: Application) : AndroidViewModel(application) {

    private val alarmRepository: AlarmRepository

    init {
        val alarmDao = AlarmDatabase.getDatabase(application).alarmDao()
        alarmRepository = AlarmRepository(alarmDao)
    }

    // -------------------------------------------------------------
    // Tab State
    // -------------------------------------------------------------
    var currentTab by mutableStateOf(0) // 0: Clock, 1: Alarm, 2: Stopwatch, 3: Timer

    // -------------------------------------------------------------
    // Clock & World Clock State
    // -------------------------------------------------------------
    private val _currentTime = MutableStateFlow(Date())
    val currentTime: StateFlow<Date> = _currentTime.asStateFlow()

    // World Zone Item Definitions
    data class WorldClock(
        val cityName: String,
        val timeZoneId: String,
        val country: String,
        val isPinned: Boolean
    )

    private val allWorldClocksAvailable = listOf(
        WorldClock("New York", "America/New_York", "United States", true),
        WorldClock("London", "Europe/London", "United Kingdom", true),
        WorldClock("Tokyo", "Asia/Tokyo", "Japan", true),
        WorldClock("Paris", "Europe/Paris", "France", false),
        WorldClock("Sydney", "Australia/Sydney", "Australia", false),
        WorldClock("Seoul", "Asia/Seoul", "South Korea", false),
        WorldClock("Los Angeles", "America/Los_Angeles", "United States", false),
        WorldClock("Mumbai", "Asia/Kolkata", "India", false),
        WorldClock("Cairo", "Africa/Cairo", "Egypt", false),
        WorldClock("Dubai", "Asia/Dubai", "United Arab Emirates", false)
    )

    private val _worldClocks = MutableStateFlow<List<WorldClock>>(emptyList())
    val worldClocks: StateFlow<List<WorldClock>> = _worldClocks.asStateFlow()

    // -------------------------------------------------------------
    // Alarm State
    // -------------------------------------------------------------
    private val _alarmsList = MutableStateFlow<List<Alarm>>(emptyList())
    val alarmsList: StateFlow<List<Alarm>> = _alarmsList.asStateFlow()

    // -------------------------------------------------------------
    // Stopwatch State
    // -------------------------------------------------------------
    private var stopwatchJob: Job? = null
    var stopwatchRunning by mutableStateOf(false)
        private set
    var stopwatchTime by mutableStateOf(0L) // in milliseconds
        private set

    data class Lap(
        val lapNumber: Int,
        val lapTime: Long,      // duration of this exact lap
        val cumulativeTime: Long // total elapsed up to this lap
    )
    private val _laps = MutableStateFlow<List<Lap>>(emptyList())
    val laps: StateFlow<List<Lap>> = _laps.asStateFlow()

    // -------------------------------------------------------------
    // Timer State
    // -------------------------------------------------------------
    enum class TimerStatus { IDLE, RUNNING, PAUSED, FINISHED }
    var timerStatus by mutableStateOf(TimerStatus.IDLE)
        private set
    var timerInitialDuration by mutableStateOf(0L) // Total starting time in ms
        private set
    var timerRemainingTime by mutableStateOf(0L) // Remaining time in ms
        private set

    private var timerJob: Job? = null

    init {
        // Observe alarms from Room
        viewModelScope.launch {
            alarmRepository.allAlarms.collectLatest { alarms ->
                _alarmsList.value = alarms
            }
        }

        // Initialize world clocks list
        _worldClocks.value = allWorldClocksAvailable

        // Clock standard tick (every 50ms for smooth sweep seconds hand)
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                _currentTime.value = Date()
                delay(50)
            }
        }
    }

    // -------------------------------------------------------------
    // World Clock Actions
    // -------------------------------------------------------------
    fun togglePinWorldClock(city: WorldClock) {
        _worldClocks.value = _worldClocks.value.map {
            if (it.cityName == city.cityName) {
                it.copy(isPinned = !it.isPinned)
            } else {
                it
            }
        }
    }

    fun getFormattedTimeForZone(timeZoneId: String): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone(timeZoneId)
        return sdf.format(_currentTime.value)
    }

    fun getFormattedDateForZone(timeZoneId: String): String {
        val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone(timeZoneId)
        return sdf.format(_currentTime.value)
    }

    // -------------------------------------------------------------
    // Alarm Actions
    // -------------------------------------------------------------
    fun addAlarm(hour: Int, minute: Int, label: String, days: String) {
        viewModelScope.launch {
            val newAlarm = Alarm(
                hour = hour,
                minute = minute,
                label = label,
                daysOfWeek = days,
                isEnabled = true
            )
            alarmRepository.insert(newAlarm)
        }
    }

    fun toggleAlarmEnabled(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.update(alarm.copy(isEnabled = !alarm.isEnabled))
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.delete(alarm)
        }
    }

    // -------------------------------------------------------------
    // Stopwatch Actions
    // -------------------------------------------------------------
    fun startStopwatch() {
        if (stopwatchRunning) return
        stopwatchRunning = true
        val startTime = System.currentTimeMillis() - stopwatchTime
        stopwatchJob = viewModelScope.launch(Dispatchers.Default) {
            while (stopwatchRunning) {
                stopwatchTime = System.currentTimeMillis() - startTime
                delay(10) // tick every 10ms for millisecond accuracy
            }
        }
    }

    fun pauseStopwatch() {
        stopwatchRunning = false
        stopwatchJob?.cancel()
    }

    fun lapStopwatch() {
        val currentTotal = stopwatchTime
        val lapNumber = _laps.value.size + 1
        val lastLapCumulative = if (_laps.value.isEmpty()) 0L else _laps.value.first().cumulativeTime
        val lapDiff = currentTotal - lastLapCumulative

        val newLap = Lap(
            lapNumber = lapNumber,
            lapTime = lapDiff,
            cumulativeTime = currentTotal
        )
        // Add at the beginning of the list for easy review
        _laps.value = listOf(newLap) + _laps.value
    }

    fun resetStopwatch() {
        pauseStopwatch()
        stopwatchTime = 0L
        _laps.value = emptyList()
    }

    fun formatStopwatchTime(timeMs: Long): String {
        val minutes = (timeMs / 60000) % 60
        val seconds = (timeMs / 1000) % 60
        val mills = (timeMs / 10) % 100
        return String.format("%02d:%01d%d.%02d", minutes, seconds / 10, seconds % 10, mills)
    }

    // -------------------------------------------------------------
    // Timer Actions
    // -------------------------------------------------------------
    fun setTimerDuration(hours: Long, minutes: Long, seconds: Long) {
        val totalMs = (hours * 3600 + minutes * 60 + seconds) * 1000
        timerInitialDuration = totalMs
        timerRemainingTime = totalMs
        timerStatus = TimerStatus.IDLE
    }

    fun startTimer() {
        if (timerRemainingTime <= 0) return
        timerStatus = TimerStatus.RUNNING
        val endTime = System.currentTimeMillis() + timerRemainingTime
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (timerStatus == TimerStatus.RUNNING) {
                val timeLeft = endTime - System.currentTimeMillis()
                if (timeLeft <= 0) {
                    timerRemainingTime = 0
                    timerStatus = TimerStatus.FINISHED
                    break
                }
                timerRemainingTime = timeLeft
                delay(100)
            }
        }
    }

    fun pauseTimer() {
        if (timerStatus != TimerStatus.RUNNING) return
        timerStatus = TimerStatus.PAUSED
        timerJob?.cancel()
    }

    fun resetTimer() {
        timerJob?.cancel()
        timerStatus = TimerStatus.IDLE
        timerRemainingTime = timerInitialDuration
    }

    fun clearTimer() {
        timerJob?.cancel()
        timerStatus = TimerStatus.IDLE
        timerInitialDuration = 0L
        timerRemainingTime = 0L
    }

    fun formatTimerTime(timeMs: Long): String {
        val hours = (timeMs / 3600000)
        val minutes = (timeMs / 60000) % 60
        val seconds = (timeMs / 1000) % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopwatchJob?.cancel()
        timerJob?.cancel()
    }

    // ViewModel Factory definitions
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ClockViewModel::class.java)) {
                return ClockViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
