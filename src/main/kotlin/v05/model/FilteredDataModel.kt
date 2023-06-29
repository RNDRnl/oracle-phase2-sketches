package v05.model

import org.openrndr.events.Event
import v05.Article
import v05.filters.FilterSet

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
        realArticles = if (filter == FilterSet.EMPTY) {
            allArticles
        } else {
            allArticles.filter {
                it.faculty in filter.faculties &&
                        it.topic in filter.topics &&
                        it.year.takeLast(4).toInt() in filter.dates.first..filter.dates.second
            }
        }
        realArticles.groupBy { it.faculty }
    }

    private var realArticles = allArticles

    val articles: List<Article>
        get() {
            return realArticles
        }

    var groupedByFaculty: Map<String, List<Article>> = realArticles.groupBy { it.faculty }
        private set

    var groupedByTopic: Map<String, List<Article>> = realArticles.groupBy { it.topic }

}