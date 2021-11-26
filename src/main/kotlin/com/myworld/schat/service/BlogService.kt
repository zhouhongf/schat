package com.myworld.schat.service

import com.myworld.schat.common.ApiResult
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

interface BlogService {
    fun getShowBlogList(pageSize: Int, pageIndex: Int): ApiResult<*>?
    @Throws(IOException::class)
    fun uploadBlogPanelPhotoBase64(base64: String): ApiResult<*>?
    @Throws(IOException::class)
    fun getBlogPanelPhotoLocation(idDetail: String, response: HttpServletResponse)

    fun createShowBlog(contactTag: String, content: String): ApiResult<*>?
    fun uploadShowPhotoBase64(blogIdDetail: String, base64s: Array<String>): ApiResult<*>?
    fun saveShowPhoto(blogIdDetail: String, base64: String): String?
    @Throws(IOException::class)
    fun getPhotoShowLocation(idDetail: String, response: HttpServletResponse)

    fun commentOnShowBlog(blogIdDetail: String, idAt: String, comment: String): ApiResult<*>?
    fun getShowLiveList(pageSize: Int, pageIndex: Int): ApiResult<*>?

    fun delShowBlog(blogIdDetail: String): ApiResult<*>?
}
