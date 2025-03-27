package benchmark

import watchlist.CsvSanctionedPersonParser
import watchlist.SanctionedCandidateFetcher
import watchlist.SanctionedPersonChecker

object SanctionedPersonIndexMemoryBenchmark {

    @JvmStatic
    fun main(args: Array<String>) {
        val parser = CsvSanctionedPersonParser(
            resourcePath = "/internal_list.csv",
        )
        println("🔍 Running custom benchmark for index creation...\n")
        measureUsedMemoryEagerly(parser)
        measureUsedMemory(parser)
        measureRetainedMemory(parser)
    }

    private fun measureUsedMemory(parser: CsvSanctionedPersonParser) {
        println("📦 Measuring USED memory before and after index creation lazily")
        System.gc()
        Thread.sleep(100)

        val before = usedMemoryMB()
        val start = System.nanoTime()

        val candidateFetcher = SanctionedCandidateFetcher(parser.parse())
        SanctionedPersonChecker(candidateFetcher)

        val end = System.nanoTime()
        val after = usedMemoryMB()

        val timeMs = (end - start) / 1_000_000
        val usedMB = after - before

        println("⏱️  Time taken: ${timeMs} ms")
        println("📊 Used memory: ${usedMB} MB\n")
    }

    private fun measureUsedMemoryEagerly(parser: CsvSanctionedPersonParser) {
        println("📦 Measuring USED memory before and after index creation eagerly")
        System.gc()
        Thread.sleep(100)

        val before = usedMemoryMB()
        val start = System.nanoTime()

        val candidateFetcher = SanctionedCandidateFetcher(parser.parse().toList().asSequence())
        SanctionedPersonChecker(candidateFetcher)

        val end = System.nanoTime()
        val after = usedMemoryMB()

        val timeMs = (end - start) / 1_000_000
        val usedMB = after - before

        println("⏱️  Time taken: ${timeMs} ms")
        println("📊 Used memory: ${usedMB} MB\n")
    }

    private fun measureRetainedMemory(parser: CsvSanctionedPersonParser) {
        println("🧠 Measuring RETAINED memory after GC\n")

        System.gc()
        Thread.sleep(100)

        val before = usedMemoryMB()

        val candidateFetcher = SanctionedCandidateFetcher(parser.parse())
        SanctionedPersonChecker(candidateFetcher)

        System.gc()
        Thread.sleep(100)

        val after = usedMemoryMB()
        val retainedMB = after - before

        println("📊 Retained memory: ${retainedMB} MB\n")
    }

    private fun usedMemoryMB(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    }
}
