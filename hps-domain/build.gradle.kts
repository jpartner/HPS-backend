plugins {
    id("hps.kotlin-jpa-conventions")
}

dependencies {
    api(project(":hps-common"))
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
}
