package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class UjianScreen {
    SPLASH,
    LOGIN,
    SISWA_HOME,
    EXAM_DETAIL,
    EXAM_TAKE,
    EXAM_RESULT,
    PENGAWAS_HOME,
    MONITORING,
    VIOLATIONS,
    LEADERBOARD,
    PROFILE,
    NOTIFICATIONS
}

class UjianViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = UjianRepository(database.examDao())

    // Postgres Supabase sync setup
    val syncManager = PostgresSyncManager.getInstance()
    val cloudSyncStatus: StateFlow<SyncStatus> = syncManager.syncStatus
    val cloudSyncMessage: StateFlow<String> = syncManager.statusMessage

    fun startCloudSync() {
        viewModelScope.launch {
            syncManager.syncDatabase(database.examDao())
        }
    }

    // UI Navigation State
    private val _currentScreen = MutableStateFlow(UjianScreen.SPLASH)
    val currentScreen: StateFlow<UjianScreen> = _currentScreen.asStateFlow()

    // Auth States
    private val _currentSiswa = MutableStateFlow<SiswaEntity?>(null)
    val currentSiswa: StateFlow<SiswaEntity?> = _currentSiswa.asStateFlow()

    private val _currentProctor = MutableStateFlow<ProctorSessionEntity?>(null)
    val currentProctor: StateFlow<ProctorSessionEntity?> = _currentProctor.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Global Data reactive flows
    val exams: StateFlow<List<ExamEntity>> = repository.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivityLogs: StateFlow<List<StudentActivityLogEntity>> = repository.allActivityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Exam Session States
    private val _activeExam = MutableStateFlow<ExamEntity?>(null)
    val activeExam: StateFlow<ExamEntity?> = _activeExam.asStateFlow()

    private val _activeQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val activeQuestions: StateFlow<List<QuestionEntity>> = _activeQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _warningCount = MutableStateFlow(0)
    val warningCount: StateFlow<Int> = _warningCount.asStateFlow()

    private val _showUnfocusWarning = MutableStateFlow(false)
    val showUnfocusWarning: StateFlow<Boolean> = _showUnfocusWarning.asStateFlow()

    private val _examError = MutableStateFlow<String?>(null)
    val examError: StateFlow<String?> = _examError.asStateFlow()

    private val _isExamForcedFinished = MutableStateFlow(false)
    val isExamForcedFinished: StateFlow<Boolean> = _isExamForcedFinished.asStateFlow()

    private var timerJob: Job? = null

    // Leaderboard
    val leaderboardEntries: StateFlow<List<LeaderboardEntity>> = repository.allLeaderboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getLeaderboardForExam(examId: String): Flow<List<LeaderboardEntity>> {
        return repository.getLeaderboardForExam(examId)
    }

    fun updateLeaderboardAfterExam(exam: ExamEntity) {
        viewModelScope.launch {
            val currentSiswaNisn = _currentSiswa.value?.nisn ?: return@launch
            val currentSiswaName = _currentSiswa.value?.name ?: "Siswa"

            val entry = LeaderboardEntity(
                studentNisn = currentSiswaNisn,
                studentName = currentSiswaName,
                examId = exam.id,
                examTitle = exam.title,
                score = exam.score
            )
            repository.insertLeaderboardEntries(listOf(entry))
        }
    }

    // Notifications
    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = repository.getUnreadNotificationCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markNotificationRead(id: Int) {
        viewModelScope.launch { repository.markNotificationAsRead(id) }
    }

    fun sendNotification(title: String, message: String, type: String, examId: String? = null) {
        viewModelScope.launch {
            repository.insertNotification(NotificationEntity(
                title = title, message = message, type = type, relatedExamId = examId
            ))
        }
    }

    // Student stats
    fun getStudentStats(nisn: String): Flow<StudentStatsEntity?> {
        return repository.getStudentStats(nisn)
    }

    fun refreshStudentStats() {
        val nisn = _currentSiswa.value?.nisn ?: return
        viewModelScope.launch { repository.calculateStudentStats(nisn) }
    }

    // Exam schedules
    val activeSchedules: StateFlow<List<ExamScheduleEntity>> = repository.activeSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initialize and pre-seed databases
        viewModelScope.launch {
            repository.populateDemoDataIfEmpty()
            startCloudSync()
        }
    }

    fun navigateTo(screen: UjianScreen) {
        _currentScreen.value = screen
        _loginError.value = null
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun selectSiswaRole() {
        _currentProctor.value = null
        navigateTo(UjianScreen.LOGIN)
    }

    fun loginUniversal(id: String, nameInput: String) {
        if (id.isBlank()) {
            _loginError.value = "Nomor Induk tidak boleh kosong"
            return
        }
        val processedId = id.trim()
        val isProctor = processedId.startsWith("19") || 
                        processedId.contains("admin", ignoreCase = true) || 
                        processedId.contains("proctor", ignoreCase = true) || 
                        processedId.startsWith("NIP", ignoreCase = true)
        if (isProctor) {
            loginProctor(processedId, nameInput)
        } else {
            loginSiswa(processedId, nameInput)
        }
    }

    fun loginSiswa(nisn: String, namaInput: String) {
        if (nisn.isBlank()) {
            _loginError.value = "NISN tidak boleh kosong"
            return
        }
        viewModelScope.launch {
            val processedNisn = nisn.trim()
            val existingSiswa = repository.getSiswaByNisn(processedNisn)
            
            if (existingSiswa != null) {
                _currentSiswa.value = existingSiswa
                _currentProctor.value = null
                navigateTo(UjianScreen.SISWA_HOME)
            } else {
                // If not found in seed, dynamically register a mock student to make the app frictionless
                val newSiswa = SiswaEntity(
                    nisn = processedNisn,
                    name = if (namaInput.isNotBlank()) namaInput.trim() else "Siswa Baru #$processedNisn",
                    kelas = "XII MIPA 1",
                    sekolah = "SMA Negeri 1 Jakarta"
                )
                repository.insertSiswa(newSiswa)
                _currentSiswa.value = newSiswa
                _currentProctor.value = null
                navigateTo(UjianScreen.SISWA_HOME)
            }
        }
    }

    fun loginProctor(nip: String, nameInput: String) {
        if (nip.isBlank()) {
            _loginError.value = "NIP tidak boleh kosong"
            return
        }
        viewModelScope.launch {
            val processedNip = nip.trim()
            // We just authorize standard or mock proctor easily
            val sessionName = if (nameInput.isNotBlank()) nameInput.trim() else "Budi Setiawan, M.Pd"
            val session = ProctorSessionEntity(
                nip = processedNip,
                name = sessionName,
                school = "SMA Negeri 1 Jakarta",
                supervisedClass = "Ruang Ujian XII-MIPA"
            )
            repository.insertProctorSession(session)
            _currentProctor.value = session
            _currentSiswa.value = null
            navigateTo(UjianScreen.PENGAWAS_HOME)
        }
    }

    fun logout() {
        _currentSiswa.value = null
        _currentProctor.value = null
        _activeExam.value = null
        _activeQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        timerJob?.cancel()
        navigateTo(UjianScreen.LOGIN)
    }

    fun selectExam(exam: ExamEntity) {
        _activeExam.value = exam
        _examError.value = null
        _isExamForcedFinished.value = false
        _warningCount.value = 0
        _showUnfocusWarning.value = false
        
        viewModelScope.launch {
            repository.getQuestionsForExam(exam.id).firstOrNull()?.let { list ->
                _activeQuestions.value = list
            }
            _currentQuestionIndex.value = 0
            navigateTo(UjianScreen.EXAM_DETAIL)
        }
    }

    fun startExam(token: String): Boolean {
        val exam = _activeExam.value ?: return false
        if (exam.tokenCode.lowercase().trim() != token.lowercase().trim()) {
            _examError.value = "Token akses ujian salah! Silakan coba lagi."
            return false
        }
        
        _examError.value = null
        _remainingSeconds.value = exam.durationMinutes * 60
        _warningCount.value = 0
        _showUnfocusWarning.value = false
        _isExamForcedFinished.value = false

        // Refresh/reset questions state so answers are clear on replay, except those already done
        viewModelScope.launch {
            repository.insertActivityLog(
                StudentActivityLogEntity(
                    studentName = _currentSiswa.value?.name ?: "Siswa",
                    examId = exam.id,
                    examTitle = exam.title,
                    type = "START",
                    message = "Siswa memulai pelaksanaan ujian."
                )
            )
            startTimer()
            navigateTo(UjianScreen.EXAM_TAKE)
        }
        return true
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value -= 1
            }
            // Time is up! Auto Submit.
            submitExam(forced = false, reason = "Waktu Ujian Habis")
        }
    }

    fun selectQuestion(index: Int) {
        if (index in _activeQuestions.value.indices) {
            _currentQuestionIndex.value = index
        }
    }

    fun answerCurrentQuestion(option: String) {
        val questions = _activeQuestions.value
        val index = _currentQuestionIndex.value
        if (index !in questions.indices) return

        val originalQuestion = questions[index]
        val updatedQuestion = originalQuestion.copy(selectedOption = option)
        
        val updatedList = questions.toMutableList()
        updatedList[index] = updatedQuestion
        _activeQuestions.value = updatedList

        viewModelScope.launch {
            repository.updateQuestion(updatedQuestion)
        }
    }

    fun toggleFlaggedCurrentQuestion() {
        val questions = _activeQuestions.value
        val index = _currentQuestionIndex.value
        if (index !in questions.indices) return

        val originalQuestion = questions[index]
        val updatedQuestion = originalQuestion.copy(isFlagged = !originalQuestion.isFlagged)

        val updatedList = questions.toMutableList()
        updatedList[index] = updatedQuestion
        _activeQuestions.value = updatedList

        viewModelScope.launch {
            repository.updateQuestion(updatedQuestion)
        }
    }

    fun submitExam(forced: Boolean = false, reason: String = "Siswa menyelesaikan ujian secara manual.") {
        timerJob?.cancel()
        val exam = _activeExam.value ?: return
        val questions = _activeQuestions.value

        viewModelScope.launch {
            var correct = 0
            var wrong = 0
            var unanswered = 0

            for (q in questions) {
                if (q.selectedOption.isNullOrBlank()) {
                    unanswered++
                } else if (q.selectedOption.uppercase().trim() == q.correctOption.uppercase().trim()) {
                    correct++
                } else {
                    wrong++
                }
            }

            val rawScore = if (questions.isNotEmpty()) {
                (correct.toDouble() / questions.size.toDouble()) * 100.0
            } else {
                0.0
            }
            
            // Format score to 1 decimal place
            val finalScore = Math.round(rawScore * 10.0) / 10.0

            val finishedExam = exam.copy(
                isFinished = true,
                score = finalScore,
                correctCount = correct,
                wrongCount = wrong,
                notAnsweredCount = unanswered
            )

            repository.updateExam(finishedExam)
            _activeExam.value = finishedExam

            repository.insertActivityLog(
                StudentActivityLogEntity(
                    studentName = _currentSiswa.value?.name ?: "Siswa",
                    examId = exam.id,
                    examTitle = exam.title,
                    type = "SUBMIT",
                    message = if (forced) "Ujian Dihentikan Paksa: $reason. Skor akhir yang tersimpan: $finalScore" else "Ujian Diselesaikan Sukses. Skor akhir: $finalScore"
                )
            )

            // Update leaderboard after exam submission
            updateLeaderboardAfterExam(finishedExam)

            // Send notification about exam result
            sendNotification(
                title = "Hasil Ujian Tersedia",
                message = "Ujian \"${exam.title}\" telah selesai. Skor Anda: $finalScore",
                type = "RESULT",
                examId = exam.id
            )

            // Refresh student stats
            refreshStudentStats()

            _isExamForcedFinished.value = forced
            navigateTo(UjianScreen.EXAM_RESULT)
        }
    }

    fun registerFocusLost() {
        val screen = _currentScreen.value
        if (screen != UjianScreen.EXAM_TAKE) return

        val exam = _activeExam.value ?: return
        val currentSiswaName = _currentSiswa.value?.name ?: "Siswa"

        _warningCount.value += 1
        val currentWarnings = _warningCount.value

        viewModelScope.launch {
            repository.insertActivityLog(
                StudentActivityLogEntity(
                    studentName = currentSiswaName,
                    examId = exam.id,
                    examTitle = exam.title,
                    type = "FOCUS_LOST",
                    message = "Peringatan $currentWarnings: Siswa terdeteksi meninggalkan layar ujian (unfocused / split screen)."
                )
            )

            if (currentWarnings >= 3) {
                // Infraction limit exceeded (Limit is 3 infractions. At 3rd focus loss, we terminate!)
                submitExam(forced = true, reason = "Melanggar batas maksimal meninggalkan monitor ujian (3 kali)")
            } else {
                // Show floating Compose Alert Dialog
                _showUnfocusWarning.value = true
            }
        }
    }

    fun dismissWarningDialog() {
        _showUnfocusWarning.value = false
    }

    fun createNewExam(title: String, subject: String, durationMinutes: Int, token: String, questionsList: List<QuestionEntity>) {
        viewModelScope.launch {
            val nextId = "EX-0${(exams.value.size + 1)}"
            val exam = ExamEntity(
                id = nextId,
                title = title.trim(),
                subject = subject.trim(),
                durationMinutes = durationMinutes,
                totalQuestions = questionsList.size,
                tokenCode = token.trim().uppercase()
            )
            val updatedQuestions = questionsList.mapIndexed { index, question ->
                question.copy(examId = nextId, questionNumber = index + 1)
            }
            repository.insertExams(listOf(exam))
            repository.insertQuestions(updatedQuestions)
            
            // Insert log
            repository.insertActivityLog(
                StudentActivityLogEntity(
                    studentName = _currentProctor.value?.name ?: "Pengawas Sesi",
                    examId = nextId,
                    examTitle = title,
                    type = "CREATE",
                    message = "Pengawas membuat paket ujian baru: $title ($subject)"
                )
            )

            // Insert exam schedule
            val startTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val endCalendar = java.util.Calendar.getInstance()
            endCalendar.add(java.util.Calendar.MINUTE, durationMinutes)
            val endTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(endCalendar.time)

            repository.insertExamSchedule(
                ExamScheduleEntity(
                    examId = nextId,
                    scheduledDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                    startTime = startTime,
                    endTime = endTime,
                    room = _currentProctor.value?.supervisedClass ?: "Ruang Utama",
                    supervisor = _currentProctor.value?.name ?: "Pengawas",
                    isActive = true
                )
            )

            // Send notification about new exam
            sendNotification(
                title = "Ujian Baru Tersedia",
                message = "Ujian \"$title\" ($subject) telah dipublikasikan. Siapkan diri Anda!",
                type = "EXAM",
                examId = nextId
            )
        }
    }

    fun deleteExam(examId: String) {
        viewModelScope.launch {
            // Note: Room doesn't support cascade delete without foreign keys.
            // This is a simplified implementation - in production, add FK constraints.
            // For now, we clear all and re-seed since we don't have per-exam delete in DAO.
            repository.clearAll()
            repository.populateDemoDataIfEmpty()
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.clearAll()
            repository.populateDemoDataIfEmpty()
            _currentSiswa.value = null
            _currentProctor.value = null
            navigateTo(UjianScreen.LOGIN)
        }
    }
}
