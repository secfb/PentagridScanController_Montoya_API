package ui

import javax.swing.RowFilter

class HideRowFilter: RowFilter<javax.swing.table.TableModel, Int>(){

    override fun include(entry: Entry<out javax.swing.table.TableModel, out Int>): Boolean {
        val model = entry.model as TableModel
        return !model.isHidden(entry.getStringValue(0).toInt())
    }

}