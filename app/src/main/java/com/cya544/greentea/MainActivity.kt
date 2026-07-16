package com.cya544.greentea

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import android.net.Uri
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.view.View
import android.view.animation.DecelerateInterpolator
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
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
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_RECORDS, array.toString())
        .apply()
}

private fun sanitizeFileName(name: String): String = name.replace(Regex("[\\/:*?\"<>|]"), "_")

private fun exportXlsx(context: Context, records: List<BloodPressureRecord>, fileName: String): File {
    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloads.exists()) downloads.mkdirs()
    val file = File(downloads, sanitizeFileName(fileName))

    fun xmlEsc(value: String): String = buildString {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }

    fun cellRef(col: Int, row: Int): String {
        var n = col
        var result = ""
        while (n >= 0) {
            result = ('A' + (n % 26)) + result
            n = n / 26 - 1
        }
        return "$result$row"
    }

    fun sheetXml(): String {
        val rows = buildString {
            append("<row r=\"1\">")
            listOf("序号", "时间", "高压（mmHg）", "低压（mmHg）", "脉搏（bpm）").forEachIndexed { index, title ->
                append("<c r=\"${cellRef(index, 1)}\" t=\"inlineStr\"><is><t>${xmlEsc(title)}</t></is></c>")
            }
            append("</row>")
            records.forEachIndexed { idx, record ->
                val rowNum = idx + 2
                append("<row r=\"$rowNum\">")
                append("<c r=\"${cellRef(0, rowNum)}\"><v>${idx + 1}</v></c>")
                append("<c r=\"${cellRef(1, rowNum)}\" t=\"inlineStr\"><is><t>${xmlEsc(formatDateTime(record.timestamp))}</t></is></c>")
                append("<c r=\"${cellRef(2, rowNum)}\"><v>${record.systolic}</v></c>")
                append("<c r=\"${cellRef(3, rowNum)}\"><v>${record.diastolic}</v></c>")
                append("<c r=\"${cellRef(4, rowNum)}\"><v>${record.pulse}</v></c>")
                append("</row>")
            }
        }
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>$rows</sheetData>
            </worksheet>
        """.trimIndent()
    }

    val contentTypes = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        </Types>
    """.trimIndent()

    val rootRels = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    val workbookXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="血压记录" sheetId="1" r:id="rId1"/>
          </sheets>
        </workbook>
    """.trimIndent()

    val workbookRels = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
        </Relationships>
    """.trimIndent()

    ZipOutputStream(FileOutputStream(file)).use { zip ->
        fun add(name: String, text: String) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(text.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        add("[Content_Types].xml", contentTypes)
        add("_rels/.rels", rootRels)
        add("xl/workbook.xml", workbookXml)
        add("xl/_rels/workbook.xml.rels", workbookRels)
        add("xl/worksheets/sheet1.xml", sheetXml())
    }
    return file
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

private fun importRecordsFromXlsx(context: Context, uri: android.net.Uri): Int {
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
        val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.xlsx")
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
    SAVE,
    HISTORY,
    // TODO,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BloodPressureApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(TabItem.SAVE) }
    var records by remember { mutableStateOf(emptyList<BloodPressureRecord>()) }
    var deleteTarget by remember { mutableStateOf<BloodPressureRecord?>(null) }

    fun refresh() {
        records = loadRecords(context)
    }

    LaunchedEffect(Unit) {
        records = loadRecords(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == TabItem.SAVE,
                    onClick = { selectedTab = TabItem.SAVE },
                    icon = { Text("＋") },
                    label = { Text("保存记录") }
                )
                NavigationBarItem(
                    selected = selectedTab == TabItem.HISTORY,
                    onClick = { selectedTab = TabItem.HISTORY },
                    icon = { Text("≡") },
                    label = { Text("历史记录") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            TabItem.SAVE -> SaveScreen(
                context = context,
                modifier = Modifier.padding(innerPadding),
                onRecordsChanged = { refresh() }
            )
            TabItem.HISTORY -> HistoryScreen(
                context = context,
                records = records,
                modifier = Modifier.padding(innerPadding),
                onDelete = {
                    deleteRecord(context, it.id)
                    refresh()
                },
                onImported = { refresh() }
            )
            // TabItem.TODO -> TodoScreen(modifier = Modifier.padding(innerPadding))
        }
    }

        deleteTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("确认删除") },
                text = { Text("确定要删除这条记录吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        deleteRecord(context, target.id)
                        refresh()
                        deleteTarget = null
                    }) { Text("删除") }
                },
                dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
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
                            TimePickerDialog(context, { _, hh, mm ->
                                cal.set(y, m, d, hh, mm, 0)
                                selectedDateTime = cal.timeInMillis
                            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }, colors = greenButtonColors()) { Text("修改时间") }
                    Button(onClick = { selectedDateTime = System.currentTimeMillis() }, colors = blueButtonColors()) { Text("更新时间") }
                }
                Text("当前时间：${formatDateTime(selectedDateTime)}")
                Button(onClick = { saveNewRecord() }, modifier = Modifier.fillMaxWidth(), colors = blueButtonColors()) { Text("保存记录") }
            }
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
                    Card {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(formatDateTime(record.timestamp), fontWeight = FontWeight.Bold)
                            Text("高压：${record.systolic}  低压：${record.diastolic}  脉搏：${record.pulse}")
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
                modifier = Modifier.fillMaxWidth().padding(16.dp)
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
            exportMessage = "已导出到 ${exportXlsx(context, monthlyRecords, "血压记录_${monthText}.xlsx").absolutePath}"
            exportDialogVisible = false
        },
        onExportWeek = {
            val now = System.currentTimeMillis()
            val start = startOfWeek(now)
            val end = endOfWeek(now)
            val weekRecords = records.filter { it.timestamp in start..end }
            val startText = SimpleDateFormat("yyyy年M月d号", Locale.getDefault()).format(Date(start))
            val endText = SimpleDateFormat("d号", Locale.getDefault()).format(Date(end))
            exportMessage = "已导出到 ${exportXlsx(context, weekRecords, "血压记录_${startText}至${endText}.xlsx").absolutePath}"
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
                exportMessage = "已导出到 ${exportXlsx(context, customRecords, fileName).absolutePath}"
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onPageChange(0) },
                enabled = canGoPrev,
                modifier = Modifier.weight(0.85f)
            ) { Text("<<") }
            Button(
                onClick = { onPageChange((page - 1).coerceAtLeast(0)) },
                enabled = canGoPrev,
                modifier = Modifier.weight(0.85f)
            ) { Text("<") }
            OutlinedTextField(
                value = jumpInput,
                onValueChange = onJumpInputChange,
                modifier = Modifier.weight(1.4f),
                label = { Text("页码跳转") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val target = jumpInput.toIntOrNull()?.minus(1) ?: return@KeyboardActions
                    onJumpToPage(target)
                })
            )
            Button(
                onClick = { onPageChange((page + 1).coerceAtMost(maxJump)) },
                enabled = canGoNext,
                modifier = Modifier.weight(0.85f)
            ) { Text(">") }
            Button(
                onClick = { onPageChange(maxJump) },
                enabled = canGoNext,
                modifier = Modifier.weight(0.85f)
            ) { Text(">>") }
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

@Composable
private fun TodoScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("每日代办页面暂未实现")
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
