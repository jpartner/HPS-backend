plugins {
    id("hps.kotlin-jpa-conventions")
}

dependencies {
    api(project(":hps-common"))
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    compileOnly("org.hibernate.orm:hibernate-core:6.6.11.Final")
}
