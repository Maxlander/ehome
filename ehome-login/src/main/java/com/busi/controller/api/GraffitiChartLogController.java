package com.busi.controller.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.busi.controller.BaseController;
import com.busi.entity.GraffitiChartLog;
import com.busi.entity.PageBean;
import com.busi.entity.ReturnData;
import com.busi.entity.UserInfo;
import com.busi.service.GraffitiChartLogService;
import com.busi.service.UserInfoService;
import com.busi.utils.CommonUtils;
import com.busi.utils.Constants;
import com.busi.utils.RedisUtils;
import com.busi.utils.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 涂鸦记录相关接口
 * author：SunTianJie
 * create time：2018/7/31 15:32
 */
@RestController
public class GraffitiChartLogController extends BaseController implements GraffitiChartLogApiController {

    @Autowired
    RedisUtils redisUtils;

    @Autowired
    GraffitiChartLogService graffitiChartLogService;

    @Autowired
    UserInfoService userInfoService;

    /***
     * 新增用户涂鸦头像接口
     * @param graffitiChartLog
     * @return
     */
    @Override
    public ReturnData addGraffitiHead(@Valid @RequestBody GraffitiChartLog graffitiChartLog, BindingResult bindingResult) {
        //验证参数格式是否正确
        if(bindingResult.hasErrors()){
            return returnData(StatusCode.CODE_PARAMETER_ERROR.CODE_VALUE,checkParams(bindingResult),new JSONObject());
        }
        //验证修改人权限
        if(CommonUtils.getMyId()==graffitiChartLog.getMyId()){
            return returnData(StatusCode.CODE_PARAMETER_ERROR.CODE_VALUE,"userId参数有误，自己不能给自己涂鸦",new JSONObject());
        }

        //验证会员信息和次数限制


        //开始修改
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(graffitiChartLog.getMyId());
        userInfo.setGraffitiHead(graffitiChartLog.getGraffitiHead());
        userInfoService.updateUserGraffitiHead(userInfo);
        //获取缓存中的登录信息
        Map<String,Object> userMap = redisUtils.hmget(Constants.REDIS_KEY_USER+userInfo.getUserId());
        if(userMap!=null&&userMap.size()>0){//缓存中存在 才更新 不存在不更新
            //更新缓存 自己修改自己的用户信息 不考虑并发问题
            redisUtils.hset(Constants.REDIS_KEY_USER+userInfo.getUserId(),"graffitiHead",userInfo.getGraffitiHead(),Constants.USER_TIME_OUT);
        }
        //新增涂鸦记录
        graffitiChartLog.setTime(new Date());
        graffitiChartLogService.add(graffitiChartLog);
        return returnData(StatusCode.CODE_SUCCESS.CODE_VALUE,"success",new JSONObject());
    }

    /***
     * 删除重置涂鸦头像接口
     * @param userId
     * @return
     */
    @Override
    public ReturnData deleteGraffitiHead(@PathVariable long userId) {
        //验证修改人权限
        if(CommonUtils.getMyId()!=userId){
            return returnData(StatusCode.CODE_PARAMETER_ERROR.CODE_VALUE,"userId参数有误，您无权限清除和重置该涂鸦",new JSONObject());
        }
        //开始修改
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setGraffitiHead("");//置空
        userInfoService.updateUserGraffitiHead(userInfo);
        //获取缓存中的登录信息
        Map<String,Object> userMap = redisUtils.hmget(Constants.REDIS_KEY_USER+userInfo.getUserId());
        if(userMap!=null&&userMap.size()>0){//缓存中存在 才更新 不存在不更新
            //更新缓存 自己修改自己的用户信息 不考虑并发问题
            redisUtils.hset(Constants.REDIS_KEY_USER+userInfo.getUserId(),"graffitiHead",userInfo.getGraffitiHead(),Constants.USER_TIME_OUT);
        }
        return returnData(StatusCode.CODE_SUCCESS.CODE_VALUE,"success",new JSONObject());
    }

    /**
     * 分页查找涂鸦记录列表接口
     * @param userId   被涂鸦者ID
     * @param page     页码 第几页 起始值1
     * @param count    每页条数
     * @return
     */
    @Override
    public ReturnData findGraffitiChartLogList(@PathVariable long userId,@PathVariable int page,@PathVariable int count) {
        //验证参数
        if(userId<=0||page<0||count<0){
            return returnData(StatusCode.CODE_PARAMETER_ERROR.CODE_VALUE,"参数有误",new JSONObject());
        }
        //开始查询
        PageBean<GraffitiChartLog> pageBean;
        pageBean = graffitiChartLogService.findList(userId,page,count);
        if(pageBean==null){
            return returnData(StatusCode.CODE_SUCCESS.CODE_VALUE,StatusCode.CODE_SUCCESS.CODE_DESC,new JSONArray());
        }
        //更换用户头像和名字信息
        List list = pageBean.getList();
        if(list!=null){
            for(int i=0;i<list.size();i++){
                GraffitiChartLog graffitiChartLog = (GraffitiChartLog) list.get(i);
                Map<String,Object> userMap = redisUtils.hmget(Constants.REDIS_KEY_USER+graffitiChartLog.getUserId() );
                if(userMap==null||userMap.size()<=0){
                    //缓存中没有用户对象信息 查询数据库
                    UserInfo userInfo = userInfoService.findUserById(graffitiChartLog.getUserId());
                    //将用户信息存入缓存中
                    userMap = CommonUtils.objectToMap(userInfo);
                    redisUtils.hmset(Constants.REDIS_KEY_USER+userInfo.getUserId(),userMap,Constants.USER_TIME_OUT);
                }
                Object name = userMap.get("name");
                Object head = userMap.get("head");
                if(name==null||head==null){
                    continue;//数据异常
                }
                graffitiChartLog.setName(name.toString());
                graffitiChartLog.setHead(head.toString());
            }
        }
        return returnData(StatusCode.CODE_SUCCESS.CODE_VALUE,StatusCode.CODE_SUCCESS.CODE_DESC,pageBean);
    }
}
