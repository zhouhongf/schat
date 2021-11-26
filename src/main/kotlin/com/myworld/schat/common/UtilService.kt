package com.myworld.schat.common

import org.apache.commons.codec.binary.Base64
import org.apache.logging.log4j.LogManager
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest


object UtilService {

    private val log = LogManager.getRootLogger()

    val yearNow: Int
        get() {
            val timestamp = Timestamp(Date().time)
            return getCustomDateFromTimestamp(timestamp, 0, 4)
        }

    val yearMonthNow: Int
        get() {
            val timestamp = Timestamp(Date().time)
            return getCustomDateFromTimestamp(timestamp, 0, 6)
        }

    val yearMonthDayNow: Int
        get() {
            val timestamp = Timestamp(Date().time)
            return getCustomDateFromTimestamp(timestamp, 0, 8)
        }

    @JvmStatic
    fun getCustomDateFromTimestamp(timestamp: Timestamp, start: Int, end: Int): Int {
        val timeNow = SimpleDateFormat("yyyyMMddHHmmss").format(timestamp)
        return timeNow.substring(start, end).toInt()
    }

    val monthNow: Int
        get() {
            val timestamp = Timestamp(Date().time)
            return getCustomDateFromTimestamp(timestamp, 4, 6)
        }

    val lastMonth: Int
        get() {
            val monthNow = monthNow
            val lastMonth: Int
            lastMonth = if (monthNow == 1) {
                12
            } else {
                monthNow - 1
            }
            return lastMonth
        }

    fun getPreviousTwelveNumber(number: Int): Int {
        return when (number) {
            1 -> 12
            2 -> 1
            3 -> 2
            4 -> 3
            5 -> 4
            6 -> 5
            7 -> 6
            8 -> 7
            9 -> 8
            10 -> 9
            11 -> 10
            12 -> 11
            else -> 0
        }
    }

    @JvmStatic
    fun getIpAddress(request: HttpServletRequest): String {
        var ip = request.getHeader("x-forwarded-for")
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("Proxy-Client-IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("HTTP_CLIENT_IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.remoteAddr
        }
        log.info("ip地址为：$ip")
        return ip
    }

    @JvmStatic
    fun shortCityName(cityName: String): String { // 如果城市中包含 市 区 县， 则去掉市 区 县 的后缀
        val theCityName: String
        val pattern = Pattern.compile("([\\u4e00-\\u9fa5]{2,})[市|区|县]")
        val m = pattern.matcher(cityName)
        theCityName = if (m.find()) {
            m.group(1)
        } else {
            cityName
        }
        return theCityName
    }

    @JvmStatic
    fun isNumeric(str: String): Boolean {
        val pattern = Pattern.compile("^(-|\\+)?\\d+(\\.\\d+)?$")
        val isNum = pattern.matcher(str)
        return isNum.matches()
    }

    @JvmStatic
    fun isInteger(str: String): Boolean {
        val pattern = Pattern.compile("^[0-9]+$")
        val isInt = pattern.matcher(str)
        return isInt.matches()
    }

    @JvmStatic
    fun isLetter(str: String): Boolean {
        val pattern = Pattern.compile("^[A-Za-z]+$")
        val isLet = pattern.matcher(str)
        return isLet.matches()
    }

    @JvmStatic
    fun isZhCN(str: String): Boolean {
        val pattern = Pattern.compile("^[\\u4e00-\\u9fa5]+$")
        val isZh = pattern.matcher(str)
        return isZh.matches()
    }

    @JvmStatic
    fun isWid(str: String): Boolean {
        val pattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{6,18}$")
        val isWid = pattern.matcher(str)
        return isWid.matches()
    }

    /**
     * 利用正则表达式，获取tinymce中的图片链接地址
     */
    @JvmStatic
    fun getImgStr(htmlStr: String): Set<String> {
        val pics: MutableSet<String> = HashSet()
        val p_image: Pattern
        val m_image: Matcher
        val regEx_img = "<img.*src\\s*=\\s*(.*?)[^>]*?>"
        p_image = Pattern.compile(regEx_img, Pattern.CASE_INSENSITIVE)
        m_image = p_image.matcher(htmlStr)
        while (m_image.find()) {                                                                // 得到<img />数据
            val img = m_image.group()
            val m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(img)     // 匹配<img>中的src数据
            while (m.find()) {
                pics.add(m.group(1))
            }
        }
        log.info("【提取出来的图片URL是】$pics")
        return pics
    }

    @JvmStatic
    fun base64ToBytes(base64: String): ByteArray {
        var base64Copy = base64
        base64Copy = base64Copy.replace("data:image/jpeg;base64,".toRegex(), "")
        return Base64.decodeBase64(base64Copy)
    }

    @JvmStatic
    fun bytesToBase64(bytes: ByteArray): String {
        return Base64.encodeBase64String(bytes)
    }


}
