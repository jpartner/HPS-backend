plugins {
    id("hps.spring-jpa-conventions")
}

dependencies {
    api(project(":hps-persistence"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}
