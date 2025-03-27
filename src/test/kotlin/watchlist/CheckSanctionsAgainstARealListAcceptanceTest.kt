package watchlist

import io.kotest.matchers.collections.shouldContainAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CheckSanctionsAgainstARealListAcceptanceTest {

    @Test
    fun `should check a potential full name match against the Consolidated Screening List of US Dept of Commerce`() {
        val csvParser = CsvSanctionedPersonParser(
            resourcePath = "/internal_list.csv",
        )
        val sanctionedCandidateFetcher = SanctionedCandidateFetcher(csvParser.parse())
        val sanctionedPersonChecker = SanctionedPersonChecker(sanctionedCandidateFetcher)

        val result = sanctionedPersonChecker.check(
            fullName = "Marwan Mohammed ABU RAS",
            dob = LocalDate.parse("1958-07-01")
        )

        result shouldContainAll listOf(
            SanctionedPersonCheck(
                sanctionedId = "1a3a4f2e-2ad4-4961-9d3b-7c20dfe44f11",
                entryId = "20201418449",
                source = SanctionedPersonSource.WAB,
                type = SanctionedPersonType.INDIVIDUAL,
                fullName = "Marwan Mohammed ABU RAS",
                aliases = listOf("Merwan Muhammed ABOU RAAS"),
                dobRanges = listOf(
                    DoBRange(LocalDate.parse("1958-01-01"), LocalDate.parse("1958-12-31"))
                ),
                score = 1.0F
            ),
            SanctionedPersonCheck(
                sanctionedId = "769361d2-9278-45e6-bbfb-cf62e5fe5379",
                entryId = "10291009672",
                source = SanctionedPersonSource.SANCTION,
                type = SanctionedPersonType.INDIVIDUAL,
                fullName = "Marwan Mohammed ABU RAS",
                aliases = listOf("ABOU RAAS, Merwan Muhammed"),
                dobRanges = listOf(
                    DoBRange(LocalDate.parse("1958-01-01"), LocalDate.parse("1958-12-31"))
                ),
                score = 1.0F
            )
        )
    }
}
