package com.myworld.schat.common

object ResultUtil {

    @JvmStatic
    fun success(code: Int = ApiResultEnum.SUCCESS.code, msg: String = ApiResultEnum.SUCCESS.msg, num: Long? = null, data: Any? = null): ApiResult<Any?> {
        return ApiResult(code = code, msg = msg, num = num, data = data)
    }

    @JvmStatic
    fun failure(code: Int = ApiResultEnum.FAILURE.code, msg: String = ApiResultEnum.FAILURE.msg): ApiResult<Any?> {
        return ApiResult(code = code, msg = msg, num = null, data = null)
    }

    @JvmStatic
    fun update(code: Int = ApiResultEnum.UPDATE.code, msg: String = ApiResultEnum.UPDATE.msg, num: Long? = null, data: Any? = null): ApiResult<Any?> {
        return ApiResult(code = code, msg = msg, num = num, data = data)
    }
}
