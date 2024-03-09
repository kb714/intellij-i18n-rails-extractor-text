import javax.swing.table.AbstractTableModel

data class TranslationKey(val key: String, val offset: Int, val esValue: String = "undefined")
class TranslationKeyTableModel : AbstractTableModel() {
    private val columnNames = arrayOf("Key", "Value")
    private var data: List<TranslationKey> = emptyList()

    fun setData(newData: List<TranslationKey>) {
        data = newData
        fireTableDataChanged()
    }

    override fun getColumnCount(): Int = columnNames.size

    override fun getRowCount(): Int = data.size

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return when (columnIndex) {
            0 -> data[rowIndex].key
            1 -> data[rowIndex].esValue
            else -> {}
        }
    }

    override fun getColumnName(column: Int): String = columnNames[column]

    fun getOffsetAt(rowIndex: Int): Int = data[rowIndex].offset
}


