package com.chathala.hala.feature.chats.data

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.lang.reflect.Type

/**
 * يجعل [MessageSender] متسامحاً: يقبل الحقل سواءً جاء ككائن كامل
 * `{ "_id": "...", "name": "..." }` أو كنصّ يحوي المعرّف فقط `"64f..."`.
 *
 * الخادم أحياناً يُرجع `sender` كمعرّف نصّي (خاصةً في استجابات/أحداث مختصرة)،
 * فيمنع هذا المحوّل خطأ: «Expected BEGIN_OBJECT but was STRING at path …sender».
 */
object MessageSenderAdapterFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: Set<Annotation>,
        moshi: Moshi
    ): JsonAdapter<*>? {
        if (type != MessageSender::class.java) return null
        // المحوّل الافتراضي للحالة العادية (كائن)
        val delegate = moshi.nextAdapter<MessageSender>(this, type, annotations)
        return object : JsonAdapter<MessageSender>() {
            override fun fromJson(reader: JsonReader): MessageSender? = when (reader.peek()) {
                JsonReader.Token.STRING -> MessageSender(id = reader.nextString())
                JsonReader.Token.NULL -> reader.nextNull()
                else -> delegate.fromJson(reader)
            }

            override fun toJson(writer: JsonWriter, value: MessageSender?) {
                delegate.toJson(writer, value)
            }
        }
    }
}
