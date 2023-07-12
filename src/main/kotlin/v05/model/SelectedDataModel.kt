package v05.model

import org.openrndr.events.Event
import v05.Article
import v05.filters.FilterSet
import v05.libs.brackets
import v05.libs.histogramOf

class SelectedDataModel() {
    val dataChanged = Event<Unit>("data-changed")

    var selectedArticles: List<Article> = listOf()
        set(value) {
            field = value
            filteredArticles = value
        }

    var filter: FilterSet = FilterSet.EMPTY
        set(value) {
            println("new filter")
            field = value
            filterData()
            dataChanged.trigger(Unit)
        }

    private fun filterData() {
        filteredArticles = if (filter == FilterSet.EMPTY) {
            selectedArticles
        } else {
            val noTopics = filter.topics.joinToString("").isBlank()
            val noFaculties = filter.faculties.joinToString("").isBlank()

            selectedArticles.filter {
                (noFaculties || (it.faculty in filter.faculties)) &&
                        (noTopics || (it.topic in filter.topics)) &&
                        it.year.toString().takeLast(4).toInt() in filter.dates.first..filter.dates.second
            }
        }

        println("filtering data using ${filter}, ${filteredArticles.size}")

        groupedByFaculty = filteredArticles.groupBy { it.faculty }
        groupedByTopic = filteredArticles.groupBy { it.topic }
        groupedByYear = filteredArticles.groupBy { it.year }
        yearBrackets = filteredArticles.histogramOf { it.year }.brackets()
    }


    var filteredArticles = selectedArticles
        private set

    val articles: List<Article>
        get() {
            return filteredArticles
        }

    var groupedByFaculty: Map<String, List<Article>> = filteredArticles.groupBy { it.faculty }
        private set

    var groupedByTopic: Map<String, List<Article>> = filteredArticles.groupBy { it.topic }
        private set

    var groupedByYear: Map<Int, List<Article>> = filteredArticles.groupBy { it.year }
        private set

    var yearBrackets = articles.histogramOf { it.year }.brackets()
        private set


}