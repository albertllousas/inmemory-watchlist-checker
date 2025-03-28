package watchlist

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.InputStreamReader
import java.time.LocalDate

class CsvSanctionedPersonParser(
    private val resourcePath: String,
    private val sanctionedIdColumn: Int = 0,
    private val entryIdColumn: Int = 1,
    private val sourceColumn: Int = 2,
    private val typeColumn: Int = 3,
    private val fullNameColumn: Int = 4,
    private val aliasColumn: Int = 5,
    private val dobStartColumn: Int = 6,
    private val dobEndColumn: Int = 7,
    private val filterColumnAndValue: Pair<Int, String>? = null,
    private val skipHeaders: Boolean = true
) {

    fun parse(): Sequence<SanctionedPerson> {
        val resource = javaClass.getResourceAsStream(resourcePath)
            ?: error("CSV file not found: $resourcePath")

        val reader = InputStreamReader(resource)

        val parser = CSVParser.builder()
            .setFormat(CSVFormat.DEFAULT)
            .setReader(reader)
            .get()

        val records = parser.asSequence()
            .drop(if (skipHeaders) 1 else 0)
            .filter { record ->
                filterColumnAndValue == null || record.get(filterColumnAndValue.first)
                    .trim() == filterColumnAndValue.second
            }
            .groupBy { it.get(sanctionedIdColumn).trim() }

        return records.map { (sanctionedId, records) ->
            val entryId = records.first().get(entryIdColumn).trim()
            val source = records.first().get(sourceColumn).trim()
            val type = records.first().get(typeColumn).trim()
            val fullName = records.first().get(fullNameColumn).trim()
            val aliases = records.map { it.get(aliasColumn).trim() }.distinct()
            val dobRanges = records.mapNotNull {
                val dobStart = it.get(dobStartColumn).trim()
                val dobEnd = it.get(dobEndColumn).trim()
                if (dobStart.isNotEmpty() && dobEnd.isNotEmpty()) {
                    DoBRange(LocalDate.parse(dobStart), LocalDate.parse(dobEnd))
                } else {
                    null
                }
            }
            SanctionedPerson(
                sanctionedId = sanctionedId,
                entryId = entryId,
                source = SanctionedPersonSource.valueOf(source),
                type = SanctionedPersonType.valueOf(type),
                fullName = fullName,
                aliases = aliases,
                dobRanges = dobRanges
            )
        }.asSequence()
    }
}
