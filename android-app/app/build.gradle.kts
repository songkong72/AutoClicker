import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// 비밀 값은 코드에 두지 않는다. android-app/local.properties(깃에 안 올라감) 또는 환경변수에서 읽는다.
// 없으면 빈 값 → 초대코드가 발급/인증되지 않고 관리자 로그인이 막힌다(안전 쪽으로 실패).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
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
        buildConfigField("String", "ADMIN_PASSWORD_HASH", "\"" + secret("admin.password.hash", "ADMIN_PASSWORD_HASH") + "\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("release-key.jks")
            storePassword = "autoclicker123"
            keyAlias = "autoclicker"
            keyPassword = "autoclicker123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
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
