package com.hps.app

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfig::class)
@ActiveProfiles("test")
abstract class BaseE2ETest {

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var mapper: ObjectMapper

    val api: TestApiClient by lazy { TestApiClient(rest, mapper) }
}
