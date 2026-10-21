package id.neotica.removify.domain.service

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.SessionSummary
import id.neotica.removify.domain.model.SwipeDecision

class DeckEngine(items: List<MediaItem>) {

    private data class DecisionRecord(val item: MediaItem, val decision: SwipeDecision)

    private val allItems = items
    private val itemsById = items.associateBy { it.id }
    private val queue = ArrayDeque(items)
    private val decisions = LinkedHashMap<String, SwipeDecision>()
    private val skipCounts = mutableMapOf<String, Int>()
    private val undoStack = ArrayDeque<DecisionRecord>()

    val total: Int get() = allItems.size
    val current: MediaItem? get() = queue.firstOrNull()
    val nextUp: List<MediaItem> get() = listOfNotNull(queue.getOrNull(1), queue.getOrNull(2))
    val decidedCount: Int get() = decisions.size
    val isFinished: Boolean get() = queue.isEmpty()
    val skippedCount: Int get() = skipCounts.size
    val decisionsMap: Map<String, SwipeDecision> get() = decisions.toMap()

    fun decide(decision: SwipeDecision): MediaItem? {
        val item = queue.removeFirstOrNull() ?: return null
        decisions[item.id] = decision
        skipCounts.remove(item.id)
        pushUndo(DecisionRecord(item, decision))
        return item
    }

    fun skip(): MediaItem? {
        val item = queue.removeFirstOrNull() ?: return null
        val count = (skipCounts[item.id] ?: 0) + 1
        skipCounts[item.id] = count
        if (count == 1) {
            queue.addLast(item)
        }
        pushUndo(DecisionRecord(item, SwipeDecision.SKIP))
        return item
    }

    fun undo(): MediaItem? {
        val record = undoStack.removeLastOrNull() ?: return null
        when (record.decision) {
            SwipeDecision.SKIP -> {
                queue.remove(record.item)
                queue.addFirst(record.item)
                val count = (skipCounts[record.item.id] ?: 1) - 1
                if (count <= 0) skipCounts.remove(record.item.id) else skipCounts[record.item.id] = count
            }

            else -> {
                decisions.remove(record.item.id)
                queue.addFirst(record.item)
            }
        }
        return record.item
    }

    fun trashItems(): List<MediaItem> = allItems.filter { decisions[it.id] == SwipeDecision.TRASH }

    fun summary(): SessionSummary {
        val kept = decisions.values.count { it == SwipeDecision.KEEP }
        val trashed = decisions.values.count { it == SwipeDecision.TRASH }
        val trashBytes = decisions.entries
            .filter { it.value == SwipeDecision.TRASH }
            .sumOf { itemsById[it.key]?.sizeBytes ?: 0L }
        return SessionSummary(
            keptCount = kept,
            trashCount = trashed,
            skippedCount = total - kept - trashed,
            trashBytes = trashBytes,
        )
    }

    private fun pushUndo(record: DecisionRecord) {
        undoStack.addLast(record)
        if (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
    }

    private companion object {
        const val UNDO_LIMIT = 100
    }
}
