import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
val botToken = "8152106026:AAHeubz1p_8o8Pf4m8UjBmLVXXs2FtQMpHc"
fun main() {
    bot.startPolling()
}
// Define groups and students
val adminIds = setOf(5027828856L, 987654321L)
data class Admin(val id: Long)
data class Student(val id: Long, val name: String, val parentId: Long)
data class Group(val id: Long, val name: String, val startTime: String, val students: MutableList<Student>)

// Storage
val admins = mutableSetOf<Admin>(Admin(5027828856L))
val groups = mutableListOf<Group>(
    Group(1, "Group 1", "09:00", mutableListOf(
        Student(1, "Test", 6323420301L),
        Student(2, "Jane Doe", 123456789L)
    )),
    Group(2, "Group 2", "10:00", mutableListOf()),
    Group(3, "Group 3", "11:00", mutableListOf())
)

val bot: Bot = bot {
    token = botToken

    dispatch {
        command("start") {
            if (message.chat.id in admins.map { it.id }) {
                bot.sendMessage(
                    ChatId.fromId(message.chat.id),
                    "Admin Menu:",
                    replyMarkup = adminMenuButtons()
                )
            }else{
                bot.sendMessage(ChatId.fromId(message.chat.id), "Your ID: ${message.from?.id}")
            }
        }
        callbackQuery("mark_late") {
            if (callbackQuery.from.id in admins.map { it.id }) {
                bot.sendMessage(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    "Select group:",
                    replyMarkup = groupButtons()
                )
            }
        }
        callbackQuery("late_def_") { ->
            val studentId = callbackQuery.data.removePrefix("late_def_").toLong()
            val student = groups.flatMap { it.students }.find { it.id == studentId }

            if (student != null) {
                val lateMinutes = calculateLateMinutes(studentId)

                bot.sendMessage(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    "${student.name} has been marked as late by $lateMinutes minutes."
                )
                bot.sendMessage(
                    ChatId.fromId(callbackQuery.message!!.chat.id),
                    "Admin Menu:",
                    replyMarkup = adminMenuButtons()
                )


                // Notify parent
                bot.sendMessage(
                    ChatId.fromId(student.parentId),
                    "Your child ${student.name} was marked as late by $lateMinutes minutes."
                )
            }
        }

        callbackQuery("late_custom_") {  ->
            val studentId = callbackQuery.data.removePrefix("late_custom_").toLong()
            bot.sendMessage(
                ChatId.fromId(callbackQuery.message!!.chat.id),
                "Enter the number of minutes late for student ID: $studentId"
            )
            pendingLateMarks[callbackQuery.message!!.chat.id] = studentId
        }

        message {  ->
            val chatId = message.chat.id
            val text = message.text ?: return@message
            val studentId = pendingLateMarks.remove(chatId) ?: return@message

            val lateMinutes = text.toIntOrNull()
            if (lateMinutes != null) {
                val student = groups.flatMap { it.students }.find { it.id == studentId }
                if (student != null) {
                    bot.sendMessage(
                        ChatId.fromId(chatId),
                        "${student.name} has late by $lateMinutes minutes."
                    )
                    bot.sendMessage(
                        ChatId.fromId(chatId),
                        "Admin Menu:",
                        replyMarkup = adminMenuButtons()
                    )
                    // Notify parent
                    bot.sendMessage(
                        ChatId.fromId(student.parentId),
                        "Your child ${student.name} was late by $lateMinutes minutes."
                    )
                }
            } else {
                bot.sendMessage(ChatId.fromId(chatId), "Please enter a valid number of minutes.")
            }
        }

        callbackQuery (""){  ->
            val data = callbackQuery.data ?: return@callbackQuery

            when {
                data.startsWith("group_") -> {
                    val groupId = data.removePrefix("group_").toLong()
                    val group = groups.find { it.id == groupId }
                    if (group != null) {
                        bot.sendMessage(
                            ChatId.fromId(callbackQuery.message!!.chat.id),
                            "Select student:",
                            replyMarkup = studentButtons(group)
                        )
                    }
                }
                data.startsWith("student_") -> {
                    val studentId = data.removePrefix("student_").toLong()
                    val student = groups.flatMap { it.students }.find { it.id == studentId }
                    if (student != null) {
                        bot.sendMessage(
                            ChatId.fromId(callbackQuery.message!!.chat.id),
                            "Choose an option for ${student.name}:",
                            replyMarkup = markLateOptions(student.id)
                        )
                    }
                }

                data.startsWith("late_") -> {
                    val studentId = data.removePrefix("late_").toLong()
                    val student = groups.flatMap { it.students }.find { it.id == studentId }
                    if (student != null) {
                        val lateMinutes = calculateLateMinutes(studentId)

                        bot.sendMessage(
                            ChatId.fromId(callbackQuery.message!!.chat.id),
                            "Select how you want to mark late:",
                            replyMarkup = lateMinutesOptions(studentId, lateMinutes)
                        )
                    }
                }

//                data.startsWith("late_def_") -> {
//                    val studentId = data.removePrefix("late_def_").toLong()
//                    val student = groups.flatMap { it.students }.find { it.id == studentId }
//                    if (student != null) {
//                        val lateMinutes = calculateLateMinutes(studentId)
//
//                        bot.sendMessage(
//                            ChatId.fromId(callbackQuery.message!!.chat.id),
//                            "${student.name} has been marked as late by $lateMinutes minutes."
//                        )
//
//                        // Notify parent
//                        bot.sendMessage(
//                            ChatId.fromId(student.parentId),
//                            "Your child ${student.name} was marked as late by $lateMinutes minutes."
//                        )
//                    }
//                }
//
//                data.startsWith("late_custom_") -> {
//                    val studentId = data.removePrefix("late_custom_").toLong()
//                    bot.sendMessage(
//                        ChatId.fromId(callbackQuery.message!!.chat.id),
//                        "Enter the number of minutes late for student ID: $studentId"
//                    )
//                    pendingLateMarks[callbackQuery.message!!.chat.id] = studentId
//                }

                data.startsWith("absent_") -> {
                    val studentId = data.removePrefix("absent_").toLong()
                    val student = groups.flatMap { it.students }.find { it.id == studentId }
                    if (student != null) {
                        bot.sendMessage(
                            ChatId.fromId(callbackQuery.message!!.chat.id),
                            "${student.name} did not attend class today."
                        )

                        // Notify parent
                        bot.sendMessage(
                            ChatId.fromId(student.parentId),
                            "Your child ${student.name} did not came to class."
                        )
                    }
                }
            }
        }

    }
}

val pendingLateMarks = mutableMapOf<Long, Long>()

// Admin Menu Buttons
fun adminMenuButtons() = InlineKeyboardMarkup.create(
    listOf(
        listOf(InlineKeyboardButton.CallbackData("Mark Late", "mark_late")),
        listOf(InlineKeyboardButton.CallbackData("Manage Groups", "manage_groups"))
    )
)

// Group Selection Buttons
fun groupButtons() = InlineKeyboardMarkup.create(
    groups.map { group ->
        listOf(InlineKeyboardButton.CallbackData(group.name, "group_${group.id}"))
    }
)

// Student Selection Buttons
fun studentButtons(group: Group) = InlineKeyboardMarkup.create(
    group.students.map { student ->
        listOf(InlineKeyboardButton.CallbackData(student.name, "student_${student.id}"))
    }
)

fun markLateOptions(studentId: Long) = InlineKeyboardMarkup.create(
    listOf(
        listOf(InlineKeyboardButton.CallbackData("Mark Late", "late_$studentId")),
        listOf(InlineKeyboardButton.CallbackData("Didn't Come", "absent_$studentId"))
    )
)
// Calculate Late Minutes
fun calculateLateMinutes(studentId: Long): Int {
    val group = groups.find { it.students.any { student -> student.id == studentId } }
    val groupStartTime = group?.startTime ?: return 0

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val startTime = LocalTime.parse(groupStartTime, formatter)
    val now = LocalTime.now()

    return now.toSecondOfDay() / 60 - startTime.toSecondOfDay() / 60
}
fun lateMinutesOptions(studentId: Long, defaultMinutes: Int) = InlineKeyboardMarkup.create(
    listOf(
        listOf(InlineKeyboardButton.CallbackData("Use Default ($defaultMinutes min)", "late_def_$studentId")),
        listOf(InlineKeyboardButton.CallbackData("Enter Custom Minutes", "late_custom_$studentId"))
    )
)