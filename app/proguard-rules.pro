# Keep Glance ActionCallback zero-argument constructors under R8
-keep class * implements androidx.glance.appwidget.action.ActionCallback { public <init>(); }
