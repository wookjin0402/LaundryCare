pluginManagement {
    repositories {
        google() // 🌟 까다로운 필터를 없애고 구글 서버를 시원하게 열어줍니다!
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // 👈 차트 라이브러리 필수 주소
    }
}

rootProject.name = "LaundryCare_Android"
include(":app")