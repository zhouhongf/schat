package com.myworld.schat.security.jwt

import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import sun.misc.BASE64Decoder
import java.io.InputStream
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

object TokenPublicKeyGenerator {
    private val log = LogManager.getRootLogger()

    lateinit var publicKey: PublicKey
    init {
        try {
            val resource: Resource = ClassPathResource("public.txt")
            val inputStream: InputStream = resource.inputStream
            val strList = IOUtils.readLines(inputStream, "utf8")
            val theKeyContent = StringBuffer()
            for (keyStr in strList) {
                if (keyStr == "-----BEGIN PUBLIC KEY-----" || keyStr == "-----END PUBLIC KEY-----") {
                    continue
                } else {
                    theKeyContent.append(keyStr)
                }
            }
            log.info("public.txt文件读出来的内容为：{}", theKeyContent.toString())
            val bytes: ByteArray = BASE64Decoder().decodeBuffer(theKeyContent.toString())
            val spec = X509EncodedKeySpec(bytes)
            val factory = KeyFactory.getInstance("RSA")
            publicKey = factory.generatePublic(spec)
            log.info("publicKey已生产完成")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
