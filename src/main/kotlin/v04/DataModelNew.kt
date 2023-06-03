package v04

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.ColumnName
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.io.toStandaloneHTML
import org.openrndr.events.Event
import org.openrndr.extra.hashgrid.filter
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.contains
import org.openrndr.shape.map
import java.io.File
import java.net.URL

class DataModelNew(val frame: Rectangle) {


    val changed = Event<Unit>()

    private val articlesDf = DataFrame.read("offline-data/all-data-v3.csv")
        .select{ cols(0..7) }
        .fillNulls("faculty").with { "Unknown Faculty" }
        .convert("faculty").with { Faculty.fromName(it as String) }
        .fillNulls("abstract").with { "No abstract provided" }
        .fillNulls("author").with { "Unknown Author" }
        .fillNulls("contributor","department").with { "" }
        .fillNulls("publication year").with { "No date" }
        .add("topic") { "no topic" }

    val pos by column<Vector2>()

    private val pointsDf = DataFrame.read("offline-data/all-data-v3-umap-2d.csv")
        .take(articlesDf.rowsCount())
        .merge { "x"<Double>() and "y"() }
        .by { Vector2(it[0], it[1]) }
        .into(pos)

    val dataFrame = pointsDf.add(articlesDf)


    val articles = articlesDf.toListOf<Article>()
    val points = pointsDf[pos].toList().run { map(this.bounds, frame) }

    //// fake topics logic

    val fakeTriangulation = points.map(frame, frame.offsetEdges(150.0))
        .filter(210.0)
        .delaunayTriangulation()
        .triangles()
        .mapIndexed { i, it -> it.contour to topics.random() }

    fun  Map<Vector2, Article>.addFakeTopics(): Map<Vector2, Article> {
        return this.onEach {
            val topic = fakeTriangulation.firstOrNull { (c, s) -> c.contains(it.key) }
            it.value.topic = topic?.second ?: "no topic"
        }
    }
    /// end fake topics logic

    val pointsToArticles = (points zip articles).toMap().addFakeTopics()
    val articlesToPoints = pointsToArticles.entries.associateBy({ it.value }) { it.key }


    val kdtree = points.kdTree()

    var radius = 40.0
    var lookAt = frame.center
        set(value) {
            activePoints = findActivePoints(value, radius)
        }

    var filtered = pointsToArticles
        set(value) {
            activePoints = findActivePoints(frame.center, radius)
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

    var activePoints = findActivePoints(frame.center, radius)
        set(value) {
            field = value
            changed.trigger(Unit)
        }
}


val faculties by lazy { facultyNames.map { Faculty.fromName(it) } }

val topics by lazy {
    File("offline-data/labels.txt").readText()
        .split(", ").map { it.drop(1).dropLast(1) }
}

val mappings = (facultyNames zip topics.chunked(topics.size / faculties.size)).toMap()

data class Article(
    @ColumnName("title")
    val title: String,

    @ColumnName("author")
    val author: String,

    @ColumnName("contributor")
    val contributor: String,

    @ColumnName("faculty")
    val faculty: Faculty,

    @ColumnName("department")
    val department: String,

    @ColumnName("publication year")
    val year: String,

    @ColumnName("abstract")
    val abstract: String,

    @ColumnName("uuid")
    val uuid: String,


    var topic: String,
)

fun main() {
    val dmn = DataModelNew(Rectangle.EMPTY)
    dmn.dataFrame.toStandaloneHTML().openInBrowser()
}
