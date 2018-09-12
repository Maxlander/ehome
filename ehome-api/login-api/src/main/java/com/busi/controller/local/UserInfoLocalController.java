package com.busi.controller.local;


import com.busi.entity.UserInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户信息相关接口（内部调用）
 * author：SunTianJie
 * create time：2018/6/7 16:02
 */
public interface UserInfoLocalController {

    /***
     * 查询用户信息
     * @param userId
     * @return
     */
    @GetMapping("getUserInfo/{userId}")
    UserInfo getUserInfo(@PathVariable(value="userId") long userId);

}