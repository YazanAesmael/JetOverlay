import java.util.Properties

// Load version.properties
val versionProps = Properties()
val versionFile = file("version.properties")
if (versionFile.exists()) {
    versionFile.inputStream().use { versionProps.load(it) }
} else {
    throw GradleException("Could not find version.properties!")
}

// Extract values
val vMajor = versionProps["VERSION_MAJOR"].toString().toInt()
val vMinor = versionProps["VERSION_MINOR"].toString().toInt()
val vPatch = versionProps["VERSION_PATCH"].toString().toInt()
val vCode = versionProps["VERSION_CODE"].toString().toInt()
val vName = "$vMajor.$vMinor.$vPatch"
val groupIdVal = versionProps["GROUP_ID"].toString()

// Share with Sub-modules via 'extra'
extra.apply {
    set("versionCode", vCode)
    set("versionName", vName)
    set("groupId", groupIdVal)
}

// Apply common config to all subprojects
allprojects {
    group = groupIdVal
    version = vName
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
}