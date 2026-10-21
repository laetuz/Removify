package id.neotica.removify

import id.neotica.removify.utils.Constants
import kotlin.test.Test
import kotlin.test.assertEquals

class ConstantsTest {

    @Test
    fun appNameIsRemovify() {
        assertEquals("Removify", Constants.APP_NAME)
    }
}
