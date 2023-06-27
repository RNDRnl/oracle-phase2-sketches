package v05

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.launch
import org.openrndr.math.*
import org.openrndr.shape.Rectangle
import kotlin.concurrent.thread


fun main() = application {
    val appMode = when (val mode=System.getProperty("appMode")) {
        "production" -> AppMode.Production
        "prototype" -> AppMode.Prototype
        else -> AppMode.Debug
    }
    val scale = 3

    configure {
        when (appMode) {
            AppMode.Debug -> { // local testing
                width = (2560 * 4) / scale
                height = (1080 * 3) / scale
                position = IntVector2(-300, -1800)
            }

            AppMode.Prototype -> { // used for testing at RNDR
                width = 1920 * 3
                height = 1080 * 2
                position = IntVector2(-1920, -1080)
                hideWindowDecorations = true
                windowAlwaysOnTop = true
            }

            AppMode.Production -> { // used for production
                width = (2560 * 4)
                height = (1080 * 3)
                position = IntVector2(0, 0)
                hideWindowDecorations = true
                windowAlwaysOnTop = true
            }
        }
    }
    program {

        val data = DataModel(Rectangle(Vector2.ZERO, 1920.0, 1080.0))
        var frames = Rectangle(0.0, 0.0, 2560 * 4.0, 1080.0 * 3)
            .grid(4, 3)
            .flatten()
            .slice(setOf(0, 1, 4, 5, 6, 7, 8, 9))

        var screens = frames.mapIndexed { i, r ->
            val vb = viewBox(Rectangle(0.0, 0.0, 2560.0, 1080.0)) { screenProgram(i, r, data) }
            val update: (mode: Int, articles: MutableList<Article>, zoomLevel: Int) -> Unit by vb.userProperties
            vb to update
        }

        when(appMode) {
            AppMode.Debug -> {
            }
            AppMode.Prototype -> {
                frames = Rectangle(0.0, 0.0, 1920 * 3.0, 1080.0 * 2)
                    .grid(3, 2)
                    .flatten()
                    .slice(setOf(0, 1, 2, 4))

                screens = frames.mapIndexed { i, r ->
                    val vb = viewBox(Rectangle(0.0, 0.0, 1920.0, 1080.0)) { screenProgram(i, r, data) }
                    val update: (mode: Int, articles: MutableList<Article>, zoomLevel: Int) -> Unit by vb.userProperties
                    vb to update
                }
            }
            AppMode.Production -> {

            }
        }

        val receiver = Receiver()

        keyboard.character.listen {
            if (it.character == '1') {
                receiver.stateReceived.trigger(
                    EventObject(
                        NAVIGATE,
                        data.articles.shuffled().take(100).map { data.articles.indexOf(it) }, 0.1
                    )
                )
            }
            if (it.character == '2') {
                receiver.stateReceived.trigger(
                    EventObject(
                        NAVIGATE,
                        data.articles.shuffled().take(100).map { data.articles.indexOf(it) }, 0.34
                    )
                )
            }
            if (it.character == '3') {
                receiver.stateReceived.trigger(
                    EventObject(
                        NAVIGATE,
                        data.articles.shuffled().take(100).map { data.articles.indexOf(it) }, 1.0
                    )
                )
            }
            if (it.character == 'i') {
                receiver.stateReceived.trigger(
                    EventObject(
                        IDLE,
                        emptyList(), 0.0
                    )
                )

            }
        }

        receiver.stateReceived.listen { e ->
            val zoomLevel = if (e.articleIndexes.size == 1) 2 else when (e.zoom) {
                in 0.0..0.33 -> 0
                in 0.33..0.8 -> 1
                else -> 2
            }

            val newArticles = mutableListOf<Article>()

            for (i in e.articleIndexes) {
                newArticles.add(data.articles[i])
            }

            println("number of articles: ${newArticles.size}")

            launch {
                if (e.screenMode == NAVIGATE) {
                    if (zoomLevel < 2) {
                        val chunks = HashMap<Int, MutableList<Article>>(8)

                        var currentScreen = 0
                        for (article in newArticles) {
                            val c = chunks.getOrPut(currentScreen) { mutableListOf() }

                            if (c.size < 1000) {
                                c.add(article)
                            } else break

                            currentScreen = if (currentScreen == 7) 0 else currentScreen + 1
                        }

                        for (screen in screens) {
                            screen.second(e.screenMode, emptyList<Article>().toMutableList(), zoomLevel)
                        }

                        for (chunk in chunks) {
                            val updateFunc = screens[chunk.key].second
                            updateFunc(e.screenMode, chunk.value, zoomLevel)
                        }
                    } else {
                        for ((_, updateFunc) in screens) {
                            updateFunc(e.screenMode, newArticles.toMutableList(), zoomLevel)
                        }
                    }
                } else if (e.screenMode == IDLE) {
                    for ((_, updateFunc) in screens) {
                        updateFunc(e.screenMode, newArticles, 0)
                    }
                }


            }

        }


        thread(isDaemon = true) {
            while (true) {
                receiver.work()
            }
        }

        extend {

            when (appMode) {
                AppMode.Debug -> drawer.scale(1.0 / scale)
                else -> {}
            }

            (screens zip frames).forEach { (screen, rect) ->
                screen.first.update()
                drawer.image(screen.first.result, screen.first.result.bounds, rect)
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.rectangle(rect)
            }
        }
    }
}
