package v05

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.read
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb

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

val facultyToAbbreviation = (facultyNames zip facultyAbbreviations).toMap()

val facultyColors = listOf(
    rgb("2D5BFF"),
    rgb("FF9254"),
    rgb("C197FB"),
    rgb("E1A400"),
    rgb("19CC78"),
    rgb("00A8B4"),
    rgb("E54949"),
    rgb("FFAD8F"),
    ColorRGBa.GRAY.mix(ColorRGBa.WHITE, 0.5)
)

val facultyToColor = (facultyNames zip facultyColors).toMap()

fun String.facultyColor(): ColorRGBa = facultyToColor[this] ?: facultyColors.last()
fun String.facultyAbbreviation(): String = facultyToAbbreviation[this] ?: facultyAbbreviations.last()