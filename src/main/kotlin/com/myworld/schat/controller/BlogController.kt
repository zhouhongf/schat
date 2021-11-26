package com.myworld.schat.controller

import com.myworld.schat.common.ApiResult
import com.myworld.schat.service.BlogService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
class BlogController {

    @Autowired
    private lateinit var blogService: BlogService

    @GetMapping("/getShowBlogList")
    fun getShowBlogList(@RequestParam pageSize: Int, @RequestParam pageIndex: Int): ApiResult<*>? {
        return blogService.getShowBlogList(pageSize, pageIndex)
    }

    @PostMapping("/uploadBlogPanelPhotoBase64")
    @Throws(IOException::class)
    fun uploadBlogPanelPhotoBase64(@RequestBody base64: String): ApiResult<*>? {
        return blogService.uploadBlogPanelPhotoBase64(base64)
    }

    @GetMapping(value = ["/getBlogPanelPhotoLocation/{idDetail}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    @Throws(IOException::class)
    fun getBlogPanelPhotoLocation(@PathVariable idDetail: String, response: HttpServletResponse) {
        blogService.getBlogPanelPhotoLocation(idDetail, response)
    }

    @PostMapping("/createShowBlog")
    fun createShowBlog(@RequestParam contactTag: String, @RequestBody content: String): ApiResult<*>? {
        return blogService.createShowBlog(contactTag, content)
    }

    @PostMapping("/uploadShowPhotoBase64")
    fun uploadShowPhotoBase64(@RequestParam blogIdDetail: String, @RequestBody base64s: Array<String>): ApiResult<*>? {
        return blogService.uploadShowPhotoBase64(blogIdDetail, base64s)
    }

    @PostMapping("/commentOnShowBlog")
    fun commentOnShowBlog(@RequestParam blogIdDetail: String, @RequestParam idAt: String, @RequestBody comment: String): ApiResult<*>? {
        return blogService.commentOnShowBlog(blogIdDetail, idAt, comment)
    }

    @GetMapping("/getShowLiveList")
    fun getShowLiveList(@RequestParam pageSize: Int, @RequestParam pageIndex: Int): ApiResult<*>? {
        return blogService.getShowLiveList(pageSize, pageIndex)
    }

    @GetMapping(value = ["/getPhotoShowLocation/{idDetail}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    @Throws(IOException::class)
    fun getPhotoShowLocation(@PathVariable idDetail: String, response: HttpServletResponse) {
        blogService.getPhotoShowLocation(idDetail, response)
    }

    @GetMapping("/delShowBlog")
    fun delShowBlog(@RequestParam blogIdDetail: String): ApiResult<*>? {
        return blogService.delShowBlog(blogIdDetail)
    }

}
