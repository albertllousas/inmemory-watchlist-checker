package watchlist

import java.time.LocalDate

class SanctionedPersonChecker(
    private val sanctionedCandidateFetcher: SanctionedCandidateFetcher,
    private val scorer: SimilarityScorer = SimilarityScorer(),
    private val scoreThreshold: Float = 0.9f,
) {
    fun check(
        fullName: String,
        dob: LocalDate? = null,
        type: SanctionedPersonType? = null,
        source: SanctionedPersonSource? = null,
    ): List<SanctionedPersonCheck> {
        val topCandidates = sanctionedCandidateFetcher.fetchTopCandidates(fullName, dob, type, source, 20)
        val scoredResults =
            topCandidates.map {
                SanctionedPersonCheck(
                    sanctionedId = it.sanctionedId,
                    entryId = it.entryId,
                    source = it.source,
                    type = it.type,
                    fullName = it.fullName,
                    aliases = it.aliases,
                    dobRanges = it.dobRanges,
                    score = scorer.score(fullName, it.fullName, it.aliases),
                )
            }
        return scoredResults.filter { it.score >= scoreThreshold }
    }
}

data class SanctionedPersonCheck(
    val sanctionedId: String,
    val entryId: String,
    val source: SanctionedPersonSource,
    val type: SanctionedPersonType,
    val fullName: String,
    val aliases: List<String>,
    val dobRanges: List<DoBRange>,
    val score: Float,
)
