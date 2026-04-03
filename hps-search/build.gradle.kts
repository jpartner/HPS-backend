plugins {
    id("hps.spring-conventions")
}

dependencies {
    api(project(":hps-domain"))
    implementation("org.typesense:typesense-java:1.3.0")
}
