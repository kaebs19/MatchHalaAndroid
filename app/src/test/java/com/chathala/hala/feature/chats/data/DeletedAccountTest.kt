package com.chathala.hala.feature.chats.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeletedAccountTest {

    private val me = "me-1"

    private fun conversation(vararg participantIds: String) = Conversation(
        id = "conv-1",
        participants = participantIds.map { Participant(id = it, name = "user-$it") }
    )

    @Test
    fun `other participant is the one that is not me`() {
        val conv = conversation(me, "other-2")
        assertEquals("other-2", conv.otherParticipant(me)?.id)
        assertFalse(conv.isOtherAccountDeleted(me))
    }

    /** الخادم يحذف وثيقة المستخدم، فيسقط من populate ولا يبقى إلا أنا. */
    @Test
    fun `missing other participant means deleted account`() {
        val conv = conversation(me)
        assertNull(conv.otherParticipant(me))
        assertTrue(conv.isOtherAccountDeleted(me))
    }

    /** لا نُرجع نفسي كطرف آخر — الخطأ الذي كان يعرض اسم المستخدم لنفسه. */
    @Test
    fun `self is never returned as the other participant`() {
        assertNull(conversation(me).otherParticipant(me))
    }

    @Test
    fun `empty participants is not treated as deleted`() {
        assertFalse(conversation().isOtherAccountDeleted(me))
    }

    @Test
    fun `unknown current user is not treated as deleted`() {
        assertFalse(conversation(me, "other-2").isOtherAccountDeleted(null))
        assertFalse(conversation(me).isOtherAccountDeleted(null))
    }
}
