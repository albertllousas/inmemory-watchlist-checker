package watchlist

import java.time.LocalDate

data class SanctionedPerson(
    val sanctionedId: String,
    val entryId: String,
    val source: SanctionedPersonSource,
    val type: SanctionedPersonType,
    val fullName: String,
    val aliases: List<String>,
    val dobRanges: List<DoBRange>
)

data class DoBRange(
    val start: LocalDate,
    val end: LocalDate,
)

enum class SanctionedPersonType {
    INDIVIDUAL,
    ENTITY
}

enum class SanctionedPersonSource {
    SANCTION,
    WAB,
    PEP,
    PIL
}
