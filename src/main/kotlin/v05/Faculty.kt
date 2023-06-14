package v05

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.read
import org.openrndr.color.ColorRGBa

val facultyNames = listOf(
    "Architecture and The Built Environment",
    "Aerospace Engineering",
    "Applied Sciences",
    "Civil Engineering and Geosciences",
    "Electrical Engineering, Mathematics & Computer Science",
    "Industrial Design Engineering",
    "Mechanical, Maritime and Materials Engineering",
    "Technology, Policy and Management",
    "Unknown Faculty"
)

val corrections by lazy {
    val df = DataFrame.read("data/faculty-corrections-v2.csv")
    df.rows().associate { it[1] as String to it[0] as String }
}

fun String.correctedFaculty(): String = corrections.getOrDefault(this.lowercase(), this)


val facultyAbbreviations = listOf(
    "ABE",
    "AE",
    "AS",
    "CEG",
    "EMCS",
    "IDE",
    "3ME",
    "TPM",
    "??"
)

val facultyColors = listOf(
    ColorRGBa.fromHex("2D5BFF"),
    ColorRGBa.fromHex("FF9254"),
    ColorRGBa.fromHex("C197FB"),
    ColorRGBa.fromHex("E1A400"),
    ColorRGBa.fromHex("19CC78"),
    ColorRGBa.fromHex("00A8B4"),
    ColorRGBa.fromHex("E54949"),
    ColorRGBa.fromHex("FFAD8F"),
    ColorRGBa.GRAY.mix(ColorRGBa.WHITE, 0.5)
)

val facultyToColor = (facultyNames zip facultyColors).toMap()

fun String.facultyColor(): ColorRGBa = facultyToColor[this] ?: facultyColors.last()