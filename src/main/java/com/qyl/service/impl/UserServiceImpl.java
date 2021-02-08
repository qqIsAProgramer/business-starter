package com.qyl.service.impl;

import com.qyl.enums.ResponseEnum;
import com.qyl.mapper.UserMapper;
import com.qyl.pojo.User;
import com.qyl.service.UserService;
import com.qyl.utils.ResponseEntity;
import com.qyl.utils.component.CookieUtil;
import com.qyl.utils.component.GenerateCodeUtil;
import com.qyl.utils.component.PwdEncoderUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @Author: qyl
 * @Date: 2020/12/7 9:51
 */
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "user:code:phone:";

    private static final String COOKIE_NAME = "USER_PHONE";

    @Override
    public ResponseEntity<Void> register(User user, HttpServletRequest request) {
        // 通过用户名判断用户是否存在
        if (userMapper.selectByName(user.getUsername()) != null) {
            return ResponseEntity.error(ResponseEnum.USER_EXIST.getCode(), ResponseEnum.USER_EXIST.getMsg());
        }
        try {
            // 通过cookie获得手机号
            user.setPhone(CookieUtil.getCookie(request, COOKIE_NAME));
            // 密码加密
            user.setPassword(PwdEncoderUtil.encodeByMD5(user.getPassword()));
            user.setCreated(new Date());
            // 写入数据库
            userMapper.insertSelective(user);
            return ResponseEntity.ok();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.fail();
    }

    @Override
    public ResponseEntity<User> queryUserByName(String username) {
        User record = new User();
        record.setUsername(username);
        User user = userMapper.selectOne(record);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.fail();
    }

    @Override
    public ResponseEntity<String> sendVerificationCode(String phone) {
        // 通过手机号判断用户是否存在
        if (userMapper.selectByPhone(phone) != null) {
            return ResponseEntity.error(ResponseEnum.USER_EXIST.getCode(), ResponseEnum.USER_EXIST.getMsg());
        }
        String code = GenerateCodeUtil.generateCode(6);
        redisTemplate.opsForValue().set(KEY_PREFIX + phone, code, 5, TimeUnit.MINUTES);
        return ResponseEntity.ok(code);
    }

    @Override
    public ResponseEntity<Void> checkVerificationCode(HttpServletResponse response, String phone, String verificationCode) {
        if (verificationCode.equals(redisTemplate.opsForValue().get(KEY_PREFIX + phone))) {
            redisTemplate.delete(KEY_PREFIX + phone);
            CookieUtil.setCookie(response, COOKIE_NAME, phone, 5 * 60);
            return ResponseEntity.ok();
        }
        return ResponseEntity.error(ResponseEnum.CODE_IS_INCORRECT.getCode(), ResponseEnum.CODE_IS_INCORRECT.getMsg());
    }

    @Override
    public ResponseEntity<User> login(String phone, String password) {
        try {
            User record = new User();
            record.setPhone(phone);
            record.setPassword(PwdEncoderUtil.encodeByMD5(password));
            User user = userMapper.selectOne(record);
            if (user != null) {
                return ResponseEntity.ok(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.error(ResponseEnum.LOGIN_ERROR.getCode(), ResponseEnum.LOGIN_ERROR.getMsg());
    }
}
