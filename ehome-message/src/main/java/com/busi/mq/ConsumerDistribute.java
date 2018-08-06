package com.busi.mq;

import com.alibaba.fastjson.JSONObject;
import com.busi.adapter.MessageAdapter;
import com.busi.service.*;
import com.busi.utils.CommonUtils;
import com.busi.utils.Constants;
import com.busi.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
/**
 * 消息监听和分发业务
 * author：SunTianJie
 * create time：2018/5/29 14:24
 */
@Slf4j
@Component
public class ConsumerDistribute {

    private MessageAdapter messageAdapter;//主适配器

    @Autowired
    RedisUtils redisUtils;
    @Autowired
    private EmailService emailService;//邮件业务
    @Autowired
    private PhoneService phoneService;//手机业务
    @Autowired
    private UserService userService;//用户业务
    @Autowired
    private LoginStatusInfoService loginStatusInfoService;//用户业务
    @Autowired
    private VisitViewService visitViewService;//访问量业务
    @Autowired
    private ImageDeleteLogService imageDeleteLogService;

    /***
     * 监听消息
     * 消息格式：
     * {
     * 	"header": {
     * 		"interfaceType": 1
     *        },
     * 	"content": {
     * 		"email": "test@qq.com",
     * 		"phone": "15901213694",
     * 		"userId": "12345"
     *    }
     * }
     * interfaceType 0:表示发送手机短信  1:表示发送邮件  2:表示新用户注册转发 3:表示用户登录时同步登录信息
     *               4:表示用户访问量信息同步 5:表示同步图片删除 6:同步任务系统 ...
     * content 中的内容，根据具体业务自定义
     * @param json
     * @param textMessage
     * @param session
     */
    @JmsListener(destination = Constants.MSG_REGISTER_MQ)
    public void distribute(String json, TextMessage textMessage, Session session) {
        try {
            log.info("消息服务平台接收到消息转发请求，消息内容:" + json);
            if (CommonUtils.checkFull(json)) {
                log.info("消息服务平台操作失败，请求参数有误:" + json);
                return;
            }
            //解决消息幂等性问题，即重复问题 此处的消息ID需要存入缓存中
            String msgId = textMessage.getJMSMessageID();
            String msgIdKey=Constants.MSG_ID +msgId;//组合成缓存中的key
            //从缓存中查找 是否存在对应的消息ID 防止幂等性问题
            boolean isExistStatus = redisUtils.isExistKey(msgIdKey);
            if(isExistStatus){//存在，则表示当前操作为重复操作 需要手动提交
                textMessage.acknowledge();
                return;
            }
            //将消息ID存入缓存 10分钟有效期
            redisUtils.set(msgIdKey,"1",Constants.MSG_TIME_OUT_MINUTE_10);

            //开始执行相关业务
            JSONObject jsonObject = JSONObject.parseObject(json);
            JSONObject header = jsonObject.getJSONObject("header");
            String interfaceType = header.getString("interfaceType");
            switch (interfaceType) {
                case "0":// 发送手机短信
                    messageAdapter = phoneService;
                    break;
                case "1":// 发送邮件
                    messageAdapter = emailService;
                    break;
                case "2"://新用户注册转发
                    messageAdapter = userService;
                    break;
                case "3"://表示用户登录时同步登录信息
                    messageAdapter = loginStatusInfoService;
                    break;
                case "4"://表示用户访问量信息同步
                    messageAdapter = visitViewService;
                    break;
                case "5"://表示同步图片删除
                    messageAdapter = imageDeleteLogService;
                    break;
                default://异常
                    log.info("消息服务平台操作失败，请求参数有误interfaceType:" + interfaceType);
                    return;
            }
            if (messageAdapter == null) {
                log.info("消息服务平台操作失败，消息平台出现异常！");
                return;
            }
            JSONObject body = jsonObject.getJSONObject("content");
            //具体业务调用
            messageAdapter.sendMsg(body);
            //手动提交
            textMessage.acknowledge();
            //此处也可以手动清理缓存中消息ID，因为上面设置失效时间会自动清理，所以此处暂不处理
            log.info("消息服务平台处理请求操作成功，提交本次操作的队列事务！");
        }catch (Exception e){
            e.printStackTrace();
            log.info("消息服务平台操作失败，启动重试机制");
            try {
                session.recover();//重试机制
            } catch (JMSException e1) {
                e1.printStackTrace();
                log.info("消息服务平台启动重试机制异常！");
            }
        }
    }
}
