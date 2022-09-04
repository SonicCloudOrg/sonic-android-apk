package org.cloud.sonic.android.lib.socketmanager.utils

/**
 * 输入检查
 * 正则检查String是否符合某些格式
 */
object StringValidationUtils {
    const val RegexIP =
        "^(25[0-5]|2[0-4][0-9]|1{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9])\\.(25[0-5]|2[0-4][0-9]|1{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|1{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|1{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])$"
    const val RegexPort =
        "^6553[0-5]|655[0-2][0-9]|65[0-4][0-9]{2}|6[0-4][0-9]{3}|[1-5][0-9]{4}|[1-9][0-9]{0,3}$"
    const val RegexAllChinese = "^[\\u4e00-\\u9fa5]*$"
    const val RegexPhoneNumber =
        "^(((13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8})|((\\d{3,4}-)?\\d{7,8}(-\\d{1,4})?)$"
    const val RegexEmail = "w+([-+.]w+)*@w+([-.]w+)*.w+([-.]w+)*"
    fun validateRegex(string: String?, regex: String): Boolean {
        return string?.matches(Regex(regex)) ?: false
    }
}