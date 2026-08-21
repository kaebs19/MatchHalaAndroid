#!/usr/bin/env bash
#
# تهيئة Android SDK لجلسات Claude Code السحابية.
#
# الجلسة السحابية تبدأ من نسخة نظيفة بلا Android SDK، فيتعذّر تشغيل `./gradlew`
# وتُراجَع التغييرات بالعين وحدها. هذا السكربت يُنزّل الحدّ الأدنى اللازم للترجمة.
#
# ثلاثة التزامات:
#  - **ينتهي بصفر دائماً**: خروج غير صفري من خطّاف SessionStart يُفشل بدء الجلسة.
#  - **قابل لإعادة التشغيل**: يتحقّق قبل أن يُنزّل، فلا يُعيد جلب ما هو موجود.
#  - **لا يمسّ إعداداً محلياً**: إن كان ANDROID_HOME/ANDROID_SDK_ROOT مضبوطاً مسبقاً
#    (جهاز عليه Android Studio) يكتفي بالتحقّق ولا يُنزّل شيئاً داخل SDK المستخدم.
#
# الاستعمال: bash scripts/setup-android-sdk.sh
#
set -uo pipefail

COMPILE_SDK="${HALA_COMPILE_SDK:-36}"
BUILD_TOOLS="${HALA_BUILD_TOOLS:-36.0.0}"
MANAGED_SDK="${HALA_ANDROID_SDK_DIR:-$HOME/android-sdk}"

# نقطة تنزيل حزم Android — محجوبة افتراضياً في بيئات Claude السحابية.
PROBE_URL="https://dl.google.com/android/repository/repository2-3.xml"

# إصدارات cmdline-tools مرتّبة من الأحدث: أوّل ما يُنزَّل بنجاح يُعتمَد. القائمة
# تتقادم، فمتغيّر البيئة يسمح بتجاوزها دون تعديل السكربت.
CMDLINE_TOOLS_BUILDS="${HALA_CMDLINE_TOOLS_BUILDS:-13114758 12700392 11076708}"

log() { printf '[android-sdk] %s\n' "$*"; }

# ── هل SDK صالح للترجمة موجود في هذا المسار؟ ──
sdk_ready() {
    local root="$1"
    [ -n "$root" ] && [ -d "$root/platforms/android-$COMPILE_SDK" ]
}

# ── يكتب local.properties (مُتجاهَل في git) ليجد Gradle الـ SDK ──
write_local_properties() {
    # تعريفات منفصلة: bash يوسّع كل الطرف الأيمن قبل أن ينفّذ `local`، فـ"$root"
    # في نفس السطر يُقرأ وهو غير معرّف بعد — و`set -u` يُنهي السكربت بخطأ.
    local root="$1"
    local project_dir="$2"
    local line="sdk.dir=$root"
    [ -d "$project_dir" ] || return 0
    if [ -f "$project_dir/local.properties" ] &&
       grep -qxF "$line" "$project_dir/local.properties"; then
        return 0
    fi
    printf '%s\n' "$line" > "$project_dir/local.properties"
    log "كُتب local.properties → $root"
}

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"

# ── 1. SDK يخصّ المستخدم: تحقّق فقط، لا تنزيل ولا تعديل ──
USER_SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -n "$USER_SDK" ]; then
    if sdk_ready "$USER_SDK"; then
        log "جاهز: android-$COMPILE_SDK موجود في $USER_SDK"
    else
        log "تنبيه: $USER_SDK لا يحتوي platforms/android-$COMPILE_SDK — ثبّته من Android Studio."
    fi
    exit 0
fi

# ── 2. SDK مُدار سبق تنزيله ──
if sdk_ready "$MANAGED_SDK"; then
    export ANDROID_SDK_ROOT="$MANAGED_SDK"
    write_local_properties "$MANAGED_SDK" "$PROJECT_DIR"
    log "جاهز: $MANAGED_SDK"
    exit 0
fi

# ── 3. هل نقطة التنزيل مسموح بها أصلاً؟ ──
# فحص سريع قبل أي تنزيل: الحجب يظهر كـ 403 على نفق CONNECT، وبدون هذا الفحص
# تُهدر دقائق في محاولات فاشلة ثم تُنسب العلّة للسكربت لا لسياسة الشبكة.
code="$(curl -sS -m 20 -o /dev/null -w '%{http_code}' "$PROBE_URL" 2>/dev/null || echo 000)"
if [ "$code" != "200" ]; then
    cat <<'MSG'
[android-sdk] dl.google.com غير مسموح به في هذه البيئة — لا يمكن تنزيل Android SDK،
[android-sdk] ولا ترجمة المشروع (حزم AGP وAndroidX تُخدَم من هناك أيضاً).
[android-sdk]
[android-sdk] الحلّ (من إعدادات البيئة في claude.ai/code):
[android-sdk]   Network access → Custom
[android-sdk]   Allowed domains:
[android-sdk]     dl.google.com
[android-sdk]     maven.google.com
[android-sdk]   ✓ Also include default list of common package managers
[android-sdk]
[android-sdk] قائمة Trusted الافتراضية تشمل Maven Central وGradle لكن لا تشمل
[android-sdk] مستودع Google — وهو ما يُعطّل البناء.
MSG
    exit 0
fi

# ── 4. تنزيل cmdline-tools ──
log "تنزيل Android command-line tools…"
tmp="$(mktemp -d)"
# shellcheck disable=SC2064
trap "rm -rf '$tmp'" EXIT

zip=""
for build in $CMDLINE_TOOLS_BUILDS; do
    url="https://dl.google.com/android/repository/commandlinetools-linux-${build}_latest.zip"
    if curl -fsSL -m 300 -o "$tmp/tools.zip" "$url"; then
        zip="$tmp/tools.zip"
        log "الإصدار $build"
        break
    fi
done
if [ -z "$zip" ]; then
    log "تعذّر تنزيل cmdline-tools. جرّب: HALA_CMDLINE_TOOLS_BUILDS=<رقم البناء> bash $0"
    exit 0
fi

mkdir -p "$MANAGED_SDK/cmdline-tools"
unzip -q -o "$zip" -d "$tmp/x" || { log "أرشيف تالف"; exit 0; }
rm -rf "$MANAGED_SDK/cmdline-tools/latest"
mv "$tmp/x/cmdline-tools" "$MANAGED_SDK/cmdline-tools/latest" || { log "فشل التركيب"; exit 0; }

export ANDROID_SDK_ROOT="$MANAGED_SDK"
export ANDROID_HOME="$MANAGED_SDK"
sdkmanager="$MANAGED_SDK/cmdline-tools/latest/bin/sdkmanager"

# ── 5. التراخيص والحزم ──
yes 2>/dev/null | timeout 120 "$sdkmanager" --licenses >/dev/null 2>&1
log "تثبيت platform-tools وandroid-$COMPILE_SDK وbuild-tools;$BUILD_TOOLS…"
timeout 900 "$sdkmanager" \
    "platform-tools" \
    "platforms;android-$COMPILE_SDK" \
    "build-tools;$BUILD_TOOLS" >/dev/null 2>&1

if sdk_ready "$MANAGED_SDK"; then
    write_local_properties "$MANAGED_SDK" "$PROJECT_DIR"
    log "جاهز: $MANAGED_SDK — الآن تعمل ./gradlew :app:assembleDebug"
else
    log "لم يكتمل التثبيت. أعِد التشغيل يدوياً: bash $0"
fi
exit 0
