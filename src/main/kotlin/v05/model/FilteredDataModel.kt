package v05.model

import org.openrndr.events.Event
import v05.Article
import v05.filters.FilterSet
import v05.libs.brackets
import v05.libs.histogramOf

class FilteredDataModel(val allArticles: List<Article>) {
    val dataChanged = Event<Unit>("data-changed")

    var filter: FilterSet = FilterSet.EMPTY
        set(value) {
            if (field != value) {
                field = value
                filterData()
                dataChanged.trigger(Unit)
            }
        }

    private fun filterData() {
        filteredArticles = if (filter == FilterSet.EMPTY) {
            allArticles
        } else {
            println(filter.topics.isEmpty())
            val noTopics = filter.topics.joinToString("").isBlank()
            val noFaculties =filter.faculties.joinToString("").isBlank()

            allArticles.filter {
                (noFaculties || (it.faculty in filter.faculties)) &&
                        (noTopics || (it.topic in filter.topics)) &&
                        it.year.takeLast(4).toInt() in filter.dates.first..filter.dates.second
            }
        }

        println("filtering data using ${filter}, ${filteredArticles.size}")

        groupedByFaculty = filteredArticles.groupBy { it.faculty }
        groupedByTopic = filteredArticles.groupBy { it.topic }
        yearBrackets = filteredArticles.histogramOf { it.year.toIntOrNull() }.brackets()

    }

    var filteredArticles = allArticles
        private set

    val articles: List<Article>
        get() {
            return filteredArticles
        }

    var groupedByFaculty: Map<String, List<Article>> = filteredArticles.groupBy { it.faculty }
        private set

    var groupedByTopic: Map<String, List<Article>> = filteredArticles.groupBy { it.topic }
        private set

    var yearBrackets = articles.histogramOf { it.year.toIntOrNull() }.brackets()
        private set
}