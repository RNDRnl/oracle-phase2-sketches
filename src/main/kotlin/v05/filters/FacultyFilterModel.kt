package v05.filters

import v05.facultyNames

class FacultyFilterModel: FilterModel() {

    override val list = facultyNames
    override val states = list.map { ToggleFilterState() }
    val filteredList: List<String>
        get() = filter()

    fun filter(): List<String> {
        return list.filterIndexed { i, _ -> states[i].visible  }
    }

    init {
        states.forEach {
            it.stateChanged.listen {
                if(states.none { it.visible }) states.forEach { it.visible = true }
                filterChanged.trigger(Unit)
            }
        }
    }

}