package com.rexaps.rexlife

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

internal data class SavingsTransaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Long,
    val kind: String,
    val category: String,
    val date: String
)

internal data class SavingsGoal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val targetAmount: Long,
    val savedAmount: Long
)

internal data class SchoolSubject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val day: String,
    val startTime: String,
    val endTime: String,
    val teacher: String = "",
    val room: String = ""
)

internal data class UniformPlan(
    val id: String = UUID.randomUUID().toString(),
    val day: String,
    val outfit: String,
    val items: String = ""
)

internal data class Homework(
    val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val description: String,
    val pages: String = "",
    val deadlineDate: String,
    val deadlineTime: String,
    val done: Boolean = false
)

internal data class RexLifeData(
    val transactions: List<SavingsTransaction> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
    val subjects: List<SchoolSubject> = emptyList(),
    val uniforms: List<UniformPlan> = emptyList(),
    val homework: List<Homework> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("schemaVersion", 1)
        .put("transactions", JSONArray().also { array ->
            transactions.forEach { item ->
                array.put(JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("amount", item.amount)
                    .put("kind", item.kind)
                    .put("category", item.category)
                    .put("date", item.date))
            }
        })
        .put("goals", JSONArray().also { array ->
            goals.forEach { item ->
                array.put(JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("targetAmount", item.targetAmount)
                    .put("savedAmount", item.savedAmount))
            }
        })
        .put("subjects", JSONArray().also { array ->
            subjects.forEach { item ->
                array.put(JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("day", item.day)
                    .put("startTime", item.startTime)
                    .put("endTime", item.endTime)
                    .put("teacher", item.teacher)
                    .put("room", item.room))
            }
        })
        .put("uniforms", JSONArray().also { array ->
            uniforms.forEach { item ->
                array.put(JSONObject()
                    .put("id", item.id)
                    .put("day", item.day)
                    .put("outfit", item.outfit)
                    .put("items", item.items))
            }
        })
        .put("homework", JSONArray().also { array ->
            homework.forEach { item ->
                array.put(JSONObject()
                    .put("id", item.id)
                    .put("subject", item.subject)
                    .put("description", item.description)
                    .put("pages", item.pages)
                    .put("deadlineDate", item.deadlineDate)
                    .put("deadlineTime", item.deadlineTime)
                    .put("done", item.done))
            }
        })

    companion object {
        fun fromJson(json: JSONObject): RexLifeData {
            fun JSONArray?.objects(block: (JSONObject) -> Unit) {
                if (this == null) return
                for (index in 0 until length()) {
                    optJSONObject(index)?.let(block)
                }
            }

            val transactions = mutableListOf<SavingsTransaction>()
            json.optJSONArray("transactions").objects {
                transactions += SavingsTransaction(
                    id = it.optString("id").ifBlank { UUID.randomUUID().toString() },
                    title = it.optString("title"),
                    amount = it.optLong("amount"),
                    kind = it.optString("kind", "income"),
                    category = it.optString("category"),
                    date = it.optString("date")
                )
            }

            val goals = mutableListOf<SavingsGoal>()
            json.optJSONArray("goals").objects {
                goals += SavingsGoal(
                    id = it.optString("id").ifBlank { UUID.randomUUID().toString() },
                    title = it.optString("title"),
                    targetAmount = it.optLong("targetAmount"),
                    savedAmount = it.optLong("savedAmount")
                )
            }

            val subjects = mutableListOf<SchoolSubject>()
            json.optJSONArray("subjects").objects {
                subjects += SchoolSubject(
                    id = it.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = it.optString("name"),
                    day = it.optString("day"),
                    startTime = it.optString("startTime", "07:00"),
                    endTime = it.optString("endTime", "07:45"),
                    teacher = it.optString("teacher"),
                    room = it.optString("room")
                )
            }

            val uniforms = mutableListOf<UniformPlan>()
            json.optJSONArray("uniforms").objects {
                uniforms += UniformPlan(
                    id = it.optString("id").ifBlank { UUID.randomUUID().toString() },
                    day = it.optString("day"),
                    outfit = it.optString("outfit"),
                    items = it.optString("items")
                )
            }

            val homework = mutableListOf<Homework>()
            json.optJSONArray("homework").objects {
                homework += Homework(
                    id = it.optString("id").ifBlank { UUID.randomUUID().toString() },
                    subject = it.optString("subject"),
                    description = it.optString("description"),
                    pages = it.optString("pages"),
                    deadlineDate = it.optString("deadlineDate"),
                    deadlineTime = it.optString("deadlineTime", "23:59"),
                    done = it.optBoolean("done")
                )
            }

            return RexLifeData(
                transactions = transactions,
                goals = goals,
                subjects = subjects,
                uniforms = uniforms,
                homework = homework
            )
        }
    }
}

/**
 * Persists one small, versioned JSON document. Public Downloads storage is preferred when the
 * user grants all-files access; app-private storage is an explicit fallback, never a silent
 * claim that the requested public path is active.
 */
internal class RexLifeStore(context: Context) {
    private val appContext = context.applicationContext
    private val publicRoot = File(
        Environment.getExternalStorageDirectory(),
        "Download/RexAps/RexLife"
    )
    private val privateRoot = File(appContext.filesDir, "RexLife")

    private var dataFile: File = resolveDataFile()
    var data by mutableStateOf(readFile(dataFile))
        private set
    var usingPublicFolder by mutableStateOf(dataFile.absolutePath.startsWith(publicRoot.absolutePath))
        private set
    var storageError by mutableStateOf(false)
        private set
    var activeStoragePath by mutableStateOf(dataFile.parentFile?.parentFile?.absolutePath.orEmpty())
        private set

    fun update(transform: (RexLifeData) -> RexLifeData) {
        val next = transform(data)
        data = next
        persist(next)
    }

    /** Call after returning from Android's all-files-access settings. */
    fun refreshStorage() {
        val target = resolveDataFile()
        if (target.absolutePath == dataFile.absolutePath) {
            usingPublicFolder = dataFile.absolutePath.startsWith(publicRoot.absolutePath)
            activeStoragePath = rootPathFor(dataFile)
            return
        }

        val targetAlreadyHasData = target.exists() && target.length() > 0L
        val privateCopyHasData = !usingPublicFolder && dataFile.exists() && dataFile.length() > 0L
        if (targetAlreadyHasData && !storageError && !privateCopyHasData) {
            // On a fresh install with no private copy yet, restore an existing public backup.
            data = readFile(target)
        } else {
            // Prefer the currently active copy when migrating after the user has made edits.
            writeFile(target, data)
        }
        dataFile = target
        usingPublicFolder = target.absolutePath.startsWith(publicRoot.absolutePath)
        activeStoragePath = rootPathFor(target)
        storageError = false
    }

    private fun persist(value: RexLifeData) {
        runCatching { writeFile(dataFile, value) }
            .onSuccess {
                storageError = false
                usingPublicFolder = dataFile.absolutePath.startsWith(publicRoot.absolutePath)
                activeStoragePath = rootPathFor(dataFile)
            }
            .onFailure {
                // Preserve edits even if Android revokes shared-storage access mid-session.
                dataFile = File(privateRoot, "data/rexlife.json")
                runCatching { writeFile(dataFile, value) }
                usingPublicFolder = false
                activeStoragePath = rootPathFor(dataFile)
                storageError = true
            }
    }

    private fun resolveDataFile(): File {
        val publicFile = File(publicRoot, "data/rexlife.json")
        if (hasPublicStoragePermission()) {
            val parent = publicFile.parentFile
            val prepared = runCatching {
                parent?.mkdirs()
                val probe = File(parent, ".rexlife-write-check")
                probe.writeText("ok")
                probe.delete()
                true
            }.getOrDefault(false)
            if (prepared) return publicFile
        }
        val privateFile = File(privateRoot, "data/rexlife.json")
        runCatching { privateFile.parentFile?.mkdirs() }
        return privateFile
    }

    private fun hasPublicStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            appContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun writeFile(file: File, value: RexLifeData) {
        val parent = file.parentFile ?: error("Folder data RexLife tidak valid")
        check(parent.exists() || parent.mkdirs()) { "Tidak dapat membuat folder ${parent.path}" }
        val temp = File(parent, "${file.name}.tmp")
        temp.writeText(value.toJson().toString(2), Charsets.UTF_8)
        if (file.exists() && !file.delete()) {
            temp.delete()
            error("Tidak dapat mengganti file data RexLife")
        }
        check(temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    private fun readFile(file: File): RexLifeData {
        if (!file.exists()) return RexLifeData()
        return runCatching {
            RexLifeData.fromJson(JSONObject(file.readText(Charsets.UTF_8)))
        }.getOrDefault(RexLifeData())
    }

    private fun rootPathFor(file: File): String {
        val parent = file.parentFile ?: return file.absolutePath
        return parent.parentFile?.absolutePath ?: parent.absolutePath
    }

    companion object {
        const val PUBLIC_FOLDER_PATH = "/storage/emulated/0/Download/RexAps/RexLife"
    }
}
