package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "siswa")
data class SiswaEntity(
    @PrimaryKey val nisn: String,
    val name: String,
    val kelas: String,
    val sekolah: String
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
    @ColumnInfo(name = "total_questions") val totalQuestions: Int,
    @ColumnInfo(name = "token_code") val tokenCode: String,
    @ColumnInfo(name = "is_finished") val isFinished: Boolean = false,
    val score: Double = 0.0,
    @ColumnInfo(name = "correct_count") val correctCount: Int = 0,
    @ColumnInfo(name = "wrong_count") val wrongCount: Int = 0,
    @ColumnInfo(name = "not_answered_count") val notAnsweredCount: Int = 0
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "exam_id") val examId: String,
    @ColumnInfo(name = "question_number") val questionNumber: Int,
    val text: String,
    @ColumnInfo(name = "option_a") val optionA: String,
    @ColumnInfo(name = "option_b") val optionB: String,
    @ColumnInfo(name = "option_c") val optionC: String,
    @ColumnInfo(name = "option_d") val optionD: String,
    @ColumnInfo(name = "option_e") val optionE: String,
    @ColumnInfo(name = "correct_option") val correctOption: String, // "A", "B", "C", "D", "E"
    @ColumnInfo(name = "selected_option") val selectedOption: String? = null, // "A" - "E"
    @ColumnInfo(name = "is_flagged") val isFlagged: Boolean = false // Ragu-ragu state
)

@Entity(tableName = "activity_logs")
data class StudentActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "student_name") val studentName: String,
    @ColumnInfo(name = "exam_id") val examId: String,
    @ColumnInfo(name = "exam_title") val examTitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "FOCUS_LOST", "SUBMIT", "START", "WARN"
    val message: String
)

@Entity(tableName = "proctor_sessions")
data class ProctorSessionEntity(
    @PrimaryKey val nip: String,
    val name: String,
    val school: String,
    @ColumnInfo(name = "supervised_class") val supervisedClass: String
)

// Leaderboard entry - tracks student rankings
@Entity(tableName = "leaderboard")
data class LeaderboardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "student_nisn") val studentNisn: String,
    @ColumnInfo(name = "student_name") val studentName: String,
    @ColumnInfo(name = "exam_id") val examId: String,
    @ColumnInfo(name = "exam_title") val examTitle: String,
    val score: Double,
    @ColumnInfo(name = "rank_position") val rankPosition: Int = 0,
    @ColumnInfo(name = "total_participants") val totalParticipants: Int = 0,
    @ColumnInfo(name = "submitted_at") val submittedAt: Long = System.currentTimeMillis()
)

// Student profile statistics
@Entity(tableName = "student_stats")
data class StudentStatsEntity(
    @PrimaryKey @ColumnInfo(name = "student_nisn") val studentNisn: String,
    @ColumnInfo(name = "total_exams_taken") val totalExamsTaken: Int = 0,
    @ColumnInfo(name = "average_score") val averageScore: Double = 0.0,
    @ColumnInfo(name = "highest_score") val highestScore: Double = 0.0,
    @ColumnInfo(name = "total_violations") val totalViolations: Int = 0,
    @ColumnInfo(name = "last_exam_date") val lastExamDate: Long = 0L,
    @ColumnInfo(name = "study_streak_days") val studyStreakDays: Int = 0
)

// Notification entity
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val type: String, // "RESULT", "REMINDER", "VIOLATION", "ANNOUNCEMENT"
    @ColumnInfo(name = "is_read") val isRead: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "related_exam_id") val relatedExamId: String? = null
)

// Exam schedule
@Entity(tableName = "exam_schedules")
data class ExamScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "exam_id") val examId: String,
    @ColumnInfo(name = "scheduled_date") val scheduledDate: String, // ISO date string
    @ColumnInfo(name = "start_time") val startTime: String, // HH:mm format
    @ColumnInfo(name = "end_time") val endTime: String,
    @ColumnInfo(name = "room") val room: String,
    @ColumnInfo(name = "supervisor") val supervisor: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)
