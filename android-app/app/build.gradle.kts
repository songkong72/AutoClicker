import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// 비밀 값은 코드에 두지 않는다. android-app/local.properties(깃에 안 올라감) 또는 환경변수에서 읽는다.
// 없으면 빈 값 → 초대코드가 발급/인증되지 않고 관리자 로그인이 막힌다(안전 쪽으로 실패).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(StringReader(f.readText().removePrefix("\uFEFF")))
}
fun secret(prop: String, env: String): String =
    (localProps.getProperty(prop) ?: System.getenv(env) ?: "").trim()

android {
    namespace = "com.sejun.autoclicker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sejun.autoclicker"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "INVITE_SECRET", "\"" + secret("invite.secret", "INVITE_SECRET") + "\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"" + secret("firebase.api.key", "FIREBASE_API_KEY") + "\"")
        buildConfigField("String", "ADMIN_PASSWORD_HASH", "\"" + secret("admin.password.hash", "ADMIN_PASSWORD_HASH") + "\"")
    }

    // 서명 키는 저장소에 없다. local.properties 의 signing.* 값(setup-signing.bat 이 만든다)으로만 서명한다.
    // 값이 없으면(예: CI) 안드로이드 기본 디버그 키로 서명한다.
    val signingFile = localProps.getProperty("signing.store.file")?.trim().orEmpty()
    val hasSigning = signingFile.isNotEmpty() && file(signingFile).exists()
    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(signingFile)
                storePassword = (localProps.getProperty("signing.store.password") ?: "").trim()
                keyAlias = (localProps.getProperty("signing.key.alias") ?: "").trim()
                keyPassword = (localProps.getProperty("signing.key.password") ?: "").trim()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
}
