package id.neotica.removify.domain.service

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.MediaType
import id.neotica.removify.domain.model.SwipeDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeckEngineRestoreTest {

    private fun item(id: String) = MediaItem(
        id = id,
        name = "$id.jpg",
        type = MediaType.IMAGE,
        sizeBytes = 100,
        dateTakenEpochMs = 0,
    )

    @Test
    fun restoresQueueOrderAndDecisions() {
        val items = listOf(item("a"), item("b"), item("c"), item("d"))
        val engine = DeckEngine(
            items = items,
            initialDecisions = mapOf("a" to SwipeDecision.KEEP, "d" to SwipeDecision.TRASH),
            initialSkipCounts = mapOf("c" to 1),
            initialQueue = listOf("b", "c"),
        )

        assertEquals("b", engine.current?.id)
        assertEquals(listOf("c"), engine.nextUp.map { it.id })
        assertEquals(2, engine.decidedCount)
        assertEquals(1, engine.skippedCount)
        assertEquals(4, engine.total)
    }

    @Test
    fun emptyRestoredQueueIsFinished() {
        val engine = DeckEngine(
            items = listOf(item("a")),
            initialDecisions = mapOf("a" to SwipeDecision.KEEP),
            initialQueue = emptyList(),
        )

        assertTrue(engine.isFinished)
        assertEquals(1, engine.decidedCount)
    }

    @Test
    fun dropsUnknownIdsFromSnapshot() {
        val engine = DeckEngine(
            items = listOf(item("a")),
            initialDecisions = mapOf("missing" to SwipeDecision.KEEP),
            initialSkipCounts = mapOf("missing" to 1),
            initialQueue = listOf("missing", "a"),
        )

        assertEquals("a", engine.current?.id)
        assertEquals(0, engine.decidedCount)
        assertEquals(0, engine.skippedCount)
    }

    @Test
    fun snapshotRoundTripPreservesState() {
        val items = listOf(item("a"), item("b"), item("c"))
        val original = DeckEngine(items)
        original.decide(SwipeDecision.KEEP)
        original.skip()
        original.decide(SwipeDecision.TRASH)

        val snapshot = original.snapshot()
        val restored = DeckEngine(
            items = items,
            initialDecisions = snapshot.decisions,
            initialSkipCounts = snapshot.skipCounts,
            initialQueue = snapshot.queue,
        )

        assertEquals(original.current?.id, restored.current?.id)
        assertEquals(original.nextUp.map { it.id }, restored.nextUp.map { it.id })
        assertEquals(original.decisionsMap, restored.decisionsMap)
        assertEquals(original.decidedCount, restored.decidedCount)
        assertEquals(original.skippedCount, restored.skippedCount)
        assertEquals(original.total, restored.total)
    }
}
