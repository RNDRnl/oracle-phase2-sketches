package v04

import classes.ArticleData
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2

class ArticleEntity(val ad: ArticleData, val color: ColorRGBa)

fun Vector2.transform(m : Matrix44) : Vector2 {
    return (m * this.xy01).xy
}
