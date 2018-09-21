package com.busi.dao;

import com.busi.entity.UserAccountSecurity;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

/**
 * 用户账户安全DAO
 * author：SunTianJie
 * create time：2018/6/26 12:22
 */
@Mapper
@Repository
public interface UserAccountSecurityDao {

    /***
     * 新增
     * @param userAccountSecurity
     * @return
     */
    @Insert("insert into UserAccountSecurity(userId,idCard,realName,phone,email,securityQuestion,otherPlatformType,otherPlatformAccount,deviceLock) " +
            "values (#{userId},#{idCard},#{realName},#{phone},#{email},#{securityQuestion},#{otherPlatformType},#{otherPlatformAccount},#{deviceLock})")
    @Options(useGeneratedKeys = true)
    int add(UserAccountSecurity userAccountSecurity);

    /***
     * 根据userId查询
     * @param userId
     * @return
     */
    @Select(("select * from userAccountSecurity where userId=#{userId}"))
    UserAccountSecurity findUserAccountSecurityByUserId(@Param("userId") long userId);

    /***
     * 根据phone查询
     * @param phone
     * @return
     */
    @Select(("select * from userAccountSecurity where phone=#{phone}"))
    UserAccountSecurity findUserAccountSecurityByPhone(@Param("phone") String phone);

    /***
     * 更新
     * @param userAccountSecurity
     * @return
     */
    @Update("<script>" +
            "update userAccountSecurity set"+
            " realName=#{realName}," +
            " idCard=#{idCard}," +
            " phone=#{phone}," +
            " email=#{email}," +
            " securityQuestion=#{securityQuestion}," +
            " deviceLock=#{deviceLock}," +
            " otherPlatformAccount=#{otherPlatformAccount}," +
            " otherPlatformType=#{otherPlatformType}," +
            " userId=#{userId}" +
            " where userId=#{userId}"+
            "</script>")
    int update(UserAccountSecurity userAccountSecurity);

    /***
     * 查询第三方平台账号是否被绑定过
     * @param otherPlatformType
     * @param otherPlatformAccount
     * @return
     */
    @Select(("select * from userAccountSecurity where otherPlatformType=#{otherPlatformType} and otherPlatformAccount=#{otherPlatformAccount}"))
    UserAccountSecurity findUserAccountSecurityByOther(@Param("otherPlatformType") int otherPlatformType,@Param("otherPlatformAccount") String otherPlatformAccount);
}
