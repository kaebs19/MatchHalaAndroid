package com.chathala.hala.core.ads

import android.content.Context
import android.util.Log
import com.unity3d.ads.metadata.MetaData

/**
 * This release requests contextual Unity ads globally. UMP canRequestAds is not
 * permission for personalized advertising. Never manufacture privacy.consent=true
 * from that flag or from the absence of a US opt-out. GDPR TCF still goes through
 * the mediation adapter; this additional restriction also respects US opt-outs.
 * Apply before SDK initialization and after every UMP privacy-options callback.
 */
internal object UnityPrivacy {
    fun applyContextualOnly(context: Context): Boolean = runCatching {
        val metadata = MetaData(context.applicationContext)
        check(metadata.set("privacy.consent", false))
        metadata.commit()
        true
    }.getOrElse {
        Log.w(AdLog.TAG, "Unable to apply Unity privacy restriction", it)
        false
    }
}
