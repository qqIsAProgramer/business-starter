package com.qyl.service.impl;

import com.qyl.enums.ResponseEnum;
import com.qyl.mapper.UserMapper;
import com.qyl.pojo.PO.TokenPO;
import com.qyl.pojo.User;
import com.qyl.service.UploadService;
import com.qyl.service.UserService;
import com.qyl.utils.ResponseEntity;
import com.qyl.utils.component.VerifyCodeUtil;
import com.qyl.utils.component.PwdEncryptUtil;
import com.qyl.utils.component.TokenUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
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
    private UploadService uploadService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "user:phone:code:";

    @Override
    public ResponseEntity<String> register(User user, String verifyCode, MultipartFile avatar) {
        // 校验验证码
        if (!verifyCode.equals(stringRedisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone()))) {
            return ResponseEntity.error(ResponseEnum.CODE_IS_INCORRECT.getCode(), ResponseEnum.CODE_IS_INCORRECT.getMsg());
        }

        // 通过用户名判断用户是否存在
        if (userMapper.selectByName(user.getUsername()) != null) {
            return ResponseEntity.error(ResponseEnum.USER_EXIST.getCode(), ResponseEnum.USER_EXIST.getMsg());
        }

        try {
            user.setPhone(user.getPhone());
            // 密码加密
            user.setPassword(PwdEncryptUtil.encodeByMD5(user.getPassword()));

            // 存储头像
            String url = uploadService.uploadAvatar(avatar);
            user.setAvatar(url);

            // 设置用户创建时间
            user.setCreateTime(new Date());
            // 写入数据库
            userMapper.insertSelective(user);
            stringRedisTemplate.delete(KEY_PREFIX + user.getPhone());
            // 返回token
            String token = TokenUtil.genToken(user.getPhone());
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.fail();
    }

    @Override
    public ResponseEntity<User> queryUserByName(String username) {
        User user = userMapper.selectByName(username);
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
        String code = VerifyCodeUtil.generateCode(6);
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + phone, code, 5, TimeUnit.MINUTES);
        return ResponseEntity.ok(code);
    }

    @Override
    public ResponseEntity<TokenPO> login(String phone, String password) {
        try {
            User record = new User();
            record.setPhone(phone);
            record.setPassword(PwdEncryptUtil.encodeByMD5(password));
            User user = userMapper.selectOne(record);
            if (user != null) {
                TokenPO tokenPO = new TokenPO(TokenUtil.genToken(phone), user);
                return ResponseEntity.ok(tokenPO);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.error(ResponseEnum.LOGIN_ERROR.getCode(), ResponseEnum.LOGIN_ERROR.getMsg());
    }
}
