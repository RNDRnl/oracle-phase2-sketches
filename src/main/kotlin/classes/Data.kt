package classes

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Spherical
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.File
import java.io.FileReader
import java.nio.file.Paths
import kotlin.io.path.bufferedReader

class Data {

    fun loadPoints(): List<Vector3> {
        val pointsData = csvReader().readAllWithHeader(File("data/corrected-15.csv")).drop(skipPoints).map {
            Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
        }

        val bounds = pointsData.bounds
        val llbounds = Rectangle(-240.0, 10.0, 480.0, 160.0)
        val latlon = pointsData.map { it.map(bounds, llbounds) }


        return latlon.map { Spherical(it.x, it.y, 10.0).cartesian }

    }

    val points = loadPoints()
    val pointIndices = points.indices.map { Pair(points[it], it) }.associate { it }

    val kdtree = points.kdTree()
    var activePoints = listOf<Int>()
    var activeFacultyColors = listOf<ColorRGBa>()

    fun loadArticles(): List<ArticleData> {
        return Gson().fromJson(FileReader(File("offline-data/mapped-v2r1.json")), Array<Entry>::class.java)
            .drop(skipPoints).map {
                ArticleData(
                    it.ogdata["Title"] as String,
                    it.ogdata["Author"] as String,
                    it.ogdata["Faculty"] as String,
                    it.ogdata["Department"] as String,
                    it.ogdata["Date"] as String
                )
            }
    }
    val articles = loadArticles()

    fun loadFacultyIndexes(): List<Int> {
        val lookUp = mutableMapOf<String, String>() // List<

        val allText = Paths.get("data/faculty-corrections.csv").bufferedReader()
        CSVParser(allText, CSVFormat.newFormat(';')).onEach {
            lookUp[it.get(0)] = it.get(1)
        }

        val correctedFaculties = articles.map {
            val correctedFaculty = lookUp[it.faculty.lowercase()]
            correctedFaculty
        }


        val indexes = correctedFaculties.mapIndexed { i, it ->
            if(it != null) {
                facultyNames.indexOf(it)
            } else {
                8
            }
        }


        return indexes
    }
    val facultyIndexes = loadFacultyIndexes()

    var zoom = 0.5
    var lookAt = Vector3.UNIT_Z * -10.0
        set(value) {
            activePoints = findActivePoints()
            activeFacultyColors = activePoints.map { it ->
                val fi = facultyIndexes[it].let { if (it == -1) 9 else it }
                facultyColors.getOrNull(fi)?: ColorRGBa.WHITE
            }
            field = value
        }

    fun findActivePoints(): List<Int> {
        return kdtree.findAllInRadius(lookAt, (1.0 - zoom).coerceAtLeast(0.1)).sortedBy { it.distanceTo(lookAt) }.map {
            pointIndices[it] ?: error("point not found")
        }
    }
}

const val skipPoints = 142082
class Entry(val type:String, val name:String, var ogdata:Map<String, Any> = emptyMap())
class ArticleData(val title: String, val author:String, val faculty:String, val department: String, val date:String) {
    fun toList():List<String> {
        return listOf(title, author, faculty, department, date)
    }
}

val facultyNames = listOf(
    "Architecture and the Built Environment (ABE)",
    "Aerospace Engineering (AE)",
    "Applied Sciences (AS)",
    "Civil Engineering and Geosciences (CEG)",
    "Electrical Engineering, Mathematics & Computer Science (EEMCS)",
    "Industrial Design Engineering (IDE)",
    "Mechanical, Maritime and Materials Engineering (3mE)",
    "Technology, Policy and Management (TPM)",
    "Unknown Faculty (?)"
)
var facultyColors = listOf(
    ColorRGBa.fromHex("2D5BFF"),
    ColorRGBa.fromHex("FF9254"),
    ColorRGBa.fromHex("C197FB"),
    ColorRGBa.fromHex("E1A400"),
    ColorRGBa.fromHex("19CC78"),
    ColorRGBa.fromHex("00A8B4"),
    ColorRGBa.fromHex("E54949"),
    ColorRGBa.fromHex("FFAD8F"),
    ColorRGBa.fromHex("A5A5A5")
)