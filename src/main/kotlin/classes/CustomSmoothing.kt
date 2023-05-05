package classes

import org.openrndr.Clock
import kotlin.math.pow
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

fun Clock.smoothing(property: KProperty0<Array<Double>>, factor: Double = 0.99): DoubleArrayPropertySmoother {
    return DoubleArrayPropertySmoother(this, property, factor, null)
}

class DoubleArrayPropertySmoother(
    private val clock: Clock,
    private val property: KProperty0<Array<Double>>,
    private val factor: Double = 0.99,
    private val factorProperty: KProperty0<Double>?
) {
    private var output: Array<Double>? = null
    private var lastTime: Double? = null
    operator fun getValue(any: Any?, property: KProperty<*>): Array<Double> {

        if (lastTime != null) {
            val dt = clock.seconds - lastTime!!
            if (dt > 1E-10) {
                val steps = dt * 60.0
                val ef = (factorProperty?.get() ?: factor).pow(steps)
                for(i in output!!.indices) {
                    output!![i] = output!![i] * ef + this.property.get()[i] * (1.0 - ef)
                }

            }
        } else {
            output = this.property.get()
        }
        lastTime = clock.seconds
        return output ?: error("no value")
    }
}

fun Clock.smoothStaggered(property: KProperty0<Array<Double>>, stagger: Double = 0.0, factor: Double = 0.99): DoubleArrayStaggeredPropertySmoother {
    return DoubleArrayStaggeredPropertySmoother(this, property, factor, null, stagger)
}

class DoubleArrayStaggeredPropertySmoother(
    private val clock: Clock,
    private val property: KProperty0<Array<Double>>,
    private val factor: Double = 0.99,
    private val factorProperty: KProperty0<Double>?,
    private val stagger: Double = 0.0
) {
    private var output: Array<Double>? = null
    private var lastTime: Double? = null
    operator fun getValue(any: Any?, property: KProperty<*>): Array<Double> {

        if (lastTime != null) {
            val dt = clock.seconds - lastTime!!
            if (dt > 1E-10) {
                val steps = dt * 60.0
                val ef = (factorProperty?.get() ?: factor).pow(steps)

                for(i in output!!.indices) {
                    val staggerF = (1.0 / (output!!.size)) * i
                    output!![i] = output!![i] * (ef * staggerF) + this.property.get()[i] * (1.0 - (ef * staggerF))
                }
            }
        } else {
            output = this.property.get()
        }
        lastTime = clock.seconds
        return output ?: error("no value")
    }
}

