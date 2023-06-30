package v05.filters

import v05.facultyNames
import v05.topicNames
import java.io.Serializable

data class FilterSet(val faculties: List<String>, val topics: List<String>, val dates: Pair<Int, Int>): Serializable {
    companion object {
        val EMPTY = FilterSet(facultyNames, topicNames, 1900 to 2023)
    }
}