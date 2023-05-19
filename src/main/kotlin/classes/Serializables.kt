package classes

import org.openrndr.MouseEventType
import java.io.Serializable

class Color(val r: Double, val g: Double, val b: Double): Serializable
class EventObject(val type: MouseEventType, val indexesToColors: List<Pair<Int, Color>>, val zoom: Double): Serializable