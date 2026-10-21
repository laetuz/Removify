package id.neotica.removify.domain.service

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.MediaType
import id.neotica.removify.domain.model.SwipeDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeckEngineTest {

    private fun item(id: String, size: Long = 100) = MediaItem(
        id = id,
        name = "$id.jpg",
        type = MediaType.IMAGE,
        sizeBytes = size,
        dateTakenEpochMs = 0,
    )

    @Test
    fun advancesInOrderAndCountsDecisions() {
        val engine = DeckEngine(listOf(item("a"), item("b"), item("c")))

        assertEquals("a", engine.current?.id)
        assertEquals(listOf("b", "c"), engine.nextUp.map { it.id })

        engine.decide(SwipeDecision.KEEP)
        assertEquals("b", engine.current?.id)

        engine.decide(SwipeDecision.TRASH)
        assertEquals("c", engine.current?.id)

        engine.skip()
        engine.skip()
        assertTrue(engine.isFinished)

        val summary = engine.summary()
        assertEquals(1, summary.keptCount)
        assertEquals(1, summary.trashCount)
        assertEquals(1, summary.skippedCount)
        assertEquals(100, summary.trashBytes)
    }

    @Test
    fun skippingRequeuesOnceThenFinishesUndecided() {
        val engine = DeckEngine(listOf(item("a"), item("b")))

        engine.skip()
        assertEquals("b", engine.current?.id)
        assertEquals(listOf("a"), engine.nextUp.map { it.id })

        engine.skip()
        assertEquals("a", engine.current?.id)

        engine.skip()
        assertEquals("b", engine.current?.id)

        engine.skip()
        assertTrue(engine.isFinished)
        assertEquals(2, engine.summary().skippedCount)
        assertEquals(2, engine.skippedCount)
    }

    @Test
    fun undoRestoresPreviousDecision() {
        val engine = DeckEngine(listOf(item("a"), item("b")))

        engine.decide(SwipeDecision.KEEP)
        engine.decide(SwipeDecision.TRASH)
        assertTrue(engine.isFinished)

        engine.undo()
        assertEquals("b", engine.current?.id)
        assertEquals(0, engine.summary().trashCount)

        engine.undo()
        assertEquals("a", engine.current?.id)
        assertEquals(0, engine.decidedCount)
    }

    @Test
    fun undoSkipReturnsItemToFront() {
        val engine = DeckEngine(listOf(item("a"), item("b")))

        engine.skip()
        assertEquals("b", engine.current?.id)

        engine.undo()
        assertEquals("a", engine.current?.id)
        assertEquals(0, engine.skippedCount)
    }

    @Test
    fun trashItemsFollowSourceOrder() {
        val engine = DeckEngine(listOf(item("a", 10), item("b", 20), item("c", 30)))
        engine.decide(SwipeDecision.TRASH)
        engine.decide(SwipeDecision.KEEP)
        engine.decide(SwipeDecision.TRASH)

        assertEquals(listOf("a", "c"), engine.trashItems().map { it.id })
        assertEquals(40, engine.summary().trashBytes)
    }

    @Test
    fun emptyDeckIsFinished() {
        val engine = DeckEngine(emptyList())

        assertTrue(engine.isFinished)
        assertNull(engine.current)
        assertEquals(0, engine.summary().trashCount)
    }
}
