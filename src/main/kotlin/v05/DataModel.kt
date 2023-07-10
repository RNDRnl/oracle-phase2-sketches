package v05

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.ColumnName
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.io.toStandaloneHTML
import org.jetbrains.kotlinx.dataframe.name
import org.openrndr.events.Event
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import v05.filters.DateFilterModel
import v05.filters.FacultyFilterModel
import v05.filters.FilterSet
import v05.libs.brackets
import v05.libs.histogramOf
import v05.filters.TopicFilterModel
import java.awt.geom.Arc2D
import java.io.Serializable
import java.math.BigDecimal

enum class SortMode {
    FACULTY_YEAR,
    YEAR_FACULTY
}

class DataModel(val frame: Rectangle = Rectangle(0.0, 0.0, 100.0, 100.0)) {
     val topicsDf = DataFrame.readCSV("offline-data/zeroshot-all-data-v5.csv")
        .add("topic") {
            val r = getRow(index()).toMap().values
            val i = r.indexOf(rowMax())
            topicNames[i]
        }
        .move { pathOf("topic") }.toLeft()
         .convert { colsOf<Number>() }.with { it.toDouble() }
        .merge { colsOf<Double>()  }.into("scores")

    private val articlesDf = DataFrame.read("offline-data/all-data-v5.csv")
        .fillNulls("faculty").with { "Unknown Faculty" }
        .convert("faculty").with { (it as String).correctedFaculty() }
        .fillNulls("abstract").with { "No abstract provided" }
        .fillNulls("author").with { "Unknown Author" }
        .fillNulls("contributor","department").with { "" }
        .fillNulls("publication year").with { "No date" }
        .add("label") { "" }
        .add(topicsDf)


    val pos by column<Vector2>()

    private val pointsDf = DataFrame.read("offline-data/umap-2d-v5.csv")
        .take(articlesDf.rowsCount())
        .merge { "x"<Double>() and "y"() }
        .by { Vector2(it[0], it[1]) }
        .into(pos)

    val dataFrame = pointsDf.add(articlesDf)

    val articles = articlesDf.toListOf<Article>()
    val points = pointsDf[pos].toList().run { map(this.bounds, frame) }

    val densities = DoubleArray(points.size)
    val rotations = DoubleArray(points.size)
    val pointsToArticles = (points zip articles).toMap()
    val pointsToArticleIndices = (points zip articles.indices).toMap()
    val articlesToPoints = pointsToArticles.entries.associateBy({ it.value }) { it.key }

    // showcases

    val topGraduates = "TOP GRADUATES" to articlesDf.filter { "best_student"<Int>() == 1 }.toListOf<Article>()
    val scTopics = articlesDf.select { cols { it.name.startsWith("topic_") } }.columns().map {
        val name = it.name
        val articles = articlesDf.filter { name<Int>() == 1 }.toListOf<Article>()
        name.drop(8).uppercase() to articles
    }
    val showcases = listOf(topGraduates) + scTopics


    var yearLabels = mutableListOf<Int>()
    var facultyLabels = mutableListOf<String>()

    val articlesSorted = mapOf(
        SortMode.FACULTY_YEAR to articles.map { it.copy() }.sortedBy { it.faculty }.sortedBy { it.year }.onEach {
            if(!yearLabels.contains(it.year)) {
                it.label = it.year.toString()
            }
            yearLabels.add(it.year)
        },
        SortMode.YEAR_FACULTY to articles.map { it.copy() }.sortedBy { it.year }.sortedBy { it.faculty }.onEach {
            if(!facultyLabels.contains(it.faculty)) {
                it.label = it.faculty
            }
            facultyLabels.add(it.faculty)
        }
    )
}


enum class FilterState {
    NO_FILTER,
    GLOBAL_FACULTY,
    GLOBAL_TOPIC,
    GLOBAL_ARTICLE,
    SHOWCASE_FACULTY,
    SHOWCASE_ARTICLE
}


class State(val model: DataModel) {

    lateinit var facultyFilter: FacultyFilterModel
    lateinit var topicFilter: TopicFilterModel
    lateinit var dateFilter: DateFilterModel

    var filterState = FilterState.NO_FILTER

    fun filterChanged() {
        val filteredFaculties = facultyFilter.filteredList
        val filteredTopics = topicFilter.filteredList
        val filteredDates = dateFilter.filteredList[0] to dateFilter.filteredList[1]
        filterSet = FilterSet(filteredFaculties, filteredTopics, filteredDates)
        changed.trigger((Unit))
    }
    val changed = Event<Unit>()

    val kdtree = model.points.kdTree()

    var idle = true
        set(value) {
            if (field != value) {
                field = value
                changed.trigger(Unit)
            }
        }

    var zoom = 0.0
    var radius = 40.0
    var lookAt = model.frame.center
        set(value) {
            activePoints = findActivePoints(value, radius)
        }

    var filtered = model.pointsToArticles
        set(value) {
            activePoints = findActivePoints(model.frame.center, radius)
            field = value
        }

    fun findActivePoints(pos: Vector2, radius: Double): Map<Vector2, Article> {
        val nearest = kdtree.findAllInRadius(pos, radius).sortedBy { it.distanceTo(pos) }
        val active = mutableMapOf<Vector2, Article>()

        for(p in nearest) {
            val pap = filtered[p]
            if(pap != null) {
                active[p] = pap
            }
        }

        return active
    }

    var activePoints = findActivePoints(model.frame.center, radius)

    var filterSet = FilterSet.EMPTY
        set(value) {
            if (value != field) {
                field = value
                filtered = if (value == FilterSet.EMPTY) {
                    model.pointsToArticles
                } else {
                    val start = System.currentTimeMillis()
                    val r = model.pointsToArticles.filter {
                        it.value.year.toInt() in value.dates.first..value.dates.second &&
                        it.value.faculty in value.faculties &&
                                it.value.topic in value.topics

                    }
                    val end = System.currentTimeMillis()
                    println("filtering took ${end-start}ms")
                    r
                }
                changed.trigger(Unit)
            }
        }
}



data class Article(
    @ColumnName("title")
    val title: String,

    @ColumnName("author")
    val author: String,

    @ColumnName("contributor")
    val contributor: String,

    @ColumnName("faculty")
    val faculty: String,

    @ColumnName("department")
    val department: String,

    @ColumnName("publication year")
    val year: Int,

    @ColumnName("abstract")
    val abstract: String,

//    @ColumnName("uuid")
//    val uuid: String,

    @ColumnName("topic")
    val topic: String,

    @ColumnName("label")
    var label: String,

    @ColumnName("scores")
    val topicScores: List<Double>,

):Serializable

fun main() {
    val dmn = DataModel(Rectangle.fromCenter(Vector2.ZERO, 1920.0, 1080.0))
    dmn.topicsDf.toStandaloneHTML().openInBrowser()

    println(dmn.scTopics)
    println(dmn.scTopics.map { it.first to it.second.size })
}
