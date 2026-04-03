plugins {
    id("hps.spring-jpa-conventions")
}

dependencies {
    implementation(project(":hps-api"))
    implementation(project(":hps-geo"))

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    mainClass.set("com.hps.app.HpsApplicationKt")
}

tasks.named<Jar>("jar") {
    enabled = false
}
