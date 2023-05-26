package v04

import classes.ArticleData
import classes.Entry
import classes.facultyNames
import classes.skipPoints
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.column
import org.jetbrains.kotlinx.dataframe.api.convert
import org.jetbrains.kotlinx.dataframe.api.join
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.size
import org.openrndr.events.Event
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.File
import java.io.FileReader
import java.nio.file.Paths
import kotlin.io.path.bufferedReader

class Article(title: String, author: String, contributor: String, publicationYear: String, faculty: String, department: String)

fun main() {
    val dmn = DataModelNew(Rectangle.EMPTY)
    println(dmn.points.size())
    println(dmn.articles.size())
}

class DataModelNew(val frame: Rectangle) {

    private val pointsDf = DataFrame.read("data/data-umap-highlight.csv", header = listOf("uuid","x","y"))
    private val articlesDf = DataFrame.read("data/data-umap-highlight.csv", header = listOf("uuid","title","author","contributor","publication year","faculty","department"))

    val uuid by column<String>()

    val x by column<Double>()
    val y by column<Double>()

    val title by column<String>()
    val author by column<String>()
    val contributor by column<String>()
    val publicationYear by column<String>()
    val faculty by column<String>()
    val department by column<String>()

    val points = pointsDf.convert { x and y }.to<Vector2>()
    val articles = articlesDf.convert {
        title and author and contributor and publicationYear and faculty and department
    }.to<ArticleEntity>()

    val data = points.join(articles)

    /*val points = csvReader().readAllWithHeader(File("data/corrected-15.csv")).drop(skipPoints).map {
        Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
    }.run { map(this.bounds, frame) }

    val kdtree = points.kdTree()

    val pointIndices = points.indices.map { Pair(points[it], it) }.associate { it }
    var filteredIndices = pointIndices

    var activePoints = findActivePoints(frame.center, radius)
        set(value) {
            field = value
            changed.trigger(Unit)
        }

    val changed = Event<Unit>()
    var radius = 40.0

    var lookAt = frame.center
        set(value) {
            activePoints = findActivePoints(value, radius)
        }

    fun findActivePoints(pos: Vector2, radius: Double): List<Int> {
        return kdtree.findAllInRadius(pos, radius)
            .sortedBy { it.distanceTo(pos) }
            .map {
                pointIndices[it] ?: error("point not found")
            }
    }

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
    val pointsToArticles = (points zip articles).associate { it }

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
    val facultyIndexes = loadFacultyIndexes()*/
}