plugins {
    id("hps.spring-conventions")
}

dependencies {
    implementation(project(":hps-persistence"))
    implementation(project(":hps-search"))
    implementation(project(":hps-geo"))
    implementation(project(":hps-messaging"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.sksamuel.scrimage:scrimage-core:4.2.0")
    implementation("com.sksamuel.scrimage:scrimage-webp:4.2.0")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
}
