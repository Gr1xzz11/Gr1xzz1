plugins { id("com.android.application") }

android {
    namespace = "dev.grxt.deviceinspector"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.grxt.deviceinspector"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
