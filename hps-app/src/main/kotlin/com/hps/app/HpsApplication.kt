package com.hps.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages = ["com.hps"])
@EntityScan(basePackages = ["com.hps.domain"])
@EnableJpaRepositories(basePackages = ["com.hps.persistence"])
class HpsApplication

fun main(args: Array<String>) {
    runApplication<HpsApplication>(*args)
}
