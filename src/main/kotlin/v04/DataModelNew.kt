package v04

import classes.*
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.ColumnName
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.io.toStandaloneHTML
import org.openrndr.events.Event
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.net.URL

class DataModelNew(frame: Rectangle) {

    val changed = Event<Unit>()

    private val articlesDf = DataFrame.read("offline-data/islandora_doctoral_thesis_full.csv")
        .select{ cols(0..7) }
        .fillNulls("Faculty").with { "Unknown Faculty" }
        .convert("Faculty").with { Faculty.fromName(it as String) }
        .fillNulls("Abstract").with { "No abstract provided" }
        .fillNulls("Author").with { "Unknown Author" }
        .fillNulls("Contributor","Department").with { "" }
        .fillNulls("Date").with { "No date" }
        .update("Date").with { it.toString().takeLast(4) }
        .rename { colsOf<URL>() }.into { "Link" }
        .move("Link").toRight()

    val pos by column<Vector2>()

    private val pointsDf = DataFrame.read("offline-data/data-umap-highlight-v1.csv")
        .take(articlesDf.rowsCount())
        .merge { "x"<Double>() and "y"() }
        .by { Vector2(it[0], it[1]) }
        .into(pos)

    val dataFrame = pointsDf.add(articlesDf)


    val articles = articlesDf.toListOf<Article>()
    val points = pointsDf[pos].toList().run { map(this.bounds, frame) }

    val pointsToArticles = (points zip articles).toMap()


    val kdtree = points.kdTree()

    var radius = 40.0
    var lookAt = frame.center
        set(value) {
            activePoints = findActivePoints(value, radius)
        }

    var filtered = pointsToArticles

    fun findActivePoints(pos: Vector2, radius: Double): List<Vector2> {
        return kdtree.findAllInRadius(pos, radius).sortedBy { it.distanceTo(pos) }
    }

    var activePoints = findActivePoints(frame.center, radius)
        set(value) {
            field = value
            changed.trigger(Unit)
        }
}

data class Article(
    @ColumnName("Title")
    val title: String,

    @ColumnName("Author")
    val author: String,

    @ColumnName("Contributor")
    val contributor: String,

    @ColumnName("Faculty")
    val faculty: Faculty,

    @ColumnName("Department")
    val department: String,

    @ColumnName("Date")
    val date: String,

    @ColumnName("Abstract")
    val abstract: String,

    @ColumnName("Link")
    val link: URL,
)

fun main() {
    val dmn = DataModelNew(Rectangle.EMPTY)
    dmn.dataFrame.toStandaloneHTML().openInBrowser()
}
