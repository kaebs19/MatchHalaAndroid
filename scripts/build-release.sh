#!/usr/bin/env bash
#
# بناء حزمة الرفع (AAB موقّعة) لمتجر Play.
#
# السبب في وجوده: خطوات الرفع التي تُنسى ليست البناء نفسه، بل ما حوله — رقم
# إصدار لم يُرفع، ملاحظات متجر تتجاوز 500 حرف، ترجمة ناقصة، أو حزمة غير موقّعة
# تُرفض عند الرفع. السكربت يفحص هذا كلّه **قبل** أن يبني، فيفشل في ثوانٍ بدل
# أن يفشل في Play Console بعد بناء طويل.
#
# الاستعمال:
#   bash scripts/build-release.sh              # فحص + بناء AAB موقّعة
#   bash scripts/build-release.sh --check      # الفحوصات فقط، دون بناء
#   bash scripts/build-release.sh --apk        # يبني APK أيضاً (للتجربة على جهاز)
#
# المتطلّبات: Android SDK (انظر scripts/setup-android-sdk.sh) و keystore.properties
# في جذر المشروع — وكلاهما غير مُضمّن في المستودع.
#
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_MODULE=":app"
BUILD_GRADLE="$ROOT/app/build.gradle.kts"
KEYSTORE_PROPS="$ROOT/keystore.properties"
WHATSNEW_DIR="$ROOT/distribution/whatsnew"
RELEASE_ROOT="$ROOT/distribution/release"
WHATSNEW_LIMIT=500   # حدّ Play Console لكل لغة

CHECK_ONLY=0
BUILD_APK=0
for arg in "$@"; do
    case "$arg" in
        --check) CHECK_ONLY=1 ;;
        --apk)   BUILD_APK=1 ;;
        -h|--help)
            # كتلة التعليق التي تلي السطر الأوّل هي التوثيق — تُطبع كما هي.
            awk 'NR>1 { if ($0 !~ /^#/) exit; sub(/^# ?/, ""); print }' "${BASH_SOURCE[0]}"
            exit 0 ;;
        *) printf 'وسيط غير معروف: %s (جرّب --help)\n' "$arg" >&2; exit 2 ;;
    esac
done

FAILED=0
log()  { printf '[release] %s\n' "$*"; }
ok()   { printf '[release] ✓ %s\n' "$*"; }
fail() { printf '[release] ✗ %s\n' "$*" >&2; FAILED=1; }

# ── 1. بيانات التوقيع ───────────────────────────────────────
# بلا هذا الملف يبني Gradle حزمة غير موقّعة **دون أن يشكو** — لأن signingConfig
# مشروط بوجوده في build.gradle.kts. المتجر يرفضها، فنفشل هنا صراحةً.
check_signing() {
    if [ ! -f "$KEYSTORE_PROPS" ]; then
        fail "لا يوجد keystore.properties في جذر المشروع — الحزمة ستُبنى بلا توقيع ويرفضها المتجر."
        return
    fi
    local missing=""
    local key
    for key in storeFile storePassword keyAlias keyPassword; do
        grep -q "^[[:space:]]*$key[[:space:]]*=" "$KEYSTORE_PROPS" || missing="$missing $key"
    done
    if [ -n "$missing" ]; then
        fail "keystore.properties ينقصه:$missing"
        return
    fi
    # storeFile نسبيّ إلى مجلّد app/ (وهو ما يفعله file() داخل build.gradle.kts)
    local store
    store="$(sed -n 's/^[[:space:]]*storeFile[[:space:]]*=[[:space:]]*//p' "$KEYSTORE_PROPS" | head -1 | tr -d '\r')"
    case "$store" in
        /*) : ;;
        *) store="$ROOT/app/$store" ;;
    esac
    if [ -f "$store" ]; then
        ok "بيانات التوقيع كاملة، ومفتاح التوقيع موجود."
    else
        fail "مفتاح التوقيع غير موجود في المسار: $store"
    fi
}

# ── 2. رقم الإصدار ──────────────────────────────────────────
# versionCode يجب أن يزيد عن آخر ما رُفع، وإلا رُفضت الحزمة. لا نعرف من هنا ما
# في المتجر، فنطبع الرقمين ليؤكّدهما من يبني.
check_version() {
    VERSION_CODE="$(sed -n 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*\([0-9]\{1,\}\).*/\1/p' "$BUILD_GRADLE" | head -1)"
    VERSION_NAME="$(sed -n 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$BUILD_GRADLE" | head -1)"
    if [ -z "$VERSION_CODE" ] || [ -z "$VERSION_NAME" ]; then
        fail "تعذّرت قراءة versionCode/versionName من app/build.gradle.kts"
        return
    fi
    ok "الإصدار: $VERSION_NAME ($VERSION_CODE) — تأكّد أن $VERSION_CODE أكبر من آخر رقم في المتجر."
}

# ── 3. ملاحظات المتجر ───────────────────────────────────────
# Play Console يقصّ ما يتجاوز 500 حرف بلا تحذير. نعدّ بالأحرف لا بالبايتات:
# العربية متعدّدة البايتات، والعدّ بالبايت يضخّم الرقم للضعف تقريباً.
#
# `wc -m` لا يصلح هنا: يعدّ الأحرف فقط إن كانت لغة النظام UTF-8، وإلّا ارتدّ
# صامتاً إلى عدّ البايتات (يحدث في حاويات CI بلا LANG). فنحذف بايتات المتابعة
# في UTF-8 (0x80–0xBF) ونعدّ الباقي — عددُ الأحرف بالضبط، مهما كانت اللغة.
utf8_chars() {
    LC_ALL=C tr -d '\200-\277' < "$1" | wc -c | tr -d '[:space:]'
}

check_whatsnew() {
    if [ ! -d "$WHATSNEW_DIR" ]; then
        fail "لا يوجد مجلّد ملاحظات الإصدار: $WHATSNEW_DIR"
        return
    fi
    local found=0 file chars
    for file in "$WHATSNEW_DIR"/whatsnew-*; do
        [ -f "$file" ] || continue
        found=1
        chars="$(utf8_chars "$file")"
        if [ "$chars" -gt "$WHATSNEW_LIMIT" ]; then
            fail "$(basename "$file"): $chars حرفاً — يتجاوز الحدّ ($WHATSNEW_LIMIT)."
        elif [ "$chars" -lt 2 ]; then
            fail "$(basename "$file"): فارغ."
        else
            ok "$(basename "$file"): $chars حرفاً."
        fi
    done
    [ "$found" -eq 1 ] || fail "لا توجد ملفات whatsnew-* في $WHATSNEW_DIR"
}

# ── 4. شجرة git ─────────────────────────────────────────────
# تحذير لا خطأ: حزمة مبنيّة من تعديلات غير مودعة لا يمكن ردّها إلى كودها عند
# تشخيص انهيار في المتجر.
check_git() {
    command -v git >/dev/null 2>&1 || return 0
    git -C "$ROOT" rev-parse --git-dir >/dev/null 2>&1 || return 0
    if [ -n "$(git -C "$ROOT" status --porcelain)" ]; then
        log "⚠ توجد تعديلات غير مودعة — الحزمة لن تكون قابلة للتتبّع إلى commit."
    else
        ok "شجرة git نظيفة (commit: $(git -C "$ROOT" rev-parse --short HEAD))."
    fi
}

# ── 5. وجود الـ SDK ─────────────────────────────────────────
check_sdk() {
    local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
    if [ -z "$sdk" ] && [ -f "$ROOT/local.properties" ]; then
        sdk="$(sed -n 's/^sdk\.dir=//p' "$ROOT/local.properties" | head -1)"
    fi
    if [ -n "$sdk" ] && [ -d "$sdk" ]; then
        ok "Android SDK: $sdk"
    else
        fail "لا يوجد Android SDK — شغّل: bash scripts/setup-android-sdk.sh"
    fi
}

# ── مجلّد النسخة ────────────────────────────────────────────
# مجلّد واحد لكل (versionName-versionCode) يجمع ما يلزم الرفع: لقطة من ملاحظات
# المتجر كما كانت وقت البناء، والحزمة الناتجة. الفائدة عند تشخيص انهيار بعد
# شهرين: ما رُفع بالضبط، وبأي ملاحظات، ومن أي commit.
prepare_release_dir() {
    RELEASE_DIR="$RELEASE_ROOT/$VERSION_NAME-$VERSION_CODE"
    mkdir -p "$RELEASE_DIR/artifacts" || { fail "تعذّر إنشاء $RELEASE_DIR"; return; }

    # يُبقي artifacts/ في git ويُخرج محتوياته منه (الحزمة كبيرة، والخرائط داخلها).
    if [ ! -f "$RELEASE_DIR/artifacts/.gitignore" ]; then
        printf '*\n!.gitignore\n' > "$RELEASE_DIR/artifacts/.gitignore"
    fi

    local file
    for file in "$WHATSNEW_DIR"/whatsnew-*; do
        [ -f "$file" ] || continue
        cp "$file" "$RELEASE_DIR/$(basename "$file")"
    done
    ok "مجلّد النسخة: ${RELEASE_DIR#"$ROOT"/}"
}

log "فحص جاهزية الرفع…"
check_version
check_signing
check_whatsnew
check_sdk
check_git

if [ "$FAILED" -ne 0 ]; then
    printf '\n[release] توقّف: عالِج ما سبق ثم أعد التشغيل.\n' >&2
    exit 1
fi

prepare_release_dir
# فشل هنا (صلاحيات، قرص ممتلئ) يعني ألّا مكان تُنسخ إليه المخرجات لاحقاً — نتوقّف
# قبل بناء طويل بدل أن ننتهي إلى حزمة لا تُؤرشَف.
if [ "$FAILED" -ne 0 ]; then
    printf '\n[release] توقّف: تعذّر تجهيز مجلّد النسخة.\n' >&2
    exit 1
fi

if [ "$CHECK_ONLY" -eq 1 ]; then
    log "الفحوصات اكتملت (--check: لم يُبنَ شيء)."
    exit 0
fi

# ── البناء ──────────────────────────────────────────────────
# lintRelease قبل البناء: قاعدة MissingTranslation/ExtraTranslation مضبوطة على
# error في build.gradle.kts، فنصّ بلا ترجمة يُوقف الرفع هنا لا في المتجر.
TASKS="clean $GRADLE_MODULE:lintRelease $GRADLE_MODULE:testReleaseUnitTest $GRADLE_MODULE:bundleRelease"
[ "$BUILD_APK" -eq 1 ] && TASKS="$TASKS $GRADLE_MODULE:assembleRelease"

log "البناء: ./gradlew $TASKS"
# shellcheck disable=SC2086
"$ROOT/gradlew" -p "$ROOT" $TASKS || {
    printf '\n[release] فشل البناء.\n' >&2
    exit 1
}

AAB="$ROOT/app/build/outputs/bundle/release/app-release.aab"
APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
MAPPING="$ROOT/app/build/outputs/mapping/release/mapping.txt"
DEST="$RELEASE_DIR/artifacts"

printf '\n'
if [ ! -f "$AAB" ]; then
    fail "لم تُنتج الحزمة في المسار المتوقّع: $AAB"
    exit 1
fi

# ننسخ لا ننقل: `./gradlew clean` يمسح app/build، فتضيع حزمة رُفعت فعلاً.
cp "$AAB" "$DEST/" && ok "الحزمة: ${DEST#"$ROOT"/}/app-release.aab ($(du -h "$AAB" | cut -f1))"

# ملف الخرائط مُضمَّن في الـ AAB تلقائياً (تقارير Play مفكوكة التشويش دون رفع
# يدوي) — نسخته هنا للأرشفة، تلزم لو أُعيد تشويش أثر قديم يدوياً.
[ -f "$MAPPING" ] && cp "$MAPPING" "$DEST/" && ok "خرائط R8: ${DEST#"$ROOT"/}/mapping.txt"

if [ "$BUILD_APK" -eq 1 ] && [ -f "$APK" ]; then
    cp "$APK" "$DEST/" && ok "APK (للتجربة، لا يُرفع): ${DEST#"$ROOT"/}/app-release.apk"
fi

# بصمة البناء: تربط الحزمة بالكود الذي أنتجها. بلا هذا يصير تشخيص انهيار في
# المتجر تخميناً لأي commit كان.
{
    printf 'versionName: %s\n' "$VERSION_NAME"
    printf 'versionCode: %s\n' "$VERSION_CODE"
    printf 'builtAt:     %s\n' "$(date -u '+%Y-%m-%d %H:%M UTC')"
    if git -C "$ROOT" rev-parse --git-dir >/dev/null 2>&1; then
        printf 'commit:      %s\n' "$(git -C "$ROOT" rev-parse HEAD)"
        printf 'branch:      %s\n' "$(git -C "$ROOT" rev-parse --abbrev-ref HEAD)"
        [ -n "$(git -C "$ROOT" status --porcelain)" ] && printf 'dirty:       نعم — الحزمة لا تطابق الـ commit تماماً\n'
    fi
    printf 'sha256:      %s\n' "$(sha256sum "$AAB" 2>/dev/null | cut -d" " -f1)"
} > "$DEST/build-info.txt"
ok "بصمة البناء: ${DEST#"$ROOT"/}/build-info.txt"

cat <<EOF

[release] الخطوات التالية في Play Console:
  1. Internal testing → Create new release → ارفع الحزمة من مجلّد النسخة أعلاه
  2. الصق ملاحظات الإصدار من مجلّد النسخة (whatsnew-ar و whatsnew-en-US)
  3. راجع Pre-launch report قبل الطرح الكامل
  4. بعد نجاح الرفع: ارفع رقم versionCode في app/build.gradle.kts للإصدار القادم
EOF
