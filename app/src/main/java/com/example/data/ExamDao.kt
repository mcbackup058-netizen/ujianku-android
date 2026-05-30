package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM siswa LIMIT 1")
    fun getSiswaDefault(): Flow<SiswaEntity?>

    @Query("SELECT * FROM siswa WHERE nisn = :nisn LIMIT 1")
    suspend fun getSiswaByNisn(nisn: String): SiswaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSiswa(siswa: SiswaEntity)

    @Query("SELECT * FROM exams")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    fun getExamById(id: String): Flow<ExamEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>)

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Query("SELECT * FROM questions WHERE exam_id = :examId ORDER BY question_number ASC")
    fun getQuestionsForExam(examId: String): Flow<List<QuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: StudentActivityLogEntity)

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivityLogs(): Flow<List<StudentActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE exam_id = :examId ORDER BY timestamp DESC")
    fun getActivityLogsForExam(examId: String): Flow<List<StudentActivityLogEntity>>

    @Query("SELECT * FROM proctor_sessions LIMIT 1")
    fun getProctorSessionDefault(): Flow<ProctorSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProctorSession(session: ProctorSessionEntity)

    @Query("DELETE FROM questions")
    suspend fun clearQuestions()

    @Query("DELETE FROM exams")
    suspend fun clearExams()

    @Query("DELETE FROM activity_logs")
    suspend fun clearActivityLogs()

    // Leaderboard queries
    @Query("SELECT * FROM leaderboard ORDER BY score DESC")
    fun getAllLeaderboard(): Flow<List<LeaderboardEntity>>

    @Query("SELECT * FROM leaderboard WHERE exam_id = :examId ORDER BY score DESC")
    fun getLeaderboardForExam(examId: String): Flow<List<LeaderboardEntity>>

    @Query("SELECT * FROM leaderboard WHERE student_nisn = :nisn ORDER BY score DESC")
    fun getLeaderboardForStudent(nisn: String): Flow<List<LeaderboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntries(entries: List<LeaderboardEntity>)

    // Student stats
    @Query("SELECT * FROM student_stats WHERE student_nisn = :nisn LIMIT 1")
    fun getStudentStats(nisn: String): Flow<StudentStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentStats(stats: StudentStatsEntity)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY created_at DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE is_read = 0 ORDER BY created_at DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :notificationId")
    suspend fun markNotificationAsRead(notificationId: Int)

    @Query("SELECT COUNT(*) FROM notifications WHERE is_read = 0")
    fun getUnreadNotificationCount(): Flow<Int>

    // Exam schedules
    @Query("SELECT * FROM exam_schedules WHERE exam_id = :examId")
    fun getSchedulesForExam(examId: String): Flow<List<ExamScheduleEntity>>

    @Query("SELECT * FROM exam_schedules WHERE is_active = 1 ORDER BY scheduled_date ASC, start_time ASC")
    fun getActiveSchedules(): Flow<List<ExamScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamSchedule(schedule: ExamScheduleEntity)

    // Get all siswa
    @Query("SELECT * FROM siswa")
    fun getAllSiswa(): Flow<List<SiswaEntity>>

    // Get finished exams for student stats
    @Query("SELECT * FROM exams WHERE is_finished = 1")
    fun getFinishedExams(): Flow<List<ExamEntity>>

    // Clear siswa
    @Query("DELETE FROM siswa")
    suspend fun clearSiswa()

    // Clear leaderboard
    @Query("DELETE FROM leaderboard")
    suspend fun clearLeaderboard()
}
