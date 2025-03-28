package benchmark

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import watchlist.CsvSanctionedPersonParser
import watchlist.SanctionedPerson
import watchlist.SanctionedPersonChecker
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Level.Trial
import org.openjdk.jmh.annotations.Mode
import watchlist.SanctionedCandidateFetcher
import watchlist.SanctionedPersonSource
import watchlist.SanctionedPersonType
import java.time.LocalDate

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
open class SanctionedPersonUsageBenchmark {

    private lateinit var checker: SanctionedPersonChecker

    private lateinit var parser: CsvSanctionedPersonParser

    private lateinit var candidateFetcher: SanctionedCandidateFetcher

    @Setup(Trial)
    fun setup() {
        val parser = CsvSanctionedPersonParser(
            resourcePath = "/internal_list.csv",
        )
        candidateFetcher = SanctionedCandidateFetcher(parser.parse())
        checker = SanctionedPersonChecker(candidateFetcher)
    }

    @Benchmark
    fun checkPerson(): List<*> {
        return checker.check(fullName = "John Doe", dob = LocalDate.parse("1979-07-01"))
    }

    @Benchmark
    fun addPerson() {
        candidateFetcher.addSanctionedPerson(
            SanctionedPerson(
                sanctionedId = "0ab6d9ea-34b9-4be7-80ed-f29422ecbb32",
                entryId = "10101010101",
                source = SanctionedPersonSource.SANCTION,
                type = SanctionedPersonType.INDIVIDUAL,
                fullName = "Jane Smith",
                aliases = listOf("J. Smith"),
                dobRanges = listOf(
                    watchlist.DoBRange(
                        start = LocalDate.parse("1979-01-01"),
                        end = LocalDate.parse("1979-12-31")
                    )
                )
            )
        )
    }

    @Benchmark
    fun createLuceneIndex(){
        SanctionedCandidateFetcher(parser.parse())
    }
}
