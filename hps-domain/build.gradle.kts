plugins {
    id("hps.kotlin-conventions")
}

dependencies {
    api(project(":hps-common"))
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
}
