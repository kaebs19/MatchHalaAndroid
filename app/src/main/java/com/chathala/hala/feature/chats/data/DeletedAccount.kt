package com.chathala.hala.feature.chats.data

import com.chathala.hala.R
import com.chathala.hala.core.i18n.S

/**
 * الحساب المحذوف: الخادم يحذف وثيقة المستخدم نهائياً ويُبقي المحادثات والرسائل،
 * فتُرجع `populate` مصفوفة مشاركين بلا الطرف الآخر (أنا فقط).
 * لذلك «غياب الطرف الآخر» هو دليل الحذف، ولا يوجد حقل من الخادم يدلّ عليه.
 */
/** يُقرأ عند كل استدعاء — لو كان `const` لتجمّد على لغة وقت التصريف. */
val DELETED_ACCOUNT_NAME: String get() = S.get(R.string.deleted_account_name)

/** الطرف الآخر في المحادثة — بلا الرجوع إلى نفسي عند غيابه. */
fun Conversation.otherParticipant(currentUserId: String?): Participant? =
    participants.firstOrNull { it.id != currentUserId }

/**
 * هل حُذف حساب الطرف الآخر؟
 * نشترط وجود مشاركين فعليين (أنا على الأقل) حتى لا نُصنّف ردّاً ناقصاً
 * أو نسخة كاش بلا مشاركين على أنها حساب محذوف.
 */
fun Conversation.isOtherAccountDeleted(currentUserId: String?): Boolean =
    currentUserId != null &&
        participants.isNotEmpty() &&
        participants.none { it.id != currentUserId }
