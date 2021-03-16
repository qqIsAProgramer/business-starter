package com.qyl.utils.component;

import org.springframework.util.DigestUtils;

/**
 * 数据加密工具类
 * @Author: qyl
 * @Date: 2021/2/5 11:22
 */
public class EncryptUtil {

    /**
     * 随机盐
     */
    private static final String SALT = "$R67@EdKd7^%PLw7";

    /**
     * MD5 加密
     * @param origin 原始数据
     * @return 加密数据
     */
    public static String encryptByMD5(String origin) {
        return DigestUtils.md5DigestAsHex((origin + SALT).getBytes());
    }
}
