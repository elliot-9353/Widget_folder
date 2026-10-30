# Folder Widget

ويدجت أندرويد: مجلد تطبيقات بشكل مخصص على الشاشة الرئيسية. بالضغط عليه تظهر كل التطبيقات.

## التشغيل
1. افتح المجلد بـ Android Studio (Koala أو أحدث) واترك الـ Gradle Sync يكتمل.
2. شغّل التطبيق على جهازك (Run ▶).
3. اضغط مطولاً على الشاشة الرئيسية ← Widgets ← Folder Widget (أو من زر "إضافة المجلد" داخل التطبيق).
4. اختر الشكل واللون وعدد التطبيقات والتطبيقات، ثم احفظ.

## إضافة شكل جديد
1. انسخ `shapes/FlowerShape.kt` إلى ملف جديد (مثلاً `CircleShape.kt`) وعدّل الدوال الثلاث:
   `outline` (حدود الشكل) و`slots` (أماكن الأيقونات) و`iconSize` (حجم الأيقونة).
2. غيّر `id` و`nameRes` (وأضف الاسم في `res/values/strings.xml` و`values-en/strings.xml`).
3. سجّله في `shapes/FolderShapes.kt` داخل القائمة `all`.

واجهة الإعدادات والرسم يلتقطون الشكل الجديد تلقائياً.

## هيكل الملفات
- `shapes/`: الأشكال (ملف لكل شكل) + السجل
- `render/FolderRenderer.kt`: يرسم المجلد كصورة
- `data/FolderConfig.kt`: الإعدادات وحفظها لكل ويدجت
- `FolderWidgetProvider.kt`: الويدجت نفسه
- `ConfigActivity.kt` + `ui/ConfigScreen.kt`: شاشة الإعدادات
- `PopupActivity.kt` + `ui/PopupScreen.kt`: النافذة التي تفتح عند الضغط
