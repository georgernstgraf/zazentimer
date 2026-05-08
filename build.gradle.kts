plugins {
    id("com.android.application") version "9.1.1" apply false
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
