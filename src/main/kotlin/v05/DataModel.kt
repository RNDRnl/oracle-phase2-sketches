package v05

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.ColumnName
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.io.toStandaloneHTML
import org.openrndr.events.Event
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import v05.filters.FilterSet
import java.io.Serializable
import java.math.BigDecimal

class DataModel(val frame: Rectangle = Rectangle(0.0, 0.0, 100.0, 100.0)) {

    private val topicsDf = DataFrame.readCSV("offline-data/zeroshot-all-data-v3.csv")
        .add("topic") {
            val r = getRow(index()).toMap().values
            val i = r.indexOf(rowMax())
            topicNames[i]
        }
        .move { pathOf("topic") }.toLeft()
        .merge { colsOf<Number>()  }.into("scores")


    private val articlesDf = DataFrame.read("offline-data/all-data-v3.csv")
        .select{ cols(0..7) }
        .fillNulls("faculty").with { "Unknown Faculty" }
        .convert("faculty").with { (it as String).correctedFaculty() }
        .fillNulls("abstract").with { "No abstract provided" }
        .fillNulls("author").with { "Unknown Author" }
        .fillNulls("contributor","department").with { "" }
        .fillNulls("publication year").with { "No date" }
        .add(topicsDf)


    val pos by column<Vector2>()

    private val pointsDf = DataFrame.read("offline-data/all-data-v3-umap-2d.csv")
        .take(articlesDf.rowsCount())
        .merge { "x"<Double>() and "y"() }
        .by { Vector2(it[0], it[1]) }
        .into(pos)

    val dataFrame = pointsDf.add(articlesDf)

    val articles = articlesDf.toListOf<Article>()
    val points = pointsDf[pos].toList().run { map(this.bounds, frame) }

    val pointsToArticles = (points zip articles).toMap()
    val articlesToPoints = pointsToArticles.entries.associateBy({ it.value }) { it.key }

}

class State(val model: DataModel) {

    val changed = Event<Unit>()

    val kdtree = model.points.kdTree()

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

        return active.toMap()
    }

    var activePoints = findActivePoints(model.frame.center, radius)

    var filterSet = FilterSet.EMPTY
        set(value) {
            println(value.faculties)
            field = value
            val new = value
            if (new == FilterSet.EMPTY) {
                filtered = model.pointsToArticles
            } else {
                filtered = model.pointsToArticles.filter {
                    it.value.faculty in new.faculties &&
                            it.value.topic in new.topics &&
                            it.value.year.takeLast(4).toInt() in new.dates.first..new.dates.second
                }
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
    val year: String,

    @ColumnName("abstract")
    val abstract: String,

    @ColumnName("uuid")
    val uuid: String,

    @ColumnName("topic")
    val topic: String,

    @ColumnName("scores")
    val topicScores: List<BigDecimal>,

):Serializable

fun main() {
    val dmn = DataModel(Rectangle.fromCenter(Vector2.ZERO, 1920.0, 1080.0))
    dmn.dataFrame.toStandaloneHTML().openInBrowser()

    println(dmn.articles.random().topicScores)
}
