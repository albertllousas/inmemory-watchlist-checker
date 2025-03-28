package watchlist

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanClause.Occur.MUST
import org.apache.lucene.search.BooleanClause.Occur.SHOULD
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BoostQuery
import org.apache.lucene.search.FuzzyQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.PhraseQuery
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TermRangeQuery
import org.apache.lucene.store.MMapDirectory
import java.nio.file.Files
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SanctionedCandidateFetcher(
    sanctionedPersons: Sequence<SanctionedPerson>,
) {
    private val index = MMapDirectory(Files.createTempDirectory("lucene-index"))

//    private val index = ByteBuffersDirectory()

    private val writer = IndexWriter(index, IndexWriterConfig(StandardAnalyzer()))

    private var reader: DirectoryReader

    private var searcher: IndexSearcher

    init {
        sanctionedPersons.forEach { writer.addDocument(it.toLuceneDocument()) }
        writer.commit()
        reader = DirectoryReader.open(index)
        searcher = IndexSearcher(reader)
    }

    private fun SanctionedPerson.toLuceneDocument() =
        Document().apply {
            add(TextField("fullName", fullName, Field.Store.YES))
            add(StringField("sanctionedId", sanctionedId, Field.Store.YES))
            add(StringField("entryId", entryId, Field.Store.YES))
            add(StringField("source", source.name, Field.Store.YES))
            add(StringField("type", type.name, Field.Store.YES))
            aliases.forEach { alias -> add(TextField("alias", alias, Field.Store.YES)) }
            dobRanges.forEach { dobRange ->
                val start = dobRange.start.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val end = dobRange.end.format(DateTimeFormatter.ISO_LOCAL_DATE)
                add(StringField("dobStart", start, Field.Store.YES))
                add(StringField("dobEnd", end, Field.Store.YES))
                add(StringField("dobRanges", "$start to $end", Field.Store.YES)) // for retrieval
            }
        }

    fun fetchTopCandidates(
        name: String,
        dob: LocalDate?,
        type: SanctionedPersonType? = null,
        source: SanctionedPersonSource? = null,
        topN: Int,
    ): List<Candidate> {
        val tokens = normalize(name).split(" ").filter { it.isNotBlank() }
        val queryBuilder = BooleanQuery.Builder()

        // boost for fullName exact phrase match
        queryBuilder.add(BoostQuery(PhraseQuery("fullName", *tokens.toTypedArray()), 10.0f), SHOULD)

        // Strong fuzzy match on fullName as a single field
        queryBuilder.add(BoostQuery(FuzzyQuery(Term("fullName", name.lowercase()), 2), 6.0f), SHOULD)

        // Token set match (unordered fullName tokens)
        val tokenSetQuery =
            BooleanQuery
                .Builder()
                .apply {
                    for (token in tokens) add(TermQuery(Term("fullName", token)), SHOULD)
                }.build()
        queryBuilder.add(BoostQuery(tokenSetQuery, 5.0f), SHOULD)

        // Token-based exact and fuzzy matching
        for (token in tokens) {
            queryBuilder.add(BoostQuery(TermQuery(Term("fullName", token)), 3.0f), SHOULD)
            queryBuilder.add(BoostQuery(FuzzyQuery(Term("fullName", token), 1), 2.0f), SHOULD)
            queryBuilder.add(BoostQuery(TermQuery(Term("alias", token)), 1.5f), SHOULD)
            queryBuilder.add(FuzzyQuery(Term("alias", token), 1), SHOULD)
        }

        // Date of Birth filtering
        if (dob != null) {
            val dobString = dob.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val dateRangeQuery =
                BooleanQuery
                    .Builder()
                    .add(TermRangeQuery.newStringRange("dobStart", null, dobString, true, true), MUST)
                    .add(TermRangeQuery.newStringRange("dobEnd", dobString, null, true, true), MUST)
                    .build()
            queryBuilder.add(dateRangeQuery, BooleanClause.Occur.MUST)
        }

        // Type filtering
        if (type != null) {
            queryBuilder.add(TermQuery(Term("type", type.name)), BooleanClause.Occur.MUST)
        }

        // Source filtering
        if (source != null) {
            queryBuilder.add(TermQuery(Term("source", source.name)), BooleanClause.Occur.MUST)
        }

        val query = queryBuilder.build()
        val topHits = searcher.search(query, topN).scoreDocs
        val candidates =
            topHits.map { hit ->
                val doc = searcher.storedFields().document(hit.doc)
                Candidate(
                    sanctionedId = doc.get("sanctionedId"),
                    entryId = doc.get("entryId"),
                    source = SanctionedPersonSource.valueOf(doc.get("source")),
                    type = SanctionedPersonType.valueOf(doc.get("type")),
                    fullName = doc.get("fullName"),
                    aliases = doc.getValues("alias").toList(),
                    dobRanges = doc.getValues("dobRanges").toList().map { parseDoBRange(it) },
                    score = hit.score,
                )
            }
        return candidates
    }

    private fun parseDoBRange(range: String): DoBRange {
        val parts = range.split(" to ")
        return DoBRange(
            LocalDate.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE),
            LocalDate.parse(parts[1], DateTimeFormatter.ISO_LOCAL_DATE),
        )
    }

    private fun normalize(name: String): String = name.lowercase().replace(Regex("\\s+"), " ").trim()

    fun addSanctionedPerson(person: SanctionedPerson) {
        writer.addDocument(person.toLuceneDocument())
        writer.commit()
        writer.flush()
        refreshReader()
    }

    private fun refreshReader() {
        val newReader = DirectoryReader.openIfChanged(reader)
        if (newReader != null) {
            reader.close()
            reader = newReader
            searcher = IndexSearcher(reader)
        }
    }
}

data class Candidate(
    val sanctionedId: String,
    val entryId: String,
    val source: SanctionedPersonSource,
    val type: SanctionedPersonType,
    val fullName: String,
    val aliases: List<String>,
    val dobRanges: List<DoBRange>,
    val score: Float,
)
