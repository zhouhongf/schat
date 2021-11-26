package com.myworld.schat.service.impl

import com.myworld.schat.common.ApiResult
import com.myworld.schat.common.ResultUtil
import com.myworld.schat.common.UserContextHolder
import com.myworld.schat.common.UtilService
import com.myworld.schat.data.entity.*
import com.myworld.schat.data.modal.Constants
import com.myworld.schat.data.repository.*
import com.myworld.schat.service.BlogService
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*
import javax.servlet.http.HttpServletResponse
import kotlin.collections.HashSet

@Service
class BlogServiceImpl : BlogService {

    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var phoneUserRepository: PhoneUserRepository
    @Autowired
    private lateinit var phoneContactRepository: PhoneContactRepository

    @Autowired
    private lateinit var showPanelPhotoRepository: ShowPanelPhotoRepository
    @Autowired
    private lateinit var showPhotoRepository: ShowPhotoRepository
    @Autowired
    private lateinit var showBlogRepository: ShowBlogRepository


    override fun getShowBlogList(pageSize: Int, pageIndex: Int): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val pageable: Pageable = PageRequest.of(pageIndex, pageSize, Sort.Direction.DESC, "createTime")
        val showBlogsPaged = showBlogRepository.findByUserWid(userWid, pageable) ?: return ResultUtil.failure(-2, "没有数据")
        return ResultUtil.success(num = showBlogsPaged.totalElements, data = showBlogsPaged.content)
    }

    @Throws(IOException::class)
    override fun uploadBlogPanelPhotoBase64(base64: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val base64Bytes = UtilService.base64ToBytes(base64)
        showPanelPhotoRepository.deleteById(userWid)
        val photo = ShowPanelPhoto(id = userWid, fileName = "BASE64-$userWid.jpg", extensionType = "image/jpeg", fileByte = base64Bytes)
        showPanelPhotoRepository.save(photo)
        return ResultUtil.success()
    }

    @Throws(IOException::class)
    override fun getBlogPanelPhotoLocation(idDetail: String, response: HttpServletResponse) {
        val file = showPanelPhotoRepository.findById(idDetail)
        if (file.isPresent) {
            IOUtils.copy(ByteArrayInputStream(file.get().fileByte), response.outputStream)
            response.contentType = file.get().extensionType
        }
    }



    override fun createShowBlog(contactTag: String, content: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val currentTime = Date().time
        val idDetail = Constants.BLOG_SHOW_PREFIX + "-" + userWid + "-" + currentTime

        val showBlog = ShowBlog(id = idDetail, userWid = userWid, content = content, createTime = currentTime)

        // 设置 此朋友圈分享信息，哪些标签下的朋友可见
        val friendWidSet: MutableSet<String> = HashSet()
        friendWidSet.add(userWid)                          // 首先添加自己的wid进去

        val phoneContacts: List<PhoneContact>?
        phoneContacts = if (contactTag == "ALL") {
            phoneContactRepository.findByUserWid(userWid)
        } else {
            phoneContactRepository.findByUserWidAndTag(userWid, contactTag)
        }

        // 如果没有找到phoneContactList, 则该博客仅创建人自己可见
        if (phoneContacts != null && phoneContacts.isNotEmpty()) {
            for (phoneContact in phoneContacts) {
                friendWidSet.add(phoneContact.targetWid!!)
            }
        }
        showBlog.openToWids = friendWidSet

        showBlogRepository.save(showBlog)
        return ResultUtil.success(data = idDetail)
    }

    override fun uploadShowPhotoBase64(blogIdDetail: String, base64s: Array<String>): ApiResult<*>? {
        val optional = showBlogRepository.findById(blogIdDetail)
        if (!optional.isPresent) {
            return ResultUtil.failure(-2, "无此信息，不能上传照片")
        }
        val newNum = base64s.size
        val theNum = showPhotoRepository.countByBlogIdDetail(blogIdDetail)
        if (theNum + newNum > 9) {
            return ResultUtil.failure(-2, "最多只能上传9张照片")
        }

        val showBlog = optional.get()

        val showPhotoSet = showBlog.showPhotos ?: HashSet()
        for (base64 in base64s) {
            val photoIdDetail = saveShowPhoto(blogIdDetail, base64)
            showPhotoSet.add(photoIdDetail)
        }
        showBlog.showPhotos = showPhotoSet

        showBlogRepository.save(showBlog)
        return ResultUtil.success()
    }

    override fun saveShowPhoto(blogIdDetail: String, base64: String): String {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val base64Bytes = UtilService.base64ToBytes(base64)
        val currentTime = Date().time
        val idDetail = Constants.BLOG_SHOW_PHOTO + currentTime + ((Math.random() * 9 + 1) * 1000).toInt()
        val showPhoto = ShowPhoto(id = idDetail, fileName = "BASE64-$currentTime.jpg", extensionType = "image/jpeg", userWid = userWid, blogIdDetail = blogIdDetail, fileByte = base64Bytes, createTime = currentTime)
        showPhotoRepository.save(showPhoto)
        return idDetail
    }

    @Throws(IOException::class)
    override fun getPhotoShowLocation(idDetail: String, response: HttpServletResponse) {
        val file = showPhotoRepository.findById(idDetail)
        if (file.isPresent) {
            IOUtils.copy(ByteArrayInputStream(file.get().fileByte), response.outputStream)
            response.contentType = file.get().extensionType
        }
    }


    override fun getShowLiveList(pageSize: Int, pageIndex: Int): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val pageable: Pageable = PageRequest.of(pageIndex, pageSize, Sort.Direction.DESC, "createTime")
        val showBlogsPaged = showBlogRepository.findByOpenToWidsContaining(userWid, pageable) ?: return ResultUtil.failure(-2, "无数据")
        return ResultUtil.success(num = showBlogsPaged.totalElements, data = showBlogsPaged.content)
    }


    override fun commentOnShowBlog(blogIdDetail: String, idAt: String, comment: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val optional = showBlogRepository.findById(blogIdDetail)
        if (!optional.isPresent) {
            return ResultUtil.failure(-2, "无此博客，不能点评")
        }

        val idAtWords = idAt.split("-")
        val commentAtType = idAtWords[0]
        val commentAtWid = idAtWords[1]
        // 如果被点评的idAt不是"BLOGSHOW"或"BLOGCOMMENT"开头的，则返回 不能点评
        if (commentAtType == Constants.BLOG_SHOW_PREFIX || commentAtType == Constants.BLOG_SHOW_COMMENT) {
            val currentTime = Date().time
            val commentId = Constants.BLOG_SHOW_COMMENT + "-" + userWid + "-" + currentTime
            // 前端可设置如果idAt等于blogIdDetail, 则是针对本博客的评论，而不是针对评论的评论，可不显示对谁评论
            val showBlogComment = ShowBlogComment(id = commentId, idAt = idAt, comment = comment, commenterWid = userWid, commentAtWid = commentAtWid)

            val showBlog = optional.get()
            var showBlogComments: MutableSet<ShowBlogComment>? = showBlog.showBlogComments
            if (showBlogComments.isNullOrEmpty()) {
                showBlogComments = HashSet()
            }

            showBlogComments.add(showBlogComment)
            showBlog.showBlogComments = showBlogComments
            showBlogRepository.save(showBlog)
            return ResultUtil.success(data = commentId)
        } else {
            return ResultUtil.failure(-2, "无此内容，不能点评")
        }
    }





    override fun delShowBlog(blogIdDetail: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val widInBlogIdDetail = blogIdDetail.split("-")[1]
        if (userWid != widInBlogIdDetail) {
            return ResultUtil.failure(-2, "您无权删除该分享")
        }

        showBlogRepository.deleteById(blogIdDetail)
        showPhotoRepository.deleteAllByBlogIdDetail(blogIdDetail)
        return ResultUtil.success()
    }
}
