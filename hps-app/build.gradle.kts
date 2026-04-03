plugins {
    id("hps.spring-jpa-conventions")
}

dependencies {
    implementation(project(":hps-api"))
    implementation(project(":hps-geo"))

    runtimeOnly("org.postgresql:postgresql")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    mainClass.set("com.hps.app.HpsApplicationKt")
}

tasks.named<Jar>("jar") {
    enabled = false
}
