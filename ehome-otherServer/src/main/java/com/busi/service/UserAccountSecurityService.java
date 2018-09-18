package com.busi.service;

import com.busi.dao.UserAccountSecurityDao;
import com.busi.entity.UserAccountSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户账户安全Service
 * author：SunTianJie
 * create time：2018/6/26 12:36
 */
@Service
public class UserAccountSecurityService {

    @Autowired
    private UserAccountSecurityDao userAccountSecurityDao;

    /***
     * 新增
     * @param userAccountSecurity
     * @return
     */
    @Transactional(rollbackFor={RuntimeException.class, Exception.class})
    public int addUserAccountSecurity(UserAccountSecurity userAccountSecurity){
        return  userAccountSecurityDao.add(userAccountSecurity);
    }

    /***
     * 更新
     * @param userAccountSecurity
     * @return
     */
    @Transactional(rollbackFor={RuntimeException.class, Exception.class})
    public int updateUserAccountSecurity(UserAccountSecurity userAccountSecurity){
        return  userAccountSecurityDao.update(userAccountSecurity);
    }

    /***
     * 查询
     * @param userId
     * @return
     */
    public UserAccountSecurity findUserAccountSecurity(long userId){
        return  userAccountSecurityDao.findUserAccountSecurity(userId);
    }

}