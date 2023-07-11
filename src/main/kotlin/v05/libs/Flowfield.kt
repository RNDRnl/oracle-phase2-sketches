package v05.libs

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter1to1
import org.openrndr.draw.Filter2to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.hashgrid.HashGrid
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.simplify
import kotlin.random.Random

class FlowfieldQuantizeDirection : Filter1to1(
    filterShaderFromCode(
        """
uniform sampler2D tex0;
in vec2 v_texCoord0;
uniform float phaseShift;
uniform int angles;
out vec4 o_output;
void main() {
    vec4 texData = texture(tex0, v_texCoord0);
    vec2 direction = texData.xy;
    float phaseShiftRadian = phaseShift / 180.0 * 3.1415926536;
    float magnitude = length(direction);
    float newAngle = 0.0;
    
    if (magnitude > 0.0) {
        float angle = atan(direction.y, direction.x) - phaseShiftRadian;
        if (angle < 0.0) { angle += 2.0 * 3.1415926536; }
        float steps = floor( (angle / (2.0 * 3.1415926535)) * angles); 
        newAngle = (steps / angles) * 2.0 * 3.1415926536 + phaseShiftRadian;
    } 
    o_output = vec4(cos(newAngle) * magnitude, sin(newAngle) * magnitude, texData.b, texData.a);
}
    
""".trimIndent(), "flowfield-quantize-direction"
    )
) {

    @IntParameter("angles", 1, 32)
    var angles by parameters

    @DoubleParameter("phase shift", -180.0, 180.0)
    var phaseShift by parameters

    init {
        angles = 4
        phaseShift = 0.0
    }
}


class FlowfieldPerpendicularDirection : Filter1to1(
    filterShaderFromCode(
        """
uniform sampler2D tex0;
in vec2 v_texCoord0;
out vec4 o_output;
void main() {
    vec4 texData = texture(tex0, v_texCoord0);
    vec2 direction = texData.xy;
    o_output = vec4(direction.y, -direction.x, texData.b, texData.a);
}
    
""".trimIndent(), "flowfield-perpendicular-direction"
    )
) {
}

class FlowfieldModulateDirection : Filter2to1(
    filterShaderFromCode(
        """
uniform sampler2D tex0;
uniform sampler2D tex1;
in vec2 v_texCoord0;
uniform float phaseShift;
uniform int angles;
out vec4 o_output;
void main() {
    vec4 texData = texture(tex0, v_texCoord0);
    vec2 direction = texData.xy;
    vec2 modDirection = texture(tex1, v_texCoord0).xy;
    vec2 modPerp = vec2(modDirection.y, -modDirection.x);
    mat2 rot = mat2(modDirection, modPerp);
    vec2 newDirection = rot * direction;
    o_output = vec4(newDirection, texData.b, texData.a);
}
    
""".trimIndent(), "flowfield-modulate-direction"
    )
)

class FlowfieldGridVisualizer : Filter1to1(
    filterShaderFromCode(
        """
uniform sampler2D tex0;
uniform vec4 stroke;
in vec2 v_texCoord0;
out vec4 o_output;
uniform float cells;


float dist2Line(vec2 a, vec2 b, vec2 p) { 
    p -= a, b -= a;
	float h = clamp(dot(p, b) / dot(b, b), 0., 1.); 
	return length( p - b * h );                       
}

float cell(vec2 uv){
    float AA = 15.0 / textureSize(tex0, 0).x;
    float LINE_W = 0.1;
	return smoothstep(LINE_W + AA, LINE_W, dist2Line(vec2(-.4, 0), vec2(.4, 0), uv));
}
             
void main() {
    vec2 ts = vec2(textureSize(tex0, 0));
    vec2 position = (v_texCoord0 - vec2(0.5)) * ts;

    vec2 s = vec2(cells) * vec2(ts.x/ts.y, 1.0); 
    vec2 direction = texture(tex0, floor(v_texCoord0*s)/s).xy;
    
    if (length(direction) > 0.0) { 
        direction = normalize(direction);
        vec2 u = v_texCoord0 * vec2(ts.x/ts.y, 1.0);
   
        vec2 cellPosition = fract(u * cells) - vec2(0.5);
        cellPosition = mat2(direction, vec2(direction.y, -direction.x)) * cellPosition;
        float o = cell(cellPosition);
        o_output = stroke * o;
    } else {
        o_output = stroke * 0.0;
    }
}
    
    
""".trimIndent(), "flowfield-grid-visualizer"
    )
) {

    @DoubleParameter("cells", 1.0, 256.0)
    var cells: Double by parameters

    @ColorParameter("stroke")
    var stroke: ColorRGBa by parameters

    init {
        cells = 128.0
        stroke = ColorRGBa.WHITE
    }
}


fun sampleFlowline(flowfield: ColorBuffer, start: Vector2, maxIterations: Int = 10_000, reverse:Boolean = false): List<Vector2> {

    val s = flowfield.shadow

    var cur = start * flowfield.contentScale
    var iteration = 0
    val line = mutableListOf(cur / flowfield.contentScale)
    while (true) {

        var samplePos = cur.toInt()

        if (samplePos.x < 0 || samplePos.y < 0 || samplePos.x > flowfield.effectiveWidth-1 || samplePos.y > flowfield.effectiveHeight-1) {
            break
        }

        val directionColor = s[samplePos.x, samplePos.y]
        val direction = Vector2(directionColor.r, directionColor.g*-1.0)
        if (direction.length < 1E-6) {
            break
        }

        if (!reverse) {
            cur += direction.normalized * flowfield.contentScale
        } else {
            cur -= direction.normalized * flowfield.contentScale
        }
        line.add(cur/flowfield.contentScale )

        iteration++
        if (iteration >= maxIterations) {
            break
        }

    }
    return line
}

fun chopFlowline(points: List<Vector2>, grid: HashGrid): List<List<Vector2>> {

    var activeLine = mutableListOf<Vector2>()
    var result = mutableListOf<List<Vector2>>()

    for (point in points) {
        if (grid.isFree(point)) {
            activeLine.add(point)
        } else {
            if (activeLine.isNotEmpty()) {
                for (point in activeLine) {
                    grid.insert(point)
                }
                result.add(activeLine)
                activeLine = mutableListOf()
            }
        }
    }
    if (activeLine.isNotEmpty()) {
        result.add(activeLine)
        for (point in activeLine) {
            grid.insert(point)
        }

    }

    return result
}

fun equidistantFlowlines(flowfield: ColorBuffer, origin: Vector2, distance0: Double = 5.0, distance1: Double = 10.0, maxIterations: Int = 100, maxCandidates:Int = 10, random: Random = Random.Default) : List<ShapeContour> {
    val flowlines = mutableListOf<ShapeContour>()
    val r1 = distance0
    val r2 = distance1
    val g = HashGrid(r1)
    val candidates = ArrayDeque<Vector2>()
    candidates.add(origin)
    while (candidates.isNotEmpty()) {
        val o = candidates.removeFirst()

        if (!g.isFree(o)) {
            continue
        }

        val f = sampleFlowline(flowfield, o, maxIterations)
        val r = sampleFlowline(flowfield, o, maxIterations, reverse = true).drop(1)
            .reversed()
        val bi = r + f

        val chops = chopFlowline(bi, g)

        for (it in chops) {
            if (it.any { it.x != it.x })
                continue

            val sit = simplify(it, 0.25)

            val s = ShapeContour.fromPoints(sit, closed = false)
            flowlines.add(s)

            val c0 = s.position(0.0) - s.normal(0.0).perpendicular() * r2
            if (g.isFree(c0)) {
                candidates.add(c0)
            }

            val c1 = s.position(1.0) + s.normal(1.0).perpendicular() * r2
            if (g.isFree(c1)) {
                candidates.add(c1)
            }

            var added = 0
            for (i in 0 until (s.length/2.0).toInt()) {
                val t = Double.uniform(0.0, 1.0, random)
                val z = if (Double.uniform(0.0, 1.0, random) < 0.5) - 1.0 else 1.0
                val b = s.position(t) + s.normal(t) * r2 * z
                if (g.isFree(b)) {
                    candidates.add(b)
                    added++
                }
                if (added >= maxCandidates) {
                    break
                }
            }
        }
    }
    return flowlines
}



