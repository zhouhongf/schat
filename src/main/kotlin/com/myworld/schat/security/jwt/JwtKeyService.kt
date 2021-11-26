package com.myworld.schat.security.jwt


import org.apache.commons.codec.binary.Base64
import org.apache.logging.log4j.LogManager
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec

object JwtKeyService {
    private val log = LogManager.getRootLogger()
    //密钥 (需要前端和后端保持一致)
    private const val CKEY = "myworldmypasskey"
    //算法
    private const val ALGORITHMSTR = "AES/ECB/PKCS5Padding"

    @JvmStatic
    fun tokenToUserWid(token: String): String? { //使用公钥解析token
        val publicKey = TokenPublicKeyGenerator.publicKey
        val map = JwtUtil.parseToken(token, publicKey)
        val userWid = map[JwtUtil.HEADER_WID] as String?
        return if (userWid != null) userWid else null
    }

    /**
    @JvmStatic
    fun decodeUserIdDetail(value: String): String? {
        val valueIn = value.replace(" ", "+")
        val valueOut = aesDecrypt(valueIn)
        log.info("【解密出来的idDetail是: {}】", valueOut)
        if (valueOut == null) {
            return null
        }

        val thePrefix = valueOut.substring(0, 6)
        val theBody = valueOut.substring(6)
        val one = theBody.substring(0, 3)
        val two = theBody.substring(4, 7)
        val three = theBody.substring(8, 11)
        val four = theBody.substring(12, 15)
        val five = theBody.substring(16, 19)
        val six = theBody.substring(20, 22)
        val userIdDetailReal = thePrefix + one + two + three + four + five + six
        log.info("去掉盐后的userIdDetail是：{}", userIdDetailReal)
        return userIdDetailReal

    }
    */

    /**
     * 将base 64 code AES解密
     * @param encryptStr 待解密的base 64 code
     * @param decryptKey 解密密钥
     * @return 解密后的string
     */
    @Throws(Exception::class)
    fun aesDecrypt(encryptStr: String): String? {
        return aesDecryptByBytes(base64Decode(encryptStr), CKEY)
    }

    /**
     * base 64 decode
     * @param base64Code 待解码的base 64 code
     * @return 解码后的byte[]
     */
    @Throws(Exception::class)
    fun base64Decode(base64Code: String): ByteArray? {
        return Base64.decodeBase64(base64Code)
    }

    /**
     * AES解密
     * @param encryptBytes 待解密的byte[]
     * @param decryptKey 解密密钥
     * @return 解密后的String
     */
    @Throws(Exception::class)
    fun aesDecryptByBytes(encryptBytes: ByteArray?, decryptKey: String): String {
        val kgen = KeyGenerator.getInstance("AES")
        kgen.init(128)
        val cipher = Cipher.getInstance(ALGORITHMSTR)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(decryptKey.toByteArray(), "AES"))
        val decryptBytes = cipher.doFinal(encryptBytes)
        return String(decryptBytes)
    }
}
