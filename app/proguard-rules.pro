# Keep chart classes: MPAndroidChart resolves renderers and value formatters
# reflectively in a few places, and its public API is used from XML.
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Room entities are read/written through generated code, but generic signatures
# and annotations are still useful for diagnostics and keep the schema stable.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Tink (pulled in by security-crypto) references JSR-305 annotations that are not
# packaged at runtime; they are compile-time only.
-dontwarn javax.annotation.**

# Material components inflate views by name from XML.
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
