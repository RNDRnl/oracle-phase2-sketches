package v05.filters


class DateFilterModel: FilterModel() {

    override val list = listOf(1900, 2020)
    override val states = list.map { DateFilterState(it) }
    val filteredList: List<Int>
        get() = filter()

    fun filter(): List<Int> {
        val low = minOf(states[0].year, states[1].year).toInt()
        val high = maxOf(states[0].year, states[1].year).toInt()
        return listOf(low, high)
    }

    init {
        states.forEachIndexed { i, it ->
            it.stateChanged.listen {
                filterChanged.trigger(Unit)
            }
        }
    }
}