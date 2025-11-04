<!--
  ~ SPDX-FileCopyrightText: 2025 Saeed <saidhany244@example.com>
  ~ SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 -->
# تطبيق [Nextcloud](https://nextcloud.com)لأجهزة أندرويد 📱

[![حالة REUSE](https://api.reuse.software/badge/github.com/nextcloud/android)](https://api.reuse.software/info/github.com/nextcloud/android)  
[![حالة البناء](https://drone.nextcloud.com/api/badges/nextcloud/android/status.svg)](https://drone.nextcloud.com/nextcloud/android)  
[![تقييم Codacy](https://app.codacy.com/project/badge/Grade/fb4cf26336774ee3a5c9adfe829c41aa)](https://app.codacy.com/gh/nextcloud/android/dashboard)  
[![الإصدارات](https://img.shields.io/github/release/nextcloud/android.svg)](https://github.com/nextcloud/android/releases/latest)

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" alt="تحميل من Google Play" height="80">](https://play.google.com/store/apps/details?id=com.nextcloud.client)  
[<img src="https://f-droid.org/badge/get-it-on.png" alt="احصل عليه من F-Droid" height="80">](https://f-droid.org/packages/com.nextcloud.client/)

## التحقق من توقيع التطبيق 🔐

للتأكد من صحة ملف APK:

- ملف APK باسم "gplay" متوفر [هنا](https://github.com/nextcloud/android/releases) أو عبر متجر Google Play  
- ملف APK باسم "nextcloud" متوفر [هنا](https://github.com/nextcloud/android/releases)  
- غير مناسب لتحميلات F-Droid، لأن F-Droid يقوم بتوقيعه بنفسه

```plaintext
SHA-256: fb009522f65e25802261b67b10a45fd70e610031976f40b28a649e152ded0373  
SHA-1: 74aa1702e714941be481e1f7ce4a8f779c19dcea
```

**تطبيق Nextcloud لأندرويد يتيح لك إدارة بياناتك بسهولة على خادم Nextcloud الخاص بك.**

## الحصول على الدعم 🆘

إذا واجهت مشكلة أو لديك سؤال، يمكنك زيارة [منتدى الدعم](https://help.nextcloud.com/c/clients/android).  
إذا اكتشفت خطأ أو لديك اقتراح لتحسين التطبيق، يمكنك [فتح قضية جديدة على GitHub](https://github.com/nextcloud/android/issues).

إذا لم تكن متأكدًا ما إذا كانت المشكلة ناتجة عن التطبيق أو الإعدادات أو الخادم، فابدأ بالسؤال في المنتدى، ثم عد إلى GitHub إذا لزم الأمر.

> ملاحظة: هذا المستودع خاص بتطبيق أندرويد فقط. إذا كانت المشكلة في الخادم، يرجى التواصل مع [فريق خادم Nextcloud](https://github.com/nextcloud/server).

## كيف تساهم في المشروع 🚀

هناك العديد من الطرق للمساهمة، سواء كنت مبرمجًا أو لا:

- مساعدة المستخدمين في المنتدى: https://help.nextcloud.com  
- ترجمة التطبيق عبر [Transifex](https://app.transifex.com/nextcloud/nextcloud/android/)  
- الإبلاغ عن المشاكل أو تقديم اقتراحات عبر [GitHub Issues](https://github.com/nextcloud/android/issues/new/choose)  
- تنفيذ إصلاحات أو تحسينات عبر Pull Requests  
- مراجعة [طلبات الدمج](https://github.com/nextcloud/android/pulls)  
- اختبار النسخ التجريبية أو اليومية أو المرشحة للإصدار  
- تحسين [التوثيق](https://github.com/nextcloud/documentation/)  
- اختبار الميزات الأساسية في آخر إصدار مستقر  
- تعلم كيفية جمع سجلات الأخطاء (logcat) لتقديم تقارير دقيقة

## إرشادات المساهمة والترخيص 📜

- الترخيص: [GPLv2](https://github.com/nextcloud/android/blob/master/LICENSE.txt)  
- جميع المساهمات بعد 16 يونيو 2016 تعتبر مرخصة تحت AGPLv3 أو أي إصدار لاحق  
- لا حاجة لتوقيع اتفاقية مساهم (CLA)  
- يُفضل إضافة السطر التالي في رأس الملف عند إجراء تغييرات كبيرة:

```plaintext
SPDX-FileCopyrightText: <السنة> <اسمك> <بريدك الإلكتروني>
```

يرجى قراءة [مدونة السلوك](https://nextcloud.com/community/code-of-conduct/) لضمان بيئة تعاون إيجابية.  
راجع أيضًا [إرشادات المساهمة](https://github.com/nextcloud/android/blob/master/CONTRIBUTING.md).

## ابدأ بالمساهمة 🔧

- اقرأ [SETUP.md](https://github.com/nextcloud/android/blob/master/SETUP.md) و[CONTRIBUTING.md](https://github.com/nextcloud/android/blob/master/CONTRIBUTING.md)  
- قم بعمل fork للمستودع وابدأ بإرسال Pull Requests إلى فرع master  
- يمكنك البدء بمراجعة [طلبات الدمج](https://github.com/nextcloud/android/pulls) أو العمل على [القضايا المبتدئة](https://github.com/nextcloud/android/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22)

## جمع سجلات الأخطاء (logcat) 🔍

### على لينكس:

- فعّل USB-Debugging على هاتفك  
- افتح الطرفية وأدخل:

```bash
adb logcat --pid=$(adb shell pidof -s 'com.nextcloud.client') > logcatOutput.txt
```

> تأكد من تثبيت [adb](https://developer.android.com/studio/releases/platform-tools.html)

### على ويندوز:

- حمّل [Minimal ADB and Fastboot](https://forum.xda-developers.com/t/tool-minimal-adb-and-fastboot-2-9-18.2317790/#post-42407269)  
- فعّل USB-Debugging  
- افتح البرنامج وأدخل:

```bash
adb shell pidof -s 'com.nextcloud.client'
```

- استخدم الناتج كـ `<processID>` في الأمر التالي:

```bash
adb logcat --pid=<processID> > "%USERPROFILE%\Downloads\logcatOutput.txt"
```

### على الجهاز (مع صلاحيات root):

```bash
su
logcat -d --pid $(pidof -s com.nextcloud.client) -f /sdcard/logcatOutput.txt
```

أو استخدم تطبيقات مثل [CatLog](https://play.google.com/store/apps/details?id=com.nolanlawson.logcat) أو [aLogcat](https://play.google.com/store/apps/details?id=org.jtb.alogcat)

## النسخة التطويرية 🛠️

- [تحميل مباشر للـ APK](https://download.nextcloud.com/android/dev/latest.apk)  
- [F-Droid النسخة التجريبية](https://f-droid.org/en/packages/com.nextcloud.android.beta/)

## المشاكل المعروفة والأسئلة الشائعة

### الإشعارات الفورية لا تعمل في نسخ F-Droid

بسبب اعتمادها على خدمات Google Play، لا تعمل الإشعارات الفورية في نسخ F-Droid حاليًا.
