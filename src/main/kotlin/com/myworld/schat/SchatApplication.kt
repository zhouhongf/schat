package com.myworld.schat

import org.springframework.boot.SpringApplication
import org.springframework.cloud.client.SpringCloudApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling


@EnableAsync
@EnableScheduling
@EnableFeignClients
@SpringCloudApplication
@EnableMongoRepositories(basePackages = ["com.**.repository"])
open class SchatApplication


fun main(args: Array<String>) {
    SpringApplication.run(SchatApplication::class.java, *args)
}
