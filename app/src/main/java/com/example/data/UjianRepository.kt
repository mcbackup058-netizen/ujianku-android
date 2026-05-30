package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class UjianRepository(private val examDao: ExamDao) {

    val siswa: Flow<SiswaEntity?> = examDao.getSiswaDefault()
    val allExams: Flow<List<ExamEntity>> = examDao.getAllExams()
    val proctorSession: Flow<ProctorSessionEntity?> = examDao.getProctorSessionDefault()
    val allActivityLogs: Flow<List<StudentActivityLogEntity>> = examDao.getAllActivityLogs()

    suspend fun getSiswaByNisn(nisn: String): SiswaEntity? {
        return examDao.getSiswaByNisn(nisn)
    }

    suspend fun insertSiswa(siswa: SiswaEntity) {
        examDao.insertSiswa(siswa)
    }

    fun getExamById(id: String): Flow<ExamEntity?> {
        return examDao.getExamById(id)
    }

    suspend fun insertExams(exams: List<ExamEntity>) {
        examDao.insertExams(exams)
    }

    suspend fun updateExam(exam: ExamEntity) {
        examDao.updateExam(exam)
    }

    fun getQuestionsForExam(examId: String): Flow<List<QuestionEntity>> {
        return examDao.getQuestionsForExam(examId)
    }

    suspend fun insertQuestions(questions: List<QuestionEntity>) {
        examDao.insertQuestions(questions)
    }

    suspend fun updateQuestion(question: QuestionEntity) {
        examDao.updateQuestion(question)
    }

    suspend fun insertActivityLog(log: StudentActivityLogEntity) {
        examDao.insertActivityLog(log)
    }

    fun getActivityLogsForExam(examId: String): Flow<List<StudentActivityLogEntity>> {
        return examDao.getActivityLogsForExam(examId)
    }

    suspend fun insertProctorSession(session: ProctorSessionEntity) {
        examDao.insertProctorSession(session)
    }

    // Leaderboard
    val allLeaderboard: Flow<List<LeaderboardEntity>> = examDao.getAllLeaderboard()
    fun getLeaderboardForExam(examId: String): Flow<List<LeaderboardEntity>> = examDao.getLeaderboardForExam(examId)
    fun getLeaderboardForStudent(nisn: String): Flow<List<LeaderboardEntity>> = examDao.getLeaderboardForStudent(nisn)
    suspend fun insertLeaderboardEntries(entries: List<LeaderboardEntity>) = examDao.insertLeaderboardEntries(entries)

    // Student stats
    fun getStudentStats(nisn: String): Flow<StudentStatsEntity?> = examDao.getStudentStats(nisn)
    suspend fun insertStudentStats(stats: StudentStatsEntity) = examDao.insertStudentStats(stats)

    // Notifications
    val allNotifications: Flow<List<NotificationEntity>> = examDao.getAllNotifications()
    val unreadNotifications: Flow<List<NotificationEntity>> = examDao.getUnreadNotifications()
    fun getUnreadNotificationCount(): Flow<Int> = examDao.getUnreadNotificationCount()
    suspend fun insertNotification(notification: NotificationEntity) = examDao.insertNotification(notification)
    suspend fun markNotificationAsRead(notificationId: Int) = examDao.markNotificationAsRead(notificationId)

    // Exam schedules
    fun getActiveSchedules(): Flow<List<ExamScheduleEntity>> = examDao.getActiveSchedules()
    suspend fun insertExamSchedule(schedule: ExamScheduleEntity) = examDao.insertExamSchedule(schedule)

    // Update clearAll to include new tables
    suspend fun clearAll() {
        examDao.clearExams()
        examDao.clearQuestions()
        examDao.clearActivityLogs()
        examDao.clearLeaderboard()
    }

    // Calculate and save student stats
    suspend fun calculateStudentStats(nisn: String) {
        val finishedExams = examDao.getFinishedExams().firstOrNull()?.filter { it.isFinished } ?: emptyList()
        val activityLogs = examDao.getAllActivityLogs().firstOrNull() ?: emptyList()
        val violations = activityLogs.count { it.type == "FOCUS_LOST" }

        val stats = StudentStatsEntity(
            studentNisn = nisn,
            totalExamsTaken = finishedExams.size,
            averageScore = if (finishedExams.isNotEmpty()) finishedExams.map { it.score }.average() else 0.0,
            highestScore = finishedExams.maxOfOrNull { it.score } ?: 0.0,
            totalViolations = violations,
            lastExamDate = finishedExams.maxOfOrNull { System.currentTimeMillis() } ?: 0L,
            studyStreakDays = 1
        )
        examDao.insertStudentStats(stats)
    }

    suspend fun populateDemoDataIfEmpty() {
        val currentExams = examDao.getAllExams().firstOrNull() ?: emptyList()
        if (currentExams.isEmpty()) {
            // Seed a default student so they can log in offline easily
            examDao.insertSiswa(
                SiswaEntity(
                    nisn = "123456789",
                    name = "Anita Arista",
                    kelas = "XII MIPA 1",
                    sekolah = "SMA Negeri 1 Jakarta"
                )
            )

            // Seed a default proctor
            examDao.insertProctorSession(
                ProctorSessionEntity(
                    nip = "198765432",
                    name = "Drs. Budi Setiawan, M.Pd",
                    school = "SMA Negeri 1 Jakarta",
                    supervisedClass = "Ruang Ujian XII-MIPA"
                )
            )

            val exams = listOf(
                ExamEntity(
                    id = "EX-001",
                    title = "Ujian Tengah Semester - Matematika Wajib",
                    subject = "Matematika Wajib",
                    durationMinutes = 10,
                    totalQuestions = 5,
                    tokenCode = "MATEMATIKA26"
                ),
                ExamEntity(
                    id = "EX-002",
                    title = "Penilaian Harian - Kimia Organik",
                    subject = "Kimia",
                    durationMinutes = 15,
                    totalQuestions = 5,
                    tokenCode = "KIMIAFUN"
                ),
                ExamEntity(
                    id = "EX-003",
                    title = "Ujian Akhir Semester - Bahasa Inggris Efektif",
                    subject = "Bahasa Inggris",
                    durationMinutes = 20,
                    totalQuestions = 5,
                    tokenCode = "ENGLISH99",
                    isFinished = true,
                    score = 90.0,
                    correctCount = 4,
                    wrongCount = 1,
                    notAnsweredCount = 0
                )
            )
            examDao.insertExams(exams)

            // Seed questions for Exam 1 - Matematika Wajib
            val questionsExam1 = listOf(
                QuestionEntity(
                    examId = "EX-001",
                    questionNumber = 1,
                    text = "Jika f(x) = 3x + 5 dan g(x) = 2x - 1, tentukan nilai dari komposisi fungsi (f o g)(3)!",
                    optionA = "16",
                    optionB = "17",
                    optionC = "20",
                    optionD = "23",
                    optionE = "26",
                    correctOption = "C" // g(3)=5, f(5)=20
                ),
                QuestionEntity(
                    examId = "EX-001",
                    questionNumber = 2,
                    text = "Berapakah jumlah dari deret aritmetika 2 + 5 + 8 + 11 + ... sampai suku ke-10?",
                    optionA = "145",
                    optionB = "155",
                    optionC = "165",
                    optionD = "175",
                    optionE = "185",
                    correctOption = "B" // S_10 = 5 * (4 + 9*3) = 5 * 31 = 155
                ),
                QuestionEntity(
                    examId = "EX-001",
                    questionNumber = 3,
                    text = "Kubus ABCD.EFGH memiliki panjang rusuk 6 cm. Jarak titik A ke garis CF adalah...",
                    optionA = "3√2 cm",
                    optionB = "3√3 cm",
                    optionC = "3√5 cm",
                    optionD = "3√6 cm",
                    optionE = "6√2 cm",
                    correctOption = "D" // d = sqrt(AC^2 - CP^2) = sqrt(72 - 18) = sqrt(54) = 3√6
                ),
                QuestionEntity(
                    examId = "EX-001",
                    questionNumber = 4,
                    text = "Turunan pertama dari f(x) = (2x^2 - 3)(5x + 1) adalah...",
                    optionA = "30x^2 + 4x - 15",
                    optionB = "30x^2 - 4x + 15",
                    optionC = "20x^2 + 4x - 15",
                    optionD = "30x^2 + 4x",
                    optionE = "20x + 5",
                    correctOption = "A" // f(x) = 10x^3 + 2x^2 - 15x - 3 -> f'(x) = 30x^2 + 4x - 15
                ),
                QuestionEntity(
                    examId = "EX-001",
                    questionNumber = 5,
                    text = "Nilai limit dari x menuju 3 untuk fungsi (x^2 - 9) / (x - 3) adalah...",
                    optionA = "0",
                    optionB = "3",
                    optionC = "6",
                    optionD = "9",
                    optionE = "Tak hingga",
                    correctOption = "C" // (x-3)(x+3)/(x-3) = x+3 -> limit = 6
                )
            )

            // Seed questions for Exam 2 - Kimia
            val questionsExam2 = listOf(
                QuestionEntity(
                    examId = "EX-002",
                    questionNumber = 1,
                    text = "Senyawa hidrokarbon jenuh yang memiliki rumus umum CnH2n+2 dinamakan golongan...",
                    optionA = "Alkuna",
                    optionB = "Alkena",
                    optionC = "Alkana",
                    optionD = "Alkil",
                    optionE = "Aromatik",
                    correctOption = "C"
                ),
                QuestionEntity(
                    examId = "EX-002",
                    questionNumber = 2,
                    text = "Manakah senyawa di bawah ini yang tergolong dalam isomer fungsional dari propanol?",
                    optionA = "Metoksi etana",
                    optionB = "Propanon",
                    optionC = "Asam propanoat",
                    optionD = "Metil etanoat",
                    optionE = "Propanal",
                    correctOption = "A" // Alkohol isomer fungsional eter (C3H8O) -> metoksi etana
                ),
                QuestionEntity(
                    examId = "EX-002",
                    questionNumber = 3,
                    text = "Zat manakah yang paling cocok digunakan sebagai zat anti-ketukan (anti-knocking) ramah lingkungan pada bensin?",
                    optionA = "TEL (Tetraethyl Lead)",
                    optionB = "MTBE (Methyl Tertiary Butyl Ether)",
                    optionC = "N-heksana",
                    optionD = "Solven Nafta",
                    optionE = "Benzena",
                    correctOption = "B"
                ),
                QuestionEntity(
                    examId = "EX-002",
                    questionNumber = 4,
                    text = "Di antara reaksi berikut, yang merupakan reaksi eliminasi adalah...",
                    optionA = "CH4 + Cl2 -> CH3Cl + HCl",
                    optionB = "CH2=CH2 + H2 -> CH3-CH3",
                    optionC = "CH3-CH2-OH -> CH2=CH2 + H2O",
                    optionD = "C6H6 + HNO3 -> C6H5NO2 + H2O",
                    optionE = "CH3-CH3 + Cl2 -> CH3-CH2Cl + HCl",
                    correctOption = "C" // dehidrasi alkohol membentuk alkena (eliminasi)
                ),
                QuestionEntity(
                    examId = "EX-002",
                    questionNumber = 5,
                    text = "Gugus fungsi aldehida (-CHO) dapat diidentifikasi menghasilkan endapan merah bata jika direaksikan dengan pereaksi...",
                    optionA = "Fehling",
                    optionB = "Tollens",
                    optionC = "Seliwanoff",
                    optionD = "Biuret",
                    optionE = "Benedict",
                    correctOption = "A" // Aldehida + Fehling -> merah bata Cu2O
                )
            )

            // Seed questions for Exam 3 - Bahasa Inggris (Historik Selesai)
            val questionsExam3 = listOf(
                QuestionEntity(
                    examId = "EX-003",
                    questionNumber = 1,
                    text = "Complete the sentence: 'If I ___ harder, I would have passed the exam yesterday.'",
                    optionA = "studying",
                    optionB = "studied",
                    optionC = "have studied",
                    optionD = "had studied",
                    optionE = "would study",
                    correctOption = "D",
                    selectedOption = "D" // Correct
                ),
                QuestionEntity(
                    examId = "EX-003",
                    questionNumber = 2,
                    text = "What is the synonym of the word 'BENEVOLENT'?",
                    optionA = "Malevolent",
                    optionB = "Kind-hearted",
                    optionC = "Selfish",
                    optionD = "Violent",
                    optionE = "Apathetic",
                    correctOption = "B",
                    selectedOption = "B" // Correct
                ),
                QuestionEntity(
                    examId = "EX-003",
                    questionNumber = 3,
                    text = "Choose the passive form of: 'The teacher praised the active student.'",
                    optionA = "The active student was praised by the teacher.",
                    optionB = "The active student is praised by the teacher.",
                    optionC = "The active student has been praised by the teacher.",
                    optionD = "The active student is being praised by the teacher.",
                    optionE = "The active student was being praised by the teacher.",
                    correctOption = "A",
                    selectedOption = "A" // Correct
                ),
                QuestionEntity(
                    examId = "EX-003",
                    questionNumber = 4,
                    text = "Identify the noun clause in this sentence: 'That she forgot her ID card is unfortunate.'",
                    optionA = "unfortunate",
                    optionB = "forgot her ID card",
                    optionC = "is unfortunate",
                    optionD = "That she forgot her ID card",
                    optionE = "her ID card",
                    correctOption = "D",
                    selectedOption = "D" // Correct
                ),
                QuestionEntity(
                    examId = "EX-003",
                    questionNumber = 5,
                    text = "Select the word that expresses contrast: 'The weather was cold ___ they decided to go for a swim.'",
                    optionA = "so",
                    optionB = "and",
                    optionC = "yet",
                    optionD = "because",
                    optionE = "moreover",
                    correctOption = "C",
                    selectedOption = "B" // Wrong (User selected B, correct is C - yet)
                )
            )

            examDao.insertQuestions(questionsExam1 + questionsExam2 + questionsExam3)
            
            // Seed base activity logs
            examDao.insertActivityLog(
                StudentActivityLogEntity(
                    studentName = "Anita Arista",
                    examId = "EX-003",
                    examTitle = "Ujian Akhir Semester - Bahasa Inggris Efektif",
                    type = "START",
                    message = "Siswa memulai ujian pada pukul 08:00 WIB"
                )
            )
            examDao.insertActivityLog(
                StudentActivityLogEntity(
                    studentName = "Anita Arista",
                    examId = "EX-003",
                    examTitle = "Ujian Akhir Semester - Bahasa Inggris Efektif",
                    type = "FOCUS_LOST",
                    message = "Peringatan 1: Siswa beralih fokus (keluar dari tab/aplikasi)"
                )
            )
            examDao.insertActivityLog(
                StudentActivityLogEntity(
                    studentName = "Anita Arista",
                    examId = "EX-003",
                    examTitle = "Ujian Akhir Semester - Bahasa Inggris Efektif",
                    type = "SUBMIT",
                    message = "Siswa menyelesaikan ujian dengan sukses. Skor: 90.0"
                )
            )
        }
    }
}
