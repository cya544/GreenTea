package com.cya544.greentea

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.net.Uri
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.edit
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import kotlinx.coroutines.launch
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.FileProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cya544.greentea.ui.theme.GreenTeaTheme
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

private const val PREFS_NAME = "blood_pressure_records"
private const val KEY_RECORDS = "records"

data class BloodPressureRecord(
    val id: Long,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val timestamp: Long,
) {
    val dateKey: String get() = formatDateOnly(timestamp)
}

class MainActivity : ComponentActivity() {
    private var keepSplashOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GreenTeaTheme {
                BloodPressureApp()
            }
        }
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val iconView = splashScreenView.iconView
            val translationDistance = -iconView.rootView.height * 0.3f

            val floatUp = ObjectAnimator.ofFloat(
                iconView,
                View.TRANSLATION_Y,
                0f,
                translationDistance
            ).apply {
                duration = 850
                interpolator = DecelerateInterpolator(1.6f)
            }

            val scaleDown = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 0.5f)
            val scaleUp = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 0.5f)
            val fadeOut = ObjectAnimator.ofFloat(iconView, View.ALPHA, 1f, 0f)

            val phase2 = AnimatorSet().apply {
                playTogether(scaleDown, scaleUp, fadeOut)
                duration = 400
            }

            AnimatorSet().apply {
                playSequentially(floatUp, phase2)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        splashScreenView.remove()
                    }
                })
                start()
            }
        }
        keepSplashOnScreen = false
    }
}

@Composable
private fun blueButtonColors() = ButtonDefaults.buttonColors(
    containerColor = androidx.compose.ui.graphics.Color(0xFF4F7DF3),
    contentColor = androidx.compose.ui.graphics.Color.White
)

@Composable
private fun greenButtonColors() = ButtonDefaults.buttonColors(
    containerColor = androidx.compose.ui.graphics.Color(0xFF2FA66B),
    contentColor = androidx.compose.ui.graphics.Color.White
)

@Composable
private fun redButtonColors() = ButtonDefaults.buttonColors(
    containerColor = androidx.compose.ui.graphics.Color(0xFFE15554),
    contentColor = androidx.compose.ui.graphics.Color.White
)

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(millis))

private fun formatDateOnly(millis: Long): String =
    SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(millis))

private fun loadRecords(context: Context): List<BloodPressureRecord> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_RECORDS, "[]")
        ?: "[]"
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    BloodPressureRecord(
                        id = item.getLong("id"),
                        systolic = item.getInt("systolic"),
                        diastolic = item.getInt("diastolic"),
                        pulse = item.getInt("pulse"),
                        timestamp = item.getLong("timestamp"),
                    )
                )
            }
        }.sortedByDescending { it.timestamp }
    }.getOrElse { emptyList() }
}

private fun formatSeconds(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return if (minutes > 0) "%02d:%02d".format(minutes, seconds) else "00:%02d".format(seconds)
}

private fun showWheelTimePicker(
    context: Context,
    initialHour: Int,
    initialMinute: Int,
    onPicked: (Int, Int) -> Unit,
) {
    val hourPicker = NumberPicker(context).apply {
        minValue = 0
        maxValue = 23
        value = initialHour
        wrapSelectorWheel = true
    }
    val minutePicker = NumberPicker(context).apply {
        minValue = 0
        maxValue = 59
        value = initialMinute
        wrapSelectorWheel = true
        displayedValues = Array(60) { i -> "%02d".format(i) }
    }

    val content = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        val padding = (context.resources.displayMetrics.density * 16).toInt()
        setPadding(padding, padding, padding, padding)
        addView(hourPicker, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(minutePicker, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }

    androidx.appcompat.app.AlertDialog.Builder(context)
        .setTitle("选择时间")
        .setView(content)
        .setPositiveButton("确定") { _: DialogInterface, _: Int -> onPicked(hourPicker.value, minutePicker.value) }
        .setNegativeButton("取消", null)
        .show()
}

private fun saveRecords(context: Context, records: List<BloodPressureRecord>) {
    val array = JSONArray()
    records.forEach { record ->
        array.put(
            JSONObject()
                .put("id", record.id)
                .put("systolic", record.systolic)
                .put("diastolic", record.diastolic)
                .put("pulse", record.pulse)
                .put("timestamp", record.timestamp)
        )
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putString(KEY_RECORDS, array.toString())
    }
}

private fun sanitizeFileName(name: String): String = name.replace(Regex("[/:*?\"<>|]"), "_")

private fun exportXlsx(records: List<BloodPressureRecord>, fileName: String): File {
    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloads.exists()) downloads.mkdirs()
    val safeName = sanitizeFileName(fileName).removeSuffix(".xlsx").removeSuffix(".xls") + ".xlsx"
    val file = File(downloads, safeName)

    val workbook = XSSFWorkbook()
    try {
        val sheet = workbook.createSheet("血压记录")

        val headerStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            fillPattern = FillPatternType.SOLID_FOREGROUND
            fillForegroundColor = 42
        }
        val bodyStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val headers = listOf("序号", "时间", "高压（mmHg）", "低压（mmHg）", "脉搏（bpm）")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { column, title ->
            val cell = headerRow.createCell(column)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        records.forEachIndexed { index, record ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).apply {
                setCellValue((index + 1).toDouble())
                cellStyle = bodyStyle
            }
            row.createCell(1).apply {
                setCellValue(formatDateTime(record.timestamp))
                cellStyle = bodyStyle
            }
            row.createCell(2).apply {
                setCellValue(record.systolic.toDouble())
                cellStyle = bodyStyle
            }
            row.createCell(3).apply {
                setCellValue(record.diastolic.toDouble())
                cellStyle = bodyStyle
            }
            row.createCell(4).apply {
                setCellValue(record.pulse.toDouble())
                cellStyle = bodyStyle
            }
        }

        sheet.setColumnWidth(0, 8 * 256)
        sheet.setColumnWidth(1, 22 * 256)
        sheet.setColumnWidth(2, 14 * 256)
        sheet.setColumnWidth(3, 14 * 256)
        sheet.setColumnWidth(4, 14 * 256)

        FileOutputStream(file).use { workbook.write(it) }
    } finally {
        workbook.close()
    }

    return file
}

private fun exportAllData(context: Context, fileName: String = "GreenTea_全部数据备份.json"): File {
    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloads.exists()) downloads.mkdirs()
    val safeName = sanitizeFileName(fileName).removeSuffix(".json") + ".json"
    val file = File(downloads, safeName)
    val (followSystemTheme, darkTheme) = loadThemeSettings(context)

    val backup = JSONObject().apply {
        put("format", "GreenTeaBackup")
        put("version", 1)
        put("exportedAt", System.currentTimeMillis())
        put("bloodPressureRecords", JSONArray().apply {
            loadRecords(context).forEach { record ->
                put(JSONObject().apply {
                    put("id", record.id)
                    put("systolic", record.systolic)
                    put("diastolic", record.diastolic)
                    put("pulse", record.pulse)
                    put("timestamp", record.timestamp)
                })
            }
        })
        put("todos", JSONArray().apply {
            loadTodos(context).forEach { todo ->
                put(JSONObject().apply {
                    put("id", todo.id)
                    put("dateKey", todo.dateKey)
                    put("title", todo.title)
                    put("done", todo.done)
                })
            }
        })
        put("exercises", JSONArray().apply {
            loadExercises(context).forEach { exercise ->
                put(JSONObject().apply {
                    put("id", exercise.id)
                    put("name", exercise.name)
                    put("steps", JSONArray().apply {
                        exercise.steps.forEach { step ->
                            put(JSONObject().apply {
                                put("name", step.name)
                                put("durationSeconds", step.durationSeconds)
                                put("restSeconds", step.restSeconds)
                            })
                        }
                    })
                })
            }
        })
        put("settings", JSONObject().apply {
            put("followSystemTheme", followSystemTheme)
            put("darkTheme", darkTheme)
        })
    }

    FileOutputStream(file).use { it.write(backup.toString(2).toByteArray()) }
    return file
}

private fun importAllDataFromBackup(context: Context, jsonContent: String): String {
    val backup = JSONObject(jsonContent)
    if (backup.optString("format") != "GreenTeaBackup") {
        return "不是有效的备份文件"
    }

    val importedBpRecords = buildList {
        val array = backup.optJSONArray("bloodPressureRecords") ?: JSONArray()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            add(
                BloodPressureRecord(
                    id = item.optLong("id", System.currentTimeMillis() + i),
                    systolic = item.optInt("systolic", 0),
                    diastolic = item.optInt("diastolic", 0),
                    pulse = item.optInt("pulse", 0),
                    timestamp = item.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }
    }
    val mergedBpRecords = (loadRecords(context) + importedBpRecords)
        .distinctBy { it.timestamp }
        .sortedByDescending { it.timestamp }
    saveRecords(context, mergedBpRecords)

    val importedTodos = buildList {
        val array = backup.optJSONArray("todos") ?: JSONArray()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            add(
                TodoItem(
                    id = item.optLong("id", System.currentTimeMillis() + i),
                    dateKey = item.optString("dateKey", formatDateOnly(System.currentTimeMillis())),
                    title = item.optString("title", "未命名代办"),
                    done = item.optBoolean("done", false)
                )
            )
        }
    }
    val mergedTodos = (loadTodos(context) + importedTodos)
        .distinctBy { it.title }
        .sortedByDescending { it.id }
    saveTodos(context, mergedTodos)

    val importedExercises = buildList {
        val array = backup.optJSONArray("exercises") ?: JSONArray()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val stepsArray = item.optJSONArray("steps") ?: JSONArray()
            val steps = buildList {
                for (j in 0 until stepsArray.length()) {
                    val step = stepsArray.optJSONObject(j) ?: continue
                    add(
                        ExerciseStep(
                            name = step.optString("name", "步骤 ${j + 1}"),
                            durationSeconds = step.optInt("durationSeconds", 60),
                            restSeconds = step.optInt("restSeconds", 60)
                        )
                    )
                }
            }
            add(
                ExercisePlan(
                    id = item.optLong("id", System.currentTimeMillis() + i),
                    name = item.optString("name", "未命名锻炼"),
                    steps = steps
                )
            )
        }
    }
    val mergedExercises = (loadExercises(context) + importedExercises)
        .distinctBy { exercise ->
            val stepsKey = exercise.steps.joinToString("|") { "${it.name}:${it.durationSeconds}:${it.restSeconds}" }
            "${exercise.name}::$stepsKey"
        }
        .sortedByDescending { it.id }
    saveExercises(context, mergedExercises)

    val settings = backup.optJSONObject("settings")
    if (settings != null) {
        saveThemeSettings(
            context,
            settings.optBoolean("followSystemTheme", true),
            settings.optBoolean("darkTheme", false)
        )
    }

    return "已恢复备份"
}

private fun importRecordsFromCsv(context: Context, csvContent: String): Int {
    val existing = loadRecords(context).toMutableList()
    val lines = csvContent.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    if (lines.isEmpty()) return 0

    val imported = mutableListOf<BloodPressureRecord>()
    lines.drop(1).forEachIndexed { index, line ->
        val parts = line.split(',').map { it.trim() }
        if (parts.size < 4) return@forEachIndexed
        val timestamp = runCatching {
            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).parse(parts[0])?.time
        }.getOrNull() ?: return@forEachIndexed
        val systolic = parts[1].toIntOrNull() ?: return@forEachIndexed
        val diastolic = parts[2].toIntOrNull() ?: return@forEachIndexed
        val pulse = parts[3].toIntOrNull() ?: return@forEachIndexed
        imported += BloodPressureRecord(System.currentTimeMillis() + index, systolic, diastolic, pulse, timestamp)
    }

    if (imported.isEmpty()) return 0
    val merged = (imported + existing).sortedByDescending { it.timestamp }
    saveRecords(context, merged)
    return imported.size
}

private fun importRecordsFromXlsx(context: Context, uri: Uri): Int {
    val existing = loadRecords(context).toMutableList()
    val imported = mutableListOf<BloodPressureRecord>()
    val errors = mutableListOf<String>()

    fun parseExcelDate(cellValue: String): Long? {
        val numeric = cellValue.toDoubleOrNull() ?: return null
        val millis = ((numeric - 25569.0) * 86400000.0).toLong()
        return if (millis > 0) millis else null
    }

    fun parseDateText(dateText: String): Long? {
        return runCatching {
            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).parse(dateText)?.time
        }.getOrNull() ?: runCatching {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(dateText)?.time
        }.getOrNull() ?: parseExcelDate(dateText)
    }

    context.contentResolver.openInputStream(uri)?.use { input ->
        val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.xls")
        tempFile.outputStream().use { output -> input.copyTo(output) }

        ZipFile(tempFile).use { zip ->
            val sharedStrings = mutableListOf<String>()
            zip.getEntry("xl/sharedStrings.xml")?.let { entry ->
                zip.getInputStream(entry).use { stream ->
                    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
                    val nodes = doc.getElementsByTagName("si")
                    for (i in 0 until nodes.length) {
                        sharedStrings += nodes.item(i).textContent.trim()
                    }
                }
            }

            fun cellText(cell: Element): String {
                val type = cell.getAttribute("t")
                val value = cell.getElementsByTagName("v").item(0)?.textContent?.trim().orEmpty()
                return when (type) {
                    "s" -> sharedStrings.getOrNull(value.toIntOrNull() ?: -1).orEmpty()
                    "inlineStr" -> cell.textContent.trim()
                    else -> value
                }
            }

            val sheetEntry = zip.entries().asSequence().firstOrNull { it.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) }
                ?: return 0
            zip.getInputStream(sheetEntry).use { stream ->
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
                val rows = doc.getElementsByTagName("row")
                if (rows.length <= 1) return 0

                val headerRow = rows.item(0) as Element
                val headerCells = headerRow.getElementsByTagName("c")
                val headers = mutableMapOf<String, Int>()
                for (i in 0 until headerCells.length) {
                    val header = cellText(headerCells.item(i) as Element).replace(" ", "")
                    if (header.isNotBlank()) headers[header] = i
                }

                fun findHeader(vararg candidates: String): Int? {
                    for (candidate in candidates) {
                        headers.entries.firstOrNull { it.key.contains(candidate) }?.let { return it.value }
                    }
                    return null
                }

                val timeIdx = findHeader("时间")
                val systolicIdx = findHeader("高压", "收缩压")
                val diastolicIdx = findHeader("低压", "舒张压")
                val pulseIdx = findHeader("脉搏", "心率")

                if (timeIdx == null) errors += "未找到表头：时间"
                if (systolicIdx == null) errors += "未找到表头：高压（mmHg）"
                if (diastolicIdx == null) errors += "未找到表头：低压（mmHg）"
                if (pulseIdx == null) errors += "未找到表头：脉搏（bpm）"

                if (timeIdx == null || systolicIdx == null || diastolicIdx == null || pulseIdx == null) return 0

                for (i in 1 until rows.length) {
                    val row = rows.item(i) as Element
                    val cells = row.getElementsByTagName("c")
                    val maxIdx = maxOf(timeIdx, systolicIdx, diastolicIdx, pulseIdx)
                    if (cells.length <= maxIdx) {
                        errors += "第 ${i + 1} 行列数不足，无法读取完整数据"
                        continue
                    }

                    val dateText = cellText(cells.item(timeIdx) as Element)
                    val systolicText = cellText(cells.item(systolicIdx) as Element)
                    val diastolicText = cellText(cells.item(diastolicIdx) as Element)
                    val pulseText = cellText(cells.item(pulseIdx) as Element)

                    val systolic = systolicText.toIntOrNull()
                    val diastolic = diastolicText.toIntOrNull()
                    val pulse = pulseText.toIntOrNull()
                    val timestamp = parseDateText(dateText)

                    if (timestamp == null) {
                        errors += "第 ${i + 1} 行时间无法识别：$dateText"
                        continue
                    }
                    if (systolic == null) {
                        errors += "第 ${i + 1} 行高压无法识别：$systolicText"
                        continue
                    }
                    if (diastolic == null) {
                        errors += "第 ${i + 1} 行低压无法识别：$diastolicText"
                        continue
                    }
                    if (pulse == null) {
                        errors += "第 ${i + 1} 行脉搏无法识别：$pulseText"
                        continue
                    }

                    imported += BloodPressureRecord(System.currentTimeMillis() + i, systolic, diastolic, pulse, timestamp)
                }
            }
        }
    }

    if (imported.isEmpty()) {
        if (errors.isNotEmpty()) {
            Toast.makeText(context, errors.take(3).joinToString("\n"), Toast.LENGTH_LONG).show()
        }
        return 0
    }
    if (errors.isNotEmpty()) {
        Toast.makeText(context, errors.take(3).joinToString("\n"), Toast.LENGTH_LONG).show()
    }
    val merged = (imported + existing).sortedByDescending { it.timestamp }
    saveRecords(context, merged)
    return imported.size
}

private fun deleteRecord(context: Context, id: Long) {
    val updated = loadRecords(context).filterNot { it.id == id }
    saveRecords(context, updated)
}

private enum class TabItem {
    RECORD,
    TODO,
    EXERCISE,
    SETTINGS,
}

private enum class RecordTabItem {
    SAVE,
    HISTORY,
}

private const val SETTINGS_PREFS = "app_settings"
private const val KEY_FOLLOW_SYSTEM_THEME = "follow_system_theme"
private const val KEY_DARK_THEME = "dark_theme"

private fun loadThemeSettings(context: Context): Pair<Boolean, Boolean> {
    val prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_FOLLOW_SYSTEM_THEME, true) to prefs.getBoolean(KEY_DARK_THEME, false)
}

private fun saveThemeSettings(context: Context, followSystem: Boolean, darkTheme: Boolean) {
    context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE).edit {
        putBoolean(KEY_FOLLOW_SYSTEM_THEME, followSystem)
        putBoolean(KEY_DARK_THEME, darkTheme)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BloodPressureApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(TabItem.RECORD) }
    var records by remember { mutableStateOf(emptyList<BloodPressureRecord>()) }
    val (storedFollowSystem, storedDarkTheme) = remember { loadThemeSettings(context) }
    var followSystemTheme by remember { mutableStateOf(storedFollowSystem) }
    var darkTheme by remember { mutableStateOf(storedDarkTheme) }

    fun refresh() {
        records = loadRecords(context)
    }

    fun persistTheme() {
        saveThemeSettings(context, followSystemTheme, darkTheme)
    }

    LaunchedEffect(Unit) {
        records = loadRecords(context)
    }

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val useDarkTheme = if (followSystemTheme) systemDark else darkTheme

    GreenTeaTheme(darkTheme = useDarkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == TabItem.RECORD,
                        onClick = { selectedTab = TabItem.RECORD },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.save),
                                contentDescription = "记录",
                                modifier = Modifier.size(35.dp)
                            )
                        },
                        label = { Text("记录") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == TabItem.TODO,
                        onClick = { selectedTab = TabItem.TODO },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.todo),
                                contentDescription = "每日代办",
                                modifier = Modifier.size(40.dp)
                            )
                        },
                        label = { Text("每日代办") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == TabItem.EXERCISE,
                        onClick = { selectedTab = TabItem.EXERCISE },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.exercise),
                                contentDescription = "锻炼",
                                modifier = Modifier.size(45.dp)
                            )
                        },
                        label = { Text("锻炼") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == TabItem.SETTINGS,
                        onClick = { selectedTab = TabItem.SETTINGS },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.settings),
                                contentDescription = "设置",
                                modifier = Modifier.size(35.dp)
                            )
                        },
                        label = { Text("设置") }
                    )
                }
            }
            ) { innerPadding ->
                when (selectedTab) {
                    TabItem.RECORD -> RecordScreen(
                        context = context,
                        records = records,
                        modifier = Modifier.padding(innerPadding),
                        onRecordsChanged = { refresh() },
                        onDelete = {
                            deleteRecord(context, it.id)
                            refresh()
                        },
                        onImported = { refresh() }
                    )
                    TabItem.TODO -> TodoScreen(modifier = Modifier.padding(innerPadding))
                    TabItem.EXERCISE -> ExerciseScreen(modifier = Modifier.padding(innerPadding))
                    TabItem.SETTINGS -> SettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        followSystemTheme = followSystemTheme,
                        darkTheme = darkTheme,
                        onFollowSystemThemeChange = {
                            followSystemTheme = it
                            persistTheme()
                        },
                        onDarkThemeChange = {
                            darkTheme = it
                            persistTheme()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordScreen(
    context: Context,
    records: List<BloodPressureRecord>,
    modifier: Modifier = Modifier,
    onRecordsChanged: () -> Unit,
    onDelete: (BloodPressureRecord) -> Unit,
    onImported: () -> Unit,
) {
    var selectedRecordTab by remember { mutableStateOf(RecordTabItem.SAVE) }

    Column(modifier = modifier.fillMaxSize()) {
        RecordTopTabBar(selectedTab = selectedRecordTab, onSelected = { selectedRecordTab = it })
        when (selectedRecordTab) {
            RecordTabItem.SAVE -> SaveScreen(
                context = context,
                modifier = Modifier.weight(1f),
                onRecordsChanged = onRecordsChanged,
            )
            RecordTabItem.HISTORY -> HistoryScreen(
                context = context,
                records = records,
                modifier = Modifier.weight(1f),
                onDelete = onDelete,
                onImported = onImported,
            )
        }
    }
}

@Composable
private fun SaveScreen(
    context: Context,
    modifier: Modifier = Modifier,
    onRecordsChanged: () -> Unit,
) {
    var records by remember { mutableStateOf(loadRecords(context)) }
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var selectedDateTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showOcrNotice by remember { mutableStateOf(false) }

    fun runOcrFromUri(uri: Uri) {
        val bitmap = runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
            }
        }.getOrNull()
        if (bitmap != null) {
            val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = result.text
                    val numbers = Regex("\\d+").findAll(text).map { it.value.toIntOrNull() }.filterNotNull().toList()
                    if (numbers.size >= 3) {
                        systolic = numbers[0].toString()
                        diastolic = numbers[1].toString()
                        pulse = numbers[2].toString()
                    }
                    showOcrNotice = true
                }
                .addOnFailureListener {
                    Toast.makeText(context, "OCR识别失败：${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = photoUri ?: return@rememberLauncherForActivityResult
            runOcrFromUri(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) runOcrFromUri(uri)
    }

    fun refresh() {
        records = loadRecords(context)
        onRecordsChanged()
    }

    fun saveNewRecord() {
        val s = systolic.toIntOrNull()
        val d = diastolic.toIntOrNull()
        val p = pulse.toIntOrNull()
        when {
            s == null -> Toast.makeText(context, "高压不科学，请输入 50-250 之间的数字", Toast.LENGTH_SHORT).show()
            d == null -> Toast.makeText(context, "低压不科学，请输入 30-150 之间的数字", Toast.LENGTH_SHORT).show()
            p == null -> Toast.makeText(context, "脉搏不科学，请输入 30-200 之间的数字", Toast.LENGTH_SHORT).show()
            s !in 50..250 -> Toast.makeText(context, "高压不科学，请输入 50-250 之间的数字", Toast.LENGTH_SHORT).show()
            d !in 30..150 -> Toast.makeText(context, "低压不科学，请输入 30-150 之间的数字", Toast.LENGTH_SHORT).show()
            p !in 30..200 -> Toast.makeText(context, "脉搏不科学，请输入 30-200 之间的数字", Toast.LENGTH_SHORT).show()
            else -> {
                val newRecord = BloodPressureRecord(System.currentTimeMillis(), s, d, p, selectedDateTime)
                records = listOf(newRecord) + records
                saveRecords(context, records)
                systolic = ""
                diastolic = ""
                pulse = ""
                Toast.makeText(context, "记录已保存", Toast.LENGTH_SHORT).show()
                refresh()
            }
        }
    }

    val today = formatDateOnly(System.currentTimeMillis())
    val todayRecords = records.filter { it.dateKey == today }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("保存记录", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = systolic, onValueChange = { systolic = it.filter(Char::isDigit) }, label = { Text("高压 mmHg") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = diastolic, onValueChange = { diastolic = it.filter(Char::isDigit) }, label = { Text("低压 mmHg") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pulse, onValueChange = { pulse = it.filter(Char::isDigit) }, label = { Text("脉搏 bpm") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateTime }
                        DatePickerDialog(context, { _, y, m, d ->
                            showWheelTimePicker(
                                context = context,
                                initialHour = cal.get(Calendar.HOUR_OF_DAY),
                                initialMinute = cal.get(Calendar.MINUTE)
                            ) { hh, mm ->
                                cal.set(y, m, d, hh, mm, 0)
                                selectedDateTime = cal.timeInMillis
                            }
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }, colors = greenButtonColors()) { Text("修改时间", fontSize = 13.sp) }
                    Button(onClick = { selectedDateTime = System.currentTimeMillis() }, colors = blueButtonColors()) { Text("更新时间", fontSize = 13.sp) }
                }
                Text("当前时间：${formatDateTime(selectedDateTime)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        val tempDir = File(context.cacheDir, "camera").apply { mkdirs() }
                        val imgFile = File.createTempFile("ocr_", ".jpg", tempDir)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imgFile)
                        photoUri = uri
                        photoLauncher.launch(uri)
                    }, modifier = Modifier.weight(1f), colors = greenButtonColors()) { Text("拍照识别", fontSize = 13.sp) }
                    Button(onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, modifier = Modifier.weight(1f), colors = greenButtonColors()) { Text("图库识别", fontSize = 13.sp) }
                    Button(onClick = { saveNewRecord() }, modifier = Modifier.weight(1f), colors = blueButtonColors()) { Text("保存记录", fontSize = 13.sp) }
                }
            }
        }

        if (showOcrNotice) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("识别完成") },
                text = {
                    Text(
                        buildAnnotatedString {
                            append("已按照高压、低压、脉搏顺序，从上到下识别文字并回写，但是OCR识别")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("难免有误")
                            }
                            append("，请您自行校准再保存。")
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showOcrNotice = false
                    }) { Text("确定") }
                }
            )
        }

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("今日记录", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("共 ${todayRecords.size} 条")
        }

        if (todayRecords.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("暂无今日记录")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                items(todayRecords, key = { it.id }) { record ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(formatDateTime(record.timestamp), fontWeight = FontWeight.Bold)
                                Text("高压：${record.systolic}  低压：${record.diastolic}  脉搏：${record.pulse}")
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun HistoryScreen(
    context: Context,
    records: List<BloodPressureRecord>,
    modifier: Modifier = Modifier,
    onDelete: (BloodPressureRecord) -> Unit,
    onImported: () -> Unit,
) {
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var page by remember { mutableStateOf(0) }
    var jumpInput by remember { mutableStateOf("") }
    val pager = rememberHistoryPagerState(records, selectedDate, page)
    var exportDialogVisible by remember { mutableStateOf(false) }
    var customExportVisible by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val count = if ((context.contentResolver.getType(uri) ?: "").contains("sheet") || uri.toString().endsWith(".xlsx", true) || uri.toString().endsWith(".xls", true)) {
                    importRecordsFromXlsx(context, uri)
                } else {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { importRecordsFromCsv(context, it.readText()) } ?: 0
                }
                importMessage = if (count > 0) "已导入 $count 条记录" else "未识别到可导入的记录"
                if (count > 0) onImported()
            }.onFailure { importMessage = "导入失败：${it.message}" }
        }
    }

    fun openDatePicker(onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(context, { _, y, m, d ->
            cal.set(y, m, d, 0, 0, 0)
            onPicked(cal.timeInMillis)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    Box(modifier = modifier.fillMaxSize()) {
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HistoryHeader(
                    selectedDate = selectedDate,
                    onPickDate = { openDatePicker { selectedDate = it; page = 0 } },
                    onClearDate = { selectedDate = null; page = 0 }
                )
                HistoryActionsRow(
                    pageText = "第 ${pager.safePage + 1}/${pager.totalPages} 页  共 ${pager.filteredRecords.size} 条",
                    onImport = { launcher.launch(arrayOf("text/*", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) },
                    onOpenExport = { exportDialogVisible = true }
                )
                importMessage?.let { Text(it) }
                exportMessage?.let { Text(it) }
                HistoryRecordList(
                    records = pager.pageRecords,
                    onDelete = onDelete
                )
            }

            HistoryPagerBar(
                page = pager.safePage,
                totalPages = pager.totalPages,
                onPageChange = { page = it },
                jumpInput = jumpInput,
                onJumpInputChange = { jumpInput = it.filter(Char::isDigit) },
                onJumpToPage = { targetPage ->
                    if (targetPage in 0 until pager.totalPages) {
                        page = targetPage
                        jumpInput = ""
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("页码超出范围，请输入 1 到 ${pager.totalPages} 之间的数字")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }

    ExportOptionsDialog(
        visible = exportDialogVisible,
        onDismiss = { exportDialogVisible = false },
        onExportMonth = {
            val now = System.currentTimeMillis()
            val monthText = SimpleDateFormat("yyyy年MM月", Locale.getDefault()).format(Date(now))
            val startEnd = currentMonthRange(now)
            val start = startEnd.first
            val end = startEnd.second
            val monthlyRecords = records.filter { it.timestamp in start..end }
            exportMessage = "已导出到 ${exportXlsx(monthlyRecords, "血压记录_${monthText}.xlsx").absolutePath}"
            exportDialogVisible = false
        },
        onExportWeek = {
            val now = System.currentTimeMillis()
            val start = startOfWeek(now)
            val end = endOfWeek(now)
            val weekRecords = records.filter { it.timestamp in start..end }
            val startText = SimpleDateFormat("yyyy年M月d号", Locale.getDefault()).format(Date(start))
            val endText = SimpleDateFormat("d号", Locale.getDefault()).format(Date(end))
            exportMessage = "已导出到 ${exportXlsx(weekRecords, "血压记录_${startText}至${endText}.xlsx").absolutePath}"
            exportDialogVisible = false
        },
        onCustom = { exportDialogVisible = false; customExportVisible = true }
    )

    CustomExportDialog(
        visible = customExportVisible,
        onDismiss = { customExportVisible = false },
        onPickStart = { openDatePicker { customStartDate = it } },
        onPickEnd = { openDatePicker { customEndDate = it } },
        startDate = customStartDate,
        endDate = customEndDate,
        onExport = {
            val start = customStartDate
            val end = customEndDate
            if (start != null && end != null) {
                val s = minOf(start, end)
                val e = maxOf(start, end)
                val customRecords = records.filter { it.timestamp in s..e }
                val fileName = "血压记录_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(s))}至${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(e))}.xlsx"
                exportMessage = "已导出到 ${exportXlsx(customRecords, fileName).absolutePath}"
                customExportVisible = false
            }
        }
    )
}

private data class HistoryPagerState(
    val filteredRecords: List<BloodPressureRecord>,
    val safePage: Int,
    val totalPages: Int,
    val pageRecords: List<BloodPressureRecord>,
)

private fun currentMonthRange(now: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis
    val end = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1) }.timeInMillis
    return start to end
}

private fun startOfWeek(now: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun endOfWeek(now: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = startOfWeek(now) }
    cal.add(Calendar.DAY_OF_MONTH, 6)
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

@Composable
private fun rememberHistoryPagerState(records: List<BloodPressureRecord>, selectedDate: Long?, page: Int): HistoryPagerState {
    val filtered = records.filter { selectedDate == null || it.dateKey == formatDateOnly(selectedDate) }
    val totalPages = maxOf(1, (filtered.size + 9) / 10)
    val safePage = page.coerceIn(0, totalPages - 1)
    return HistoryPagerState(filtered, safePage, totalPages, filtered.drop(safePage * 10).take(10))
}

@Composable
private fun HistoryHeader(selectedDate: Long?, onPickDate: () -> Unit, onClearDate: () -> Unit) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text("历史记录", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPickDate, colors = blueButtonColors()) { Text(selectedDate?.let { formatDateOnly(it) } ?: "按日期筛选") }
            Button(onClick = onClearDate, colors = greenButtonColors()) { Text("全部") }
        }
    }
}

@Composable
private fun RecordTopTabBar(selectedTab: RecordTabItem, onSelected: (RecordTabItem) -> Unit) {
    val selectedColor = androidx.compose.ui.graphics.Color(0xFF4F7DF3)
    val unselectedColor = androidx.compose.ui.graphics.Color(0xFF333333)
    val tabs = listOf(
        RecordTabItem.SAVE to "保存",
        RecordTabItem.HISTORY to "历史记录",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { (tab, title) ->
                val isSelected = selectedTab == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) selectedColor else unselectedColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .fillMaxWidth(0.18f)
                            .background(if (isSelected) selectedColor else androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryActionsRow(pageText: String, onImport: () -> Unit, onOpenExport: () -> Unit) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onImport, colors = greenButtonColors()) { Text("导入记录") }
            Button(onClick = onOpenExport) { Text("导出记录") }
        }
        Text(pageText)
    }
}

@Composable
private fun HistoryRecordList(records: List<BloodPressureRecord>, onDelete: (BloodPressureRecord) -> Unit) {
    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("暂无记录") }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            items(records, key = { it.id }) { record ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(formatDateTime(record.timestamp), fontWeight = FontWeight.Bold)
                            Text("高压：${record.systolic}  低压：${record.diastolic}  脉搏：${record.pulse}")
                        }
                        Button(onClick = { onDelete(record) }, colors = redButtonColors()) { Text("删除") }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryPagerBar(page: Int, totalPages: Int, onPageChange: (Int) -> Unit, jumpInput: String, onJumpInputChange: (String) -> Unit, onJumpToPage: (Int) -> Unit, modifier: Modifier = Modifier) {
    val maxJump = totalPages - 1
    val canGoPrev = page > 0
    val canGoNext = page < maxJump
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onPageChange(0) },
                enabled = canGoPrev,
                modifier = Modifier.size(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.first_page),
                    contentDescription = "第一页",
                    modifier = Modifier.size(26.dp)
                )
            }
            Button(
                onClick = { onPageChange((page - 1).coerceAtLeast(0)) },
                enabled = canGoPrev,
                modifier = Modifier.size(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.previous_page),
                    contentDescription = "上一页",
                    modifier = Modifier.size(26.dp)
                )
            }
            OutlinedTextField(
                value = jumpInput,
                onValueChange = onJumpInputChange,
                modifier = Modifier.weight(1.4f),
                label = { Text("页码跳转", fontSize = 13.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val target = jumpInput.toIntOrNull()?.minus(1) ?: return@KeyboardActions
                    onJumpToPage(target)
                })
            )
            Button(
                onClick = { onPageChange((page + 1).coerceAtMost(maxJump)) },
                enabled = canGoNext,
                modifier = Modifier.size(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.next_page),
                    contentDescription = "下一页",
                    modifier = Modifier.size(26.dp)
                )
            }
            Button(
                onClick = { onPageChange(maxJump) },
                enabled = canGoNext,
                modifier = Modifier.size(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.last_page),
                    contentDescription = "最后一页",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun ExportOptionsDialog(visible: Boolean, onDismiss: () -> Unit, onExportMonth: () -> Unit, onExportWeek: () -> Unit, onCustom: () -> Unit) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出数据") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("请选择导出范围")
                Button(onClick = onExportMonth, modifier = Modifier.fillMaxWidth()) { Text("导出近一个月") }
                Button(onClick = onExportWeek, modifier = Modifier.fillMaxWidth()) { Text("导出近一个星期") }
                Button(onClick = onCustom, modifier = Modifier.fillMaxWidth()) { Text("自定义导出") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun CustomExportDialog(visible: Boolean, onDismiss: () -> Unit, onPickStart: () -> Unit, onPickEnd: () -> Unit, startDate: Long?, endDate: Long?, onExport: () -> Unit) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义导出") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPickStart, modifier = Modifier.fillMaxWidth()) { Text(startDate?.let { formatDateOnly(it) } ?: "选择开始时间") }
                Button(onClick = onPickEnd, modifier = Modifier.fillMaxWidth()) { Text(endDate?.let { formatDateOnly(it) } ?: "选择结束时间") }
            }
        },
        confirmButton = { TextButton(onClick = onExport) { Text("导出") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private data class TodoItem(
    val id: Long,
    val dateKey: String,
    val title: String,
    val done: Boolean = false,
)

private fun loadTodos(context: Context): List<TodoItem> {
    val raw = context.getSharedPreferences("todo_records", Context.MODE_PRIVATE)
        .getString("todos", "[]") ?: "[]"
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    TodoItem(
                        id = item.getLong("id"),
                        dateKey = item.getString("dateKey"),
                        title = item.getString("title"),
                        done = item.getBoolean("done")
                    )
                )
            }
        }
    }.getOrElse { emptyList() }
}

private fun saveTodos(context: Context, todos: List<TodoItem>) {
    val array = JSONArray()
    todos.forEach { todo ->
        array.put(JSONObject().apply {
            put("id", todo.id)
            put("dateKey", todo.dateKey)
            put("title", todo.title)
            put("done", todo.done)
        })
    }
    context.getSharedPreferences("todo_records", Context.MODE_PRIVATE)
        .edit()
        .putString("todos", array.toString())
        .apply()
}

private fun monthKey(calendar: Calendar): String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)

private fun firstDayOfMonth(calendar: Calendar): Calendar = (calendar.clone() as Calendar).apply {
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

@Composable
private fun TodoCalendarHeader(calendar: Calendar, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onPrevMonth) { Text("<", fontSize = 14.sp) }
        Text(SimpleDateFormat("yyyy年MM月", Locale.getDefault()).format(calendar.time), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        TextButton(onClick = onNextMonth) { Text(">", fontSize = 14.sp) }
    }
}

@Composable
private fun CalendarDayButton(
    dayText: String,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val colors = when {
        isSelected -> blueButtonColors()
        isToday -> ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF2FA66B), contentColor = androidx.compose.ui.graphics.Color.White)
        else -> ButtonDefaults.buttonColors()
    }
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = colors
    ) {
        Text(dayText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

@Composable
private fun ExpandedMonthCard(
    currentMonth: Calendar,
    selectedDate: Long,
    todos: List<TodoItem>,
    onSelectDate: (Long) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val startOfMonth = firstDayOfMonth(currentMonth)
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstWeekday = (startOfMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val todayKey = formatDateOnly(System.currentTimeMillis())
    val selectedKey = formatDateOnly(selectedDate)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TodoCalendarHeader(calendar = currentMonth, onPrevMonth = onPrevMonth, onNextMonth = onNextMonth)
        val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
        Row(modifier = Modifier.fillMaxWidth()) {
            weekLabels.forEach { label -> Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
        val totalCells = firstWeekday + daysInMonth
        val rows = (totalCells + 6) / 7
        var day = 1
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val cellIndex = it * 7 + col
                        val isValid = cellIndex >= firstWeekday && day <= daysInMonth
                        val dateMillis = if (isValid) {
                            Calendar.getInstance().apply {
                                timeInMillis = currentMonth.timeInMillis
                                set(Calendar.DAY_OF_MONTH, day)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        } else 0L
                        val dateKey = if (isValid) formatDateOnly(dateMillis) else ""
                        val hasTodo = isValid && todos.any { it.dateKey == dateKey }
                        val isSelected = isValid && dateKey == selectedKey
                        Box(modifier = Modifier.weight(1f).padding(2.dp)) {
                            if (isValid) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CalendarDayButton(
                                        dayText = day.toString(),
                                        isSelected = isSelected,
                                        isToday = dateKey == todayKey,
                                        onClick = { onSelectDate(dateMillis) }
                                    )
                                    if (hasTodo) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .height(4.dp)
                                                .fillMaxWidth(0.25f)
                                                .background(androidx.compose.ui.graphics.Color(0xFF2FA66B), RoundedCornerShape(999.dp))
                                        )
                                    }
                                }
                                day++
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val todayMillis = remember { System.currentTimeMillis() }
    var todos by remember { mutableStateOf(loadTodos(context)) }
    var selectedDate by remember { mutableStateOf(todayMillis) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var addDialogVisible by remember { mutableStateOf(false) }
    var addTitle by remember { mutableStateOf("") }
    var addDateChoice by remember { mutableStateOf(0) }
    var calendarExpanded by remember { mutableStateOf(true) }

    fun refresh() { todos = loadTodos(context) }
    fun setToday() {
        selectedDate = todayMillis
        currentMonth = Calendar.getInstance()
    }
    fun carryOverUnfinishedFromYesterday() {
        val today = formatDateOnly(System.currentTimeMillis())
        val yesterday = formatDateOnly(System.currentTimeMillis() - 24L * 60 * 60 * 1000)
        val pending = todos.filter { it.dateKey == yesterday && !it.done }
        if (pending.isNotEmpty()) {
            val updated = todos.filterNot { it.dateKey == yesterday && !it.done } + pending.map {
                it.copy(id = System.currentTimeMillis() + it.id, dateKey = today, done = false)
            }
            saveTodos(context, updated)
            todos = updated
        }
    }

    LaunchedEffect(Unit) {
        setToday()
        refresh()
        carryOverUnfinishedFromYesterday()
        refresh()
    }

    val selectedTodos = todos.filter { it.dateKey == formatDateOnly(selectedDate) }
    val startOfMonth = firstDayOfMonth(currentMonth)
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstWeekday = (startOfMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val todayKey = formatDateOnly(System.currentTimeMillis())
    val selectedKey = formatDateOnly(selectedDate)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("每日代办", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (calendarExpanded) {
                        TodoCalendarHeader(
                            calendar = currentMonth,
                            onPrevMonth = { currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) } },
                            onNextMonth = { currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }
                        )
                        val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            weekLabels.forEach { label -> Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold) }
                        }
                        val totalCells = firstWeekday + daysInMonth
                        val rows = (totalCells + 6) / 7
                        var day = 1
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(rows) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    repeat(7) { col ->
                                        val cellIndex = it * 7 + col
                                        val isValid = cellIndex >= firstWeekday && day <= daysInMonth
                                        val dateMillis = if (isValid) {
                                            Calendar.getInstance().apply {
                                                timeInMillis = currentMonth.timeInMillis
                                                set(Calendar.DAY_OF_MONTH, day)
                                                set(Calendar.HOUR_OF_DAY, 0)
                                                set(Calendar.MINUTE, 0)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }.timeInMillis
                                        } else 0L
                                        val dateKey = if (isValid) formatDateOnly(dateMillis) else ""
                                        val hasTodo = isValid && todos.any { it.dateKey == dateKey }
                                        val isSelected = isValid && dateKey == selectedKey
                                        Box(modifier = Modifier.weight(1f).padding(2.dp)) {
                                            if (isValid) {
                                                CalendarDayButton(
                                                    dayText = day.toString(),
                                                    isSelected = isSelected,
                                                    isToday = dateKey == todayKey,
                                                    onClick = { selectedDate = dateMillis }
                                                )
                                                if (hasTodo) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(6.dp)
                                                            .height(6.dp)
                                                            .fillMaxWidth()
                                                    )
                                                }
                                                day++
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(formatDateOnly(selectedDate), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    TextButton(onClick = { calendarExpanded = !calendarExpanded }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (calendarExpanded) "收起月历" else "展开月历")
                    }
                }
            }

            Card(modifier = Modifier.weight(1f)) {
                if (selectedTodos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无代办事项") }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(selectedTodos, key = { it.id }) { todo ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(
                                            color = if (todo.done) androidx.compose.ui.graphics.Color(0xFF2FA66B) else androidx.compose.ui.graphics.Color.Transparent,
                                            shape = RoundedCornerShape(999.dp)
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (todo.done) androidx.compose.ui.graphics.Color(0xFF2FA66B) else androidx.compose.ui.graphics.Color(0xFF9AA0A6),
                                            shape = RoundedCornerShape(999.dp)
                                        )
                                        .padding(2.dp)
                                ) {
                                    androidx.compose.material3.IconButton(
                                        onClick = {
                                            val updated = todos.map { if (it.id == todo.id) it.copy(done = !todo.done) else it }
                                            saveTodos(context, updated)
                                            refresh()
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        content = {
                                            if (todo.done) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "已完成",
                                                    tint = androidx.compose.ui.graphics.Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                                Text(
                                    todo.title,
                                    modifier = Modifier.padding(start = 8.dp),
                                    textDecoration = if (todo.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { addDialogVisible = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 96.dp)
                .size(56.dp),
            shape = RoundedCornerShape(50),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) { Text("＋", fontSize = 24.sp) }
    }

    if (addDialogVisible) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("新增代办事项") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = addTitle, onValueChange = { addTitle = it }, label = { Text("事项内容") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { addDateChoice = 0 }, colors = if (addDateChoice == 0) blueButtonColors() else ButtonDefaults.buttonColors()) { Text("今天") }
                        Button(onClick = { addDateChoice = 1 }, colors = if (addDateChoice == 1) blueButtonColors() else ButtonDefaults.buttonColors()) { Text("明天") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val targetDate = Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                        if (addDateChoice == 1) add(Calendar.DAY_OF_MONTH, 1)
                    }.timeInMillis
                    val item = TodoItem(System.currentTimeMillis(), formatDateOnly(targetDate), addTitle.ifBlank { "未命名代办" }, false)
                    saveTodos(context, listOf(item) + todos)
                    addTitle = ""
                    addDialogVisible = false
                    refresh()
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { addDialogVisible = false }) { Text("取消") } }
        )
    }
}

private data class ExerciseStep(
    val name: String,
    val durationSeconds: Int,
    val restSeconds: Int = 60,
)

private data class ExercisePlan(
    val id: Long,
    val name: String,
    val steps: List<ExerciseStep>,
)

private fun loadExercises(context: Context): List<ExercisePlan> {
    val raw = context.getSharedPreferences("exercise_records", Context.MODE_PRIVATE)
        .getString("exercises", "[]") ?: "[]"
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val stepsArray = item.optJSONArray("steps") ?: JSONArray()
                val steps = buildList {
                    for (j in 0 until stepsArray.length()) {
                        val step = stepsArray.getJSONObject(j)
                        add(
                            ExerciseStep(
                                name = step.optString("name", "步骤 ${j + 1}"),
                                durationSeconds = step.optInt("durationSeconds", 60),
                                restSeconds = step.optInt("restSeconds", 60)
                            )
                        )
                    }
                }
                add(
                    ExercisePlan(
                        id = item.getLong("id"),
                        name = item.getString("name"),
                        steps = steps
                    )
                )
            }
        }
    }.getOrElse { emptyList() }
}

private fun saveExercises(context: Context, exercises: List<ExercisePlan>) {
    val array = JSONArray()
    exercises.forEach { exercise ->
        array.put(JSONObject().apply {
            put("id", exercise.id)
            put("name", exercise.name)
            put("steps", JSONArray().apply {
                exercise.steps.forEach { step ->
                    put(JSONObject().apply {
                        put("name", step.name)
                        put("durationSeconds", step.durationSeconds)
                        put("restSeconds", step.restSeconds)
                    })
                }
            })
        })
    }
    context.getSharedPreferences("exercise_records", Context.MODE_PRIVATE)
        .edit()
        .putString("exercises", array.toString())
        .apply()
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    followSystemTheme: Boolean,
    darkTheme: Boolean,
    onFollowSystemThemeChange: (Boolean) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@runCatching Toast.makeText(context, "无法读取导入文件", Toast.LENGTH_SHORT).show()
                val message = importAllDataFromBackup(context, content)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "导入失败：${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("设置", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("开启主题跟随", fontWeight = FontWeight.Medium)
                        Text("跟随系统明暗模式自动切换", fontSize = 12.sp)
                    }
                    androidx.compose.material3.Switch(
                        checked = followSystemTheme,
                        onCheckedChange = onFollowSystemThemeChange
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("切换主题", fontWeight = FontWeight.Medium)
                        Text(if (followSystemTheme) "主题跟随开启时不可手动切换" else "手动切换深色/浅色主题", fontSize = 12.sp)
                    }
                    androidx.compose.material3.Switch(
                        checked = darkTheme,
                        onCheckedChange = onDarkThemeChange,
                        enabled = !followSystemTheme
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("导入数据") }
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val fileName = "GreenTea_全部数据备份_${SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date(now))}.json"
                            val file = exportAllData(context, fileName)
                            Toast.makeText(context, "已导出到 ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("导出数据") }
                }
            }
        }
    }
}

@Composable
private fun ExerciseScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var exercises by remember { mutableStateOf(loadExercises(context)) }
    var addDialogVisible by remember { mutableStateOf(false) }
    var newExerciseName by remember { mutableStateOf("") }
    var activeExercise by remember { mutableStateOf<ExercisePlan?>(null) }
    var editingExercise by remember { mutableStateOf<ExercisePlan?>(null) }
    var deleteTarget by remember { mutableStateOf<ExercisePlan?>(null) }

    fun refresh() { exercises = loadExercises(context) }

    Box(modifier = modifier.fillMaxSize()) {
        if (activeExercise == null) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("锻炼", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (exercises.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无锻炼计划") }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        items(exercises.chunked(2), key = { row -> row.first().id }) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                row.forEach { exercise ->
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(exercise.name, fontWeight = FontWeight.Bold)
                                                    Text("共 ${exercise.steps.size} 步")
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(onClick = { activeExercise = exercise }, modifier = Modifier.weight(1f), colors = blueButtonColors()) { Text("开始") }
                                                    Button(onClick = { editingExercise = exercise }, modifier = Modifier.weight(1f), colors = blueButtonColors()) { Text("编辑") }
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(999.dp))
                                                    .border(1.5.dp, androidx.compose.ui.graphics.Color.Red, RoundedCornerShape(999.dp))
                                            ) {
                                                Button(
                                                    onClick = { deleteTarget = exercise },
                                                    modifier = Modifier.size(24.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Transparent, contentColor = androidx.compose.ui.graphics.Color.Red),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                                ) { Text("×", color = androidx.compose.ui.graphics.Color.Red, fontSize = 14.sp) }
                                            }
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { addDialogVisible = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 96.dp)
                    .size(56.dp),
                shape = RoundedCornerShape(50),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF2FA66B), contentColor = androidx.compose.ui.graphics.Color.White)
            ) { Text("＋", fontSize = 24.sp) }
        } else {
            ExerciseRunScreen(
                exercise = activeExercise!!,
                onExit = { activeExercise = null },
                onPause = { /* 暂停预留 */ }
            )
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除运动") },
            text = { Text("确定要删除「${target.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    exercises = exercises.filterNot { it.id == target.id }
                    saveExercises(context, exercises)
                    deleteTarget = null
                    refresh()
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }

    if (addDialogVisible) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("新建锻炼") },
            text = {
                OutlinedTextField(
                    value = newExerciseName,
                    onValueChange = { newExerciseName = it },
                    label = { Text("锻炼名字") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newExerciseName.isBlank()) return@TextButton
                    val exercise = ExercisePlan(id = System.currentTimeMillis(), name = newExerciseName.trim(), steps = listOf(ExerciseStep("第一步骤", 60, 60)))
                    exercises = listOf(exercise) + exercises
                    saveExercises(context, exercises)
                    newExerciseName = ""
                    addDialogVisible = false
                    editingExercise = exercise
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { addDialogVisible = false }) { Text("取消") } }
        )
    }

    editingExercise?.let { exercise ->
        ExerciseStepEditorDialog(
            exercise = exercise,
            onDismiss = { editingExercise = null },
            onSave = { updated ->
                exercises = exercises.map { if (it.id == updated.id) updated else it }
                saveExercises(context, exercises)
                editingExercise = null
                refresh()
            }
        )
    }
}

@Composable
private fun ExerciseStepEditorDialog(
    exercise: ExercisePlan,
    onDismiss: () -> Unit,
    onSave: (ExercisePlan) -> Unit,
) {
    var stepIndex by remember { mutableStateOf(0) }
    val steps = remember(exercise.steps) { exercise.steps.toMutableList().ifEmpty { mutableListOf(ExerciseStep("第一步骤", 60, 60)) } }
    var stepName by remember { mutableStateOf(steps[0].name) }
    var stepDuration by remember { mutableStateOf(steps[0].durationSeconds.toString()) }
    var restDuration by remember { mutableStateOf(steps[0].restSeconds.toString()) }

    fun persistCurrentStep() {
        steps[stepIndex] = ExerciseStep(
            stepName.ifBlank { "步骤 ${stepIndex + 1}" },
            stepDuration.toIntOrNull() ?: 60,
            restDuration.toIntOrNull() ?: 60
        )
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text(exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("步骤 ${stepIndex + 1}/${steps.size}")
                OutlinedTextField(value = stepName, onValueChange = { stepName = it }, label = { Text("步骤名称") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = stepDuration, onValueChange = { stepDuration = it.filter(Char::isDigit) }, label = { Text("持续时间（秒）") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = restDuration, onValueChange = { restDuration = it.filter(Char::isDigit) }, label = { Text("休息时间（秒）") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    persistCurrentStep()
                    if (stepIndex > 0) {
                        stepIndex--
                        stepName = steps[stepIndex].name
                        stepDuration = steps[stepIndex].durationSeconds.toString()
                        restDuration = steps[stepIndex].restSeconds.toString()
                    }
                }) { Text("上一步") }
                TextButton(onClick = {
                    persistCurrentStep()
                    onSave(exercise.copy(steps = steps.toList()))
                }) { Text("保存") }
                TextButton(onClick = {
                    persistCurrentStep()
                    if (stepIndex < steps.lastIndex) {
                        stepIndex++
                    } else {
                        steps.add(ExerciseStep("第 ${steps.size + 1} 步", 60, 60))
                        stepIndex = steps.lastIndex
                    }
                    stepName = steps[stepIndex].name
                    stepDuration = steps[stepIndex].durationSeconds.toString()
                    restDuration = steps[stepIndex].restSeconds.toString()
                }) { Text("下一步") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private data class ExerciseRunState(
    val stepIndex: Int,
    val phase: String,
    val phaseEndsAt: Long,
    val running: Boolean,
    val pausedAt: Long? = null,
)

private fun loadExerciseRunState(context: Context): ExerciseRunState? {
    val raw = context.getSharedPreferences("exercise_run_state", Context.MODE_PRIVATE)
        .getString("state", null) ?: return null
    return runCatching {
        JSONObject(raw).let {
            ExerciseRunState(
                stepIndex = it.getInt("stepIndex"),
                phase = it.getString("phase"),
                phaseEndsAt = it.getLong("phaseEndsAt"),
                running = it.getBoolean("running"),
                pausedAt = if (it.has("pausedAt") && !it.isNull("pausedAt")) it.getLong("pausedAt") else null
            )
        }
    }.getOrNull()
}

private fun saveExerciseRunState(context: Context, state: ExerciseRunState?) {
    val prefs = context.getSharedPreferences("exercise_run_state", Context.MODE_PRIVATE)
    if (state == null) {
        prefs.edit().remove("state").apply()
        return
    }
    prefs.edit().putString("state", JSONObject().apply {
        put("stepIndex", state.stepIndex)
        put("phase", state.phase)
        put("phaseEndsAt", state.phaseEndsAt)
        put("running", state.running)
        put("pausedAt", state.pausedAt)
    }.toString()).apply()
}

@Composable
private fun rememberBeepPlayer(): ToneGenerator? {
    return remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull() }
}

@Composable
private fun ExerciseRunScreen(
    exercise: ExercisePlan,
    onExit: () -> Unit,
    onPause: () -> Unit,
) {
    val context = LocalContext.current
    var runState by remember {
        mutableStateOf(
            loadExerciseRunState(context) ?: ExerciseRunState(
                0,
                "work",
                System.currentTimeMillis() + ((exercise.steps.firstOrNull()?.durationSeconds ?: 0) * 1000L),
                true
            )
        )
    }
    val currentStep = exercise.steps.getOrNull(runState.stepIndex)
    val totalSeconds = if (runState.phase == "work") currentStep?.durationSeconds ?: 0 else currentStep?.restSeconds ?: 0
    var tick by remember { mutableStateOf(System.currentTimeMillis()) }
    var lastBeepSecond by remember { mutableStateOf(-1) }
    val remainingSeconds = if (runState.running) {
        ((runState.phaseEndsAt - tick) / 1000L).coerceAtLeast(0).toInt()
    } else {
        runState.pausedAt?.let { ((runState.phaseEndsAt - it) / 1000L).coerceAtLeast(0).toInt() } ?: ((runState.phaseEndsAt - tick) / 1000L).coerceAtLeast(0).toInt()
    }
    val color = if (runState.phase == "work") androidx.compose.ui.graphics.Color(0xFF4F7DF3) else androidx.compose.ui.graphics.Color(0xFF2FA66B)

    LaunchedEffect(runState.stepIndex, runState.phase, runState.running, runState.phaseEndsAt) {
        if (!runState.running) return@LaunchedEffect
        while (runState.running) {
            tick = System.currentTimeMillis()
            val left = ((runState.phaseEndsAt - tick) / 1000L).coerceAtLeast(0).toInt()
            if (left in 1..5 && left != lastBeepSecond) {
                lastBeepSecond = left
                ToneGenerator(AudioManager.STREAM_ALARM, 100).startTone(ToneGenerator.TONE_PROP_BEEP, 140)
            }
            if (left <= 0) break
            kotlinx.coroutines.delay(250)
        }
        if (!runState.running) return@LaunchedEffect
        val nextState = if (runState.phase == "work") {
            val rest = currentStep?.restSeconds ?: 0
            runState.copy(phase = "rest", phaseEndsAt = System.currentTimeMillis() + rest * 1000L)
        } else {
            if (runState.stepIndex < exercise.steps.lastIndex) {
                val nextIndex = runState.stepIndex + 1
                val nextDuration = exercise.steps[nextIndex].durationSeconds
                runState.copy(stepIndex = nextIndex, phase = "work", phaseEndsAt = System.currentTimeMillis() + nextDuration * 1000L)
            } else {
                saveExerciseRunState(context, null)
                onExit()
                return@LaunchedEffect
            }
        }
        runState = nextState
        saveExerciseRunState(context, nextState)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                androidx.compose.material3.CircularProgressIndicator(
                    progress = if (totalSeconds == 0) 0f else remainingSeconds.toFloat() / totalSeconds.toFloat(),
                    modifier = Modifier.height(220.dp).fillMaxWidth(0.65f),
                    strokeWidth = 10.dp,
                    color = color
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(currentStep?.name ?: "完成", fontWeight = FontWeight.Bold)
                    Text(formatSeconds(remainingSeconds))
                    Text(if (runState.phase == "work") "运动中" else "休息中")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { saveExerciseRunState(context, null); onExit() }, modifier = Modifier.weight(1f), colors = redButtonColors()) { Text("退出运动") }
                Button(onClick = {
                    val now = System.currentTimeMillis()
                    runState = if (runState.running) {
                        runState.copy(running = false, pausedAt = now)
                    } else {
                        runState.copy(
                            running = true,
                            phaseEndsAt = runState.phaseEndsAt + (now - (runState.pausedAt ?: now)),
                            pausedAt = null
                        )
                    }
                    saveExerciseRunState(context, runState)
                    onPause()
                }, modifier = Modifier.weight(1f), colors = blueButtonColors()) { Text(if (runState.running) "暂停运动" else "继续运动") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    records: List<BloodPressureRecord>,
    dateFilter: String?,
    onDismiss: () -> Unit,
    onFilterChange: (String?) -> Unit,
    onDelete: (BloodPressureRecord) -> Unit,
) {
    val groupedDates = remember(records) { records.map { it.dateKey }.distinct().sortedDescending() }
    val filteredRecords = if (dateFilter == null) records else records.filter { it.dateKey == dateFilter }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("历史记录", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("日期筛选")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(dateFilter ?: "全部日期", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onFilterChange(null) }) { Text("全部") }
                groupedDates.take(5).forEach { date ->
                    TextButton(onClick = { onFilterChange(date) }) { Text(date) }
                }
            }
            if (filteredRecords.isEmpty()) {
                Text("暂无记录")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredRecords, key = { it.id }) { record ->
                        Card {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(formatDateTime(record.timestamp), fontWeight = FontWeight.Bold)
                                Text("高压：${record.systolic}  低压：${record.diastolic}  脉搏：${record.pulse}")
                                Button(onClick = { onDelete(record) }) { Text("删除") }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    GreenTeaTheme { BloodPressureApp() }
}
