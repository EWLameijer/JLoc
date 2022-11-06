package net.ericwubbo.jloc

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

var overallReport = Report(0, 0, 0, 0, 0)

fun main() {
    val dir = getDirectory()
    listAllFiles(File(dir))
    val summaryLine = getSummaryLine()
    println("\n" + summaryLine)
    writeSummaryToFile(dir, summaryLine)
    println("Press <Enter> to exit")
    readln()
}

private fun writeSummaryToFile(dir: String, reportLine: String) {
    val directoryHash = dir.map { if (it.isLetterOrDigit()) it.lowercase() else "_" }.joinToString("")
    val reportFile = File("report_$directoryHash.txt")
    val lines = if (reportFile.exists()) reportFile.readLines() else listOf()
    reportFile.writeText((listOf(reportLine) + lines).joinToString("\n"))
}

private fun getSummaryLine(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val now = formatter.format(LocalDateTime.now())
    return "$now: $overallReport"
}

private fun getDirectory(): String {
    print("Please give the name of the directory to scan: ")
    val dir = readln()

    val directory = File(dir)
    if (!directory.exists()) {
        println("'$dir' is not a valid directory!")
        exitProcess(-1)
    }
    return dir
}

data class Report(val total: Int, val blank: Int, val opening: Int, val comments: Int, val code: Int) {
    operator fun plus(other: Report): Report {
        return Report(
            total + other.total,
            blank + other.blank,
            opening + other.opening,
            comments + other.comments,
            code + other.code
        )
    }

    override fun toString() = "total: $total, blank: $blank, opening: $opening, comments: $comments, code: $code"
}

fun listAllFiles(dir: File) {
    for (file in dir.listFiles()!!) {
        if (file.name.endsWith(".java")) overallReport += analyze(file!!)
        else if (file.isDirectory) listAllFiles(file)
    }
}

fun analyze(file: File): Report {
    val lines = file.readLines().map { it.trim() }
    print(file.name + ": ")
    val (blankLines, nonBlankLines) = lines.partition { it.isEmpty() }
    val numBlankLines = blankLines.size
    val (openingLines, otherLines) = nonBlankLines.span { it.isStartingLine() }
    val numOpeningLines = openingLines.size
    val commentLines = countCommentLines(otherLines)
    val codeLines = otherLines.size - commentLines
    val report = Report(lines.size, numBlankLines, numOpeningLines, commentLines, codeLines)
    println(report)
    return report
}

private fun countCommentLines(otherLines: List<String>): Int {
    var commentMode = false
    var commentLines = 0
    for (line in otherLines) {
        if (line.startsWith("//")) commentLines++
        else if (line.startsWith("/*")) {
            commentLines++
            commentMode = true
        } else if (commentMode) commentLines++
        if (line.endsWith("*/")) commentMode = false
    }
    return commentLines
}

private fun String.isStartingLine() = startsWith("import") || startsWith("package")

private fun <E> List<E>.span(predicate: (E) -> Boolean): Pair<List<E>, List<E>> {
    val first = takeWhile(predicate)
    val second = dropWhile(predicate)
    return first to second
}