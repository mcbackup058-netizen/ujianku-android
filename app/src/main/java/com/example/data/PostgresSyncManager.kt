package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Statement
import java.util.Properties

enum class SyncStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    SYNCING,
    SYNC_SUCCESS,
    ERROR
}

class PostgresSyncManager private constructor() {

    // Database credentials loaded from BuildConfig (injected from .env via Secrets plugin)
    // No hardcoded fallback URL — the app must be configured with a .env file
    private val dbUrl: String
    private val dbUser: String
    private val dbPassword: String

    // Track last sync timestamp for incremental upserts
    private var lastSyncTimestamp: Long = 0L

    private val _syncStatus = MutableStateFlow(SyncStatus.DISCONNECTED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow("Belum tersinkronisasi dengan Supabase Cloud.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    companion object {
        private const val TAG = "PostgresSyncManager"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
        private const val CONNECTION_TIMEOUT_SECONDS = 15
        private const val SOCKET_TIMEOUT_SECONDS = 30

        @Volatile
        private var INSTANCE: PostgresSyncManager? = null

        fun getInstance(): PostgresSyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PostgresSyncManager()
                INSTANCE = instance
                instance
            }
        }
    }

    init {
        val rawUrl = com.example.BuildConfig.DB_URL ?: ""
        val rawUser = com.example.BuildConfig.DB_USER ?: ""
        val rawPassword = com.example.BuildConfig.DB_PASSWORD ?: ""

        // Append SSL mode and timeout parameters to the JDBC URL for Supabase
        dbUrl = buildJdbcUrl(rawUrl)
        dbUser = rawUser
        dbPassword = rawPassword
    }

    /**
     * Build the JDBC URL with SSL and timeout parameters required for Supabase.
     * Supabase requires SSL (sslmode=require) and benefits from connection timeouts.
     */
    private fun buildJdbcUrl(rawUrl: String): String {
        if (rawUrl.isBlank()) return rawUrl

        val separator = if ("?" in rawUrl) "&" else "?"
        return buildString {
            append(rawUrl)
            append(separator)
            append("sslmode=require")
            append("&connectTimeout=$CONNECTION_TIMEOUT_SECONDS")
            append("&socketTimeout=$SOCKET_TIMEOUT_SECONDS")
            append("&tcpKeepAlive=true")
            append("&reWriteBatchedInserts=true")
        }
    }

    /**
     * Establish a connection to the PostgreSQL database with retry logic.
     */
    private suspend fun getConnectionWithRetry(retries: Int = MAX_RETRIES): Connection? {
        repeat(retries) { attempt ->
            try {
                return getConnection()
            } catch (e: Exception) {
                Log.w(TAG, "Connection attempt ${attempt + 1}/$retries failed: ${e.message}")
                if (attempt < retries - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        return null
    }

    private suspend fun getConnection(): Connection? = withContext(Dispatchers.IO) {
        try {
            if (dbUrl.isBlank() || dbUser.isBlank()) {
                Log.e(TAG, "Database credentials not configured. Create a .env file with DB_URL, DB_USER, DB_PASSWORD.")
                return@withContext null
            }
            Class.forName("org.postgresql.Driver")

            val props = Properties().apply {
                put("user", dbUser)
                put("password", dbPassword)
                put("loginTimeout", CONNECTION_TIMEOUT_SECONDS.toString())
                put("socketTimeout", SOCKET_TIMEOUT_SECONDS.toString())
                put("tcpKeepAlive", "true")
                put("sslmode", "require")
            }

            DriverManager.getConnection(dbUrl, props)
        } catch (e: Exception) {
            Log.e(TAG, "Koneksi PostgreSQL gagal: ${e.message}", e)
            null
        }
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.CONNECTING
        _statusMessage.value = "Menghubungkan ke PostgreSQL di Supabase..."
        val conn = getConnectionWithRetry()
        if (conn != null) {
            try {
                conn.close()
                _syncStatus.value = SyncStatus.CONNECTED
                _statusMessage.value = "Terhubung dengan Supabase Cloud!"
                true
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                _statusMessage.value = "Gagal memverifikasi koneksi: ${e.message}"
                false
            }
        } else {
            _syncStatus.value = SyncStatus.ERROR
            _statusMessage.value = "Koneksi internet atau database tersumbat."
            false
        }
    }

    /**
     * Initialize cloud tables using an existing connection if provided,
     * otherwise opens a new one.
     */
    private suspend fun initializeCloudTables(conn: Connection? = null): Boolean = withContext(Dispatchers.IO) {
        val ownConnection = conn == null
        val c = conn ?: getConnection() ?: return@withContext false
        var stmt: Statement? = null
        try {
            stmt = c.createStatement()

            // Siswa table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS siswa (
                    nisn VARCHAR(100) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    kelas VARCHAR(100) NOT NULL,
                    sekolah VARCHAR(255) NOT NULL
                )
            """.trimIndent())

            // Exams table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS exams (
                    id VARCHAR(100) PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    subject VARCHAR(100) NOT NULL,
                    duration_minutes INT NOT NULL,
                    total_questions INT NOT NULL,
                    token_code VARCHAR(100) NOT NULL,
                    is_finished BOOLEAN DEFAULT FALSE,
                    score DOUBLE PRECISION DEFAULT 0.0,
                    correct_count INT DEFAULT 0,
                    wrong_count INT DEFAULT 0,
                    not_answered_count INT DEFAULT 0
                )
            """.trimIndent())

            // Questions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS questions (
                    id SERIAL PRIMARY KEY,
                    exam_id VARCHAR(100) NOT NULL,
                    question_number INT NOT NULL,
                    text TEXT NOT NULL,
                    option_a TEXT NOT NULL,
                    option_b TEXT NOT NULL,
                    option_c TEXT NOT NULL,
                    option_d TEXT NOT NULL,
                    option_e TEXT NOT NULL,
                    correct_option VARCHAR(10) NOT NULL,
                    selected_option VARCHAR(10),
                    is_flagged BOOLEAN DEFAULT FALSE
                )
            """.trimIndent())

            // Activity Logs table — add updated_at column for incremental sync
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id SERIAL PRIMARY KEY,
                    student_name VARCHAR(255) NOT NULL,
                    exam_id VARCHAR(100) NOT NULL,
                    exam_title VARCHAR(255) NOT NULL,
                    timestamp BIGINT NOT NULL,
                    type VARCHAR(100) NOT NULL,
                    message TEXT NOT NULL,
                    updated_at BIGINT DEFAULT 0
                )
            """.trimIndent())

            // Add updated_at column if it doesn't exist (for existing databases)
            try {
                stmt.execute("ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS updated_at BIGINT DEFAULT 0")
            } catch (e: Exception) {
                Log.w(TAG, "Could not add updated_at column (may already exist): ${e.message}")
            }

            // Proctor Session table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS proctor_sessions (
                    nip VARCHAR(100) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    school VARCHAR(255) NOT NULL,
                    supervised_class VARCHAR(255) NOT NULL
                )
            """.trimIndent())

            // Leaderboard table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS leaderboard (
                    id SERIAL PRIMARY KEY,
                    student_nisn VARCHAR(100) NOT NULL,
                    student_name VARCHAR(255) NOT NULL,
                    exam_id VARCHAR(100) NOT NULL,
                    exam_title VARCHAR(255) NOT NULL,
                    score DOUBLE PRECISION NOT NULL,
                    rank_position INT DEFAULT 0,
                    total_participants INT DEFAULT 0,
                    submitted_at BIGINT DEFAULT 0
                )
            """.trimIndent())

            // Student Stats table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS student_stats (
                    student_nisn VARCHAR(100) PRIMARY KEY,
                    total_exams_taken INT DEFAULT 0,
                    average_score DOUBLE PRECISION DEFAULT 0.0,
                    highest_score DOUBLE PRECISION DEFAULT 0.0,
                    total_violations INT DEFAULT 0,
                    last_exam_date BIGINT DEFAULT 0,
                    study_streak_days INT DEFAULT 0
                )
            """.trimIndent())

            // Notifications table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id SERIAL PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    message TEXT NOT NULL,
                    type VARCHAR(50) NOT NULL,
                    is_read BOOLEAN DEFAULT FALSE,
                    created_at BIGINT DEFAULT 0,
                    related_exam_id VARCHAR(100)
                )
            """.trimIndent())

            // Exam Schedules table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS exam_schedules (
                    id SERIAL PRIMARY KEY,
                    exam_id VARCHAR(100) NOT NULL,
                    scheduled_date VARCHAR(20) NOT NULL,
                    start_time VARCHAR(10) NOT NULL,
                    end_time VARCHAR(10) NOT NULL,
                    room VARCHAR(255) NOT NULL,
                    supervisor VARCHAR(255) NOT NULL,
                    is_active BOOLEAN DEFAULT TRUE
                )
            """.trimIndent())

            true
        } catch (e: Exception) {
            Log.e(TAG, "Inisialisasi tabel awan gagal: ${e.message}", e)
            false
        } finally {
            try { stmt?.close() } catch (ignored: Exception) {}
            if (ownConnection) {
                try { c.close() } catch (ignored: Exception) {}
            }
        }
    }

    /**
     * Dual-direction synchronization with incremental upsert logic:
     * 1. Pull new exams and questions created on Supabase Cloud to local Room DB.
     * 2. Push student registrations, results, and logs from local Room DB to Supabase Cloud.
     * Uses timestamp comparison for incremental syncs instead of destructive TRUNCATE.
     */
    suspend fun syncDatabase(examDao: ExamDao): Boolean = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.SYNCING
        _statusMessage.value = "Memulai sinkronisasi dua arah..."

        val conn = getConnectionWithRetry()
        if (conn == null) {
            _syncStatus.value = SyncStatus.ERROR
            _statusMessage.value = "Koneksi gagal saat sinkronisasi. Pastikan .env sudah dikonfigurasi."
            return@withContext false
        }

        try {
            // Ensure cloud tables exist (reuse existing connection)
            val setupOk = initializeCloudTables(conn)
            if (!setupOk) {
                _syncStatus.value = SyncStatus.ERROR
                _statusMessage.value = "Gagal menginisialisasi skema cloud."
                return@withContext false
            }

            // Record the start of this sync for incremental tracking
            val syncStartTime = System.currentTimeMillis()

            // ==========================================
            // A. PULL PHASE: Get cloud exams and questions
            // ==========================================
            _statusMessage.value = "Mengunduh paket ujian dari Supabase..."
            val pulledExams = mutableListOf<ExamEntity>()
            val pulledQuestions = mutableListOf<QuestionEntity>()

            // Read exams
            try {
                val examStmt = conn.createStatement()
                val examRs = examStmt.executeQuery("SELECT * FROM exams")
                while (examRs.next()) {
                    pulledExams.add(
                        ExamEntity(
                            id = examRs.getString("id"),
                            title = examRs.getString("title"),
                            subject = examRs.getString("subject"),
                            durationMinutes = examRs.getInt("duration_minutes"),
                            totalQuestions = examRs.getInt("total_questions"),
                            tokenCode = examRs.getString("token_code"),
                            isFinished = examRs.getBoolean("is_finished"),
                            score = examRs.getDouble("score"),
                            correctCount = examRs.getInt("correct_count"),
                            wrongCount = examRs.getInt("wrong_count"),
                            notAnsweredCount = examRs.getInt("not_answered_count")
                        )
                    )
                }
                examRs.close()
                examStmt.close()
            } catch (e: Exception) {
                Log.e(TAG, "Gagal membaca data ujian dari cloud: ${e.message}", e)
            }

            // Read questions
            try {
                val qStmt = conn.createStatement()
                val qRs = qStmt.executeQuery("SELECT * FROM questions")
                while (qRs.next()) {
                    pulledQuestions.add(
                        QuestionEntity(
                            id = qRs.getInt("id"),
                            examId = qRs.getString("exam_id"),
                            questionNumber = qRs.getInt("question_number"),
                            text = qRs.getString("text"),
                            optionA = qRs.getString("option_a"),
                            optionB = qRs.getString("option_b"),
                            optionC = qRs.getString("option_c"),
                            optionD = qRs.getString("option_d"),
                            optionE = qRs.getString("option_e"),
                            correctOption = qRs.getString("correct_option"),
                            selectedOption = qRs.getString("selected_option"),
                            isFlagged = qRs.getBoolean("is_flagged")
                        )
                    )
                }
                qRs.close()
                qStmt.close()
            } catch (e: Exception) {
                Log.e(TAG, "Gagal membaca data pertanyaan dari cloud: ${e.message}", e)
            }

            // Apply to Room Local DB
            try {
                if (pulledExams.isNotEmpty()) {
                    examDao.insertExams(pulledExams)
                }
                if (pulledQuestions.isNotEmpty()) {
                    examDao.insertQuestions(pulledQuestions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menyimpan data ke Room DB lokal: ${e.message}", e)
            }

            // ==========================================
            // B. PUSH PHASE: Sync local data to cloud
            // ==========================================
            _statusMessage.value = "Mengunggah data lokal ke Supabase..."

            // 1. Sync siswa tables
            try {
                val localSiswa = examDao.getSiswaDefault().firstOrNull()
                if (localSiswa != null) {
                    val siswaPs: PreparedStatement = conn.prepareStatement("""
                        INSERT INTO siswa(nisn, name, kelas, sekolah)
                        VALUES(?, ?, ?, ?)
                        ON CONFLICT(nisn) DO UPDATE SET name = EXCLUDED.name, kelas = EXCLUDED.kelas, sekolah = EXCLUDED.sekolah
                    """.trimIndent())
                    siswaPs.setString(1, localSiswa.nisn)
                    siswaPs.setString(2, localSiswa.name)
                    siswaPs.setString(3, localSiswa.kelas)
                    siswaPs.setString(4, localSiswa.sekolah)
                    siswaPs.executeUpdate()
                    siswaPs.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengunggah data siswa: ${e.message}", e)
            }

            // 2. Sync proctor sessions
            try {
                val localProctor = examDao.getProctorSessionDefault().firstOrNull()
                if (localProctor != null) {
                    val proctorPs = conn.prepareStatement("""
                        INSERT INTO proctor_sessions(nip, name, school, supervised_class)
                        VALUES(?, ?, ?, ?)
                        ON CONFLICT(nip) DO UPDATE SET name = EXCLUDED.name, school = EXCLUDED.school, supervised_class = EXCLUDED.supervised_class
                    """.trimIndent())
                    proctorPs.setString(1, localProctor.nip)
                    proctorPs.setString(2, localProctor.name)
                    proctorPs.setString(3, localProctor.school)
                    proctorPs.setString(4, localProctor.supervisedClass)
                    proctorPs.executeUpdate()
                    proctorPs.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengunggah data pengawas: ${e.message}", e)
            }

            // 3. Sync finished/updated exams from local back to cloud (upsert)
            try {
                val localExamsList = examDao.getAllExams().firstOrNull() ?: emptyList()
                for (exam in localExamsList) {
                    val examPs = conn.prepareStatement("""
                        INSERT INTO exams(id, title, subject, duration_minutes, total_questions, token_code, is_finished, score, correct_count, wrong_count, not_answered_count)
                        VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET title = EXCLUDED.title, subject = EXCLUDED.subject, 
                            is_finished = EXCLUDED.is_finished, score = EXCLUDED.score, 
                            correct_count = EXCLUDED.correct_count, wrong_count = EXCLUDED.wrong_count,
                            not_answered_count = EXCLUDED.not_answered_count
                    """.trimIndent())
                    examPs.setString(1, exam.id)
                    examPs.setString(2, exam.title)
                    examPs.setString(3, exam.subject)
                    examPs.setInt(4, exam.durationMinutes)
                    examPs.setInt(5, exam.totalQuestions)
                    examPs.setString(6, exam.tokenCode)
                    examPs.setBoolean(7, exam.isFinished)
                    examPs.setDouble(8, exam.score)
                    examPs.setInt(9, exam.correctCount)
                    examPs.setInt(10, exam.wrongCount)
                    examPs.setInt(11, exam.notAnsweredCount)
                    examPs.executeUpdate()
                    examPs.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengunggah data ujian lokal: ${e.message}", e)
            }

            // 4. Incremental upsert of activity logs using timestamp comparison
            // Replaces the destructive TRUNCATE approach — only pushes new/updated logs
            try {
                val localLogsList = examDao.getAllActivityLogs().firstOrNull() ?: emptyList()
                val newOrUpdatedLogs = if (lastSyncTimestamp > 0) {
                    // Only push logs that were created/updated after last sync
                    localLogsList.filter { it.timestamp > lastSyncTimestamp }
                } else {
                    // First sync — push all logs
                    localLogsList
                }

                for (log in newOrUpdatedLogs) {
                    val logPs = conn.prepareStatement("""
                        INSERT INTO activity_logs(student_name, exam_id, exam_title, timestamp, type, message, updated_at)
                        VALUES(?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT DO NOTHING
                    """.trimIndent())
                    logPs.setString(1, log.studentName)
                    logPs.setString(2, log.examId)
                    logPs.setString(3, log.examTitle)
                    logPs.setLong(4, log.timestamp)
                    logPs.setString(5, log.type)
                    logPs.setString(6, log.message)
                    logPs.setLong(7, syncStartTime)
                    logPs.executeUpdate()
                    logPs.close()
                }
                Log.i(TAG, "Incremental activity log sync: pushed ${newOrUpdatedLogs.size} logs (of ${localLogsList.size} total)")
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengunggah log aktivitas: ${e.message}", e)
            }

            // 5. Bidirectional question sync — push local questions to cloud, pull cloud questions
            syncQuestions(conn, examDao)

            // Update the last sync timestamp after successful sync
            lastSyncTimestamp = syncStartTime

            _syncStatus.value = SyncStatus.SYNC_SUCCESS
            _statusMessage.value = "Sinkronisasi Supabase Berhasil! ${pulledExams.size} Ujian & ${pulledQuestions.size} Pertanyaan Ditarik."
            true
        } catch (e: Exception) {
            Log.e(TAG, "Proses sinkronisasi database gagal: ${e.message}", e)
            _syncStatus.value = SyncStatus.ERROR
            _statusMessage.value = "Error Sinkronisasi: ${e.message}"
            false
        } finally {
            try { conn.close() } catch (ignored: Exception) {}
        }
    }

    /**
     * Bidirectional sync of questions:
     * - Push new questions from local Room DB to cloud that don't exist in cloud yet.
     * - Pull new questions from cloud that don't exist in local yet.
     */
    private suspend fun syncQuestions(conn: Connection, examDao: ExamDao) = withContext(Dispatchers.IO) {
        // Push local questions that are not yet in the cloud
        try {
            // Get all local questions across all exams
            val allExams = examDao.getAllExams().firstOrNull() ?: emptyList()
            val allLocalQuestions = mutableListOf<QuestionEntity>()
            for (exam in allExams) {
                val questions = examDao.getQuestionsForExam(exam.id).firstOrNull() ?: emptyList()
                allLocalQuestions.addAll(questions)
            }

            // Get existing cloud question IDs
            val cloudQuestionIds = mutableSetOf<Int>()
            val idStmt = conn.createStatement()
            val idRs = idStmt.executeQuery("SELECT id FROM questions")
            while (idRs.next()) {
                cloudQuestionIds.add(idRs.getInt("id"))
            }
            idRs.close()
            idStmt.close()

            // Push questions that don't exist in cloud
            var pushedCount = 0
            for (q in allLocalQuestions) {
                if (q.id != 0 && q.id !in cloudQuestionIds) {
                    val qPs = conn.prepareStatement("""
                        INSERT INTO questions(id, exam_id, question_number, text, option_a, option_b, option_c, option_d, option_e, correct_option, selected_option, is_flagged)
                        VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT DO NOTHING
                    """.trimIndent())
                    qPs.setInt(1, q.id)
                    qPs.setString(2, q.examId)
                    qPs.setInt(3, q.questionNumber)
                    qPs.setString(4, q.text)
                    qPs.setString(5, q.optionA)
                    qPs.setString(6, q.optionB)
                    qPs.setString(7, q.optionC)
                    qPs.setString(8, q.optionD)
                    qPs.setString(9, q.optionE)
                    qPs.setString(10, q.correctOption)
                    qPs.setString(11, q.selectedOption)
                    qPs.setBoolean(12, q.isFlagged)
                    qPs.executeUpdate()
                    qPs.close()
                    pushedCount++
                }
            }
            Log.i(TAG, "Pushed $pushedCount new questions to cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengunggah pertanyaan lokal: ${e.message}", e)
        }

        // Pull cloud questions that may have been updated (with selected_option changes)
        try {
            val pullStmt = conn.createStatement()
            val pullRs = pullStmt.executeQuery(
                "SELECT * FROM questions WHERE selected_option IS NOT NULL"
            )
            val updatedQuestions = mutableListOf<QuestionEntity>()
            while (pullRs.next()) {
                val selectedOpt = pullRs.getString("selected_option")
                if (selectedOpt != null) {
                    updatedQuestions.add(
                        QuestionEntity(
                            id = pullRs.getInt("id"),
                            examId = pullRs.getString("exam_id"),
                            questionNumber = pullRs.getInt("question_number"),
                            text = pullRs.getString("text"),
                            optionA = pullRs.getString("option_a"),
                            optionB = pullRs.getString("option_b"),
                            optionC = pullRs.getString("option_c"),
                            optionD = pullRs.getString("option_d"),
                            optionE = pullRs.getString("option_e"),
                            correctOption = pullRs.getString("correct_option"),
                            selectedOption = selectedOpt,
                            isFlagged = pullRs.getBoolean("is_flagged")
                        )
                    )
                }
            }
            pullRs.close()
            pullStmt.close()

            if (updatedQuestions.isNotEmpty()) {
                examDao.insertQuestions(updatedQuestions)
                Log.i(TAG, "Pulled ${updatedQuestions.size} answered questions from cloud")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menarik pertanyaan dari cloud: ${e.message}", e)
        }
    }

    /**
     * Push a finished exam result to the cloud database.
     * This is called when a student completes an exam.
     */
    suspend fun pushExamResult(exam: ExamEntity, examDao: ExamDao): Boolean = withContext(Dispatchers.IO) {
        if (!exam.isFinished) {
            Log.w(TAG, "Cannot push unfinished exam result: ${exam.id}")
            return@withContext false
        }

        val conn = getConnectionWithRetry()
        if (conn == null) {
            Log.e(TAG, "Gagal terhubung ke cloud untuk mengunggah hasil ujian.")
            return@withContext false
        }

        try {
            // Upsert exam result
            val examPs = conn.prepareStatement("""
                INSERT INTO exams(id, title, subject, duration_minutes, total_questions, token_code, is_finished, score, correct_count, wrong_count, not_answered_count)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET 
                    is_finished = EXCLUDED.is_finished, 
                    score = EXCLUDED.score, 
                    correct_count = EXCLUDED.correct_count, 
                    wrong_count = EXCLUDED.wrong_count,
                    not_answered_count = EXCLUDED.not_answered_count
            """.trimIndent())
            examPs.setString(1, exam.id)
            examPs.setString(2, exam.title)
            examPs.setString(3, exam.subject)
            examPs.setInt(4, exam.durationMinutes)
            examPs.setInt(5, exam.totalQuestions)
            examPs.setString(6, exam.tokenCode)
            examPs.setBoolean(7, exam.isFinished)
            examPs.setDouble(8, exam.score)
            examPs.setInt(9, exam.correctCount)
            examPs.setInt(10, exam.wrongCount)
            examPs.setInt(11, exam.notAnsweredCount)
            examPs.executeUpdate()
            examPs.close()

            // Also push the answered questions for this exam
            try {
                val questions = examDao.getQuestionsForExam(exam.id).firstOrNull() ?: emptyList()
                for (q in questions) {
                    val qPs = conn.prepareStatement("""
                        INSERT INTO questions(id, exam_id, question_number, text, option_a, option_b, option_c, option_d, option_e, correct_option, selected_option, is_flagged)
                        VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET 
                            selected_option = EXCLUDED.selected_option,
                            is_flagged = EXCLUDED.is_flagged
                    """.trimIndent())
                    qPs.setInt(1, q.id)
                    qPs.setString(2, q.examId)
                    qPs.setInt(3, q.questionNumber)
                    qPs.setString(4, q.text)
                    qPs.setString(5, q.optionA)
                    qPs.setString(6, q.optionB)
                    qPs.setString(7, q.optionC)
                    qPs.setString(8, q.optionD)
                    qPs.setString(9, q.optionE)
                    qPs.setString(10, q.correctOption)
                    qPs.setString(11, q.selectedOption)
                    qPs.setBoolean(12, q.isFlagged)
                    qPs.executeUpdate()
                    qPs.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengunggah jawaban pertanyaan untuk ujian ${exam.id}: ${e.message}", e)
            }

            // Push activity logs for this exam
            try {
                val logs = examDao.getActivityLogsForExam(exam.id).firstOrNull() ?: emptyList()
                val now = System.currentTimeMillis()
                for (log in logs) {
                    val logPs = conn.prepareStatement("""
                        INSERT INTO activity_logs(student_name, exam_id, exam_title, timestamp, type, message, updated_at)
                        VALUES(?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT DO NOTHING
                    """.trimIndent())
                    logPs.setString(1, log.studentName)
                    logPs.setString(2, log.examId)
                    logPs.setString(3, log.examTitle)
                    logPs.setLong(4, log.timestamp)
                    logPs.setString(5, log.type)
                    logPs.setString(6, log.message)
                    logPs.setLong(7, now)
                    logPs.executeUpdate()
                    logPs.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengunggah log aktivitas untuk ujian ${exam.id}: ${e.message}", e)
            }

            Log.i(TAG, "Hasil ujian berhasil diunggah: ${exam.id} (skor: ${exam.score})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengunggah hasil ujian: ${e.message}", e)
            false
        } finally {
            try { conn.close() } catch (ignored: Exception) {}
        }
    }
}
