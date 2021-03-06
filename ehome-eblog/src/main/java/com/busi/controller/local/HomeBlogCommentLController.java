package com.busi.controller.local;

import com.alibaba.fastjson.JSONObject;
import com.busi.controller.BaseController;
import com.busi.entity.HomeBlogComment;
import com.busi.entity.ReturnData;
import com.busi.service.HomeBlogCommentService;
import com.busi.utils.Constants;
import com.busi.utils.RedisUtils;
import com.busi.utils.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @program: ehome
 * @description:更新生活圈指定评论回复数相关接口
 * @author: ZHaoJiaJie
 * @create: 2018-11-23 13:13
 */
@RestController
public class HomeBlogCommentLController extends BaseController implements HomeBlogCommentLocalController {

    @Autowired
    RedisUtils redisUtils;

    @Autowired
    private HomeBlogCommentService homeBlogCommentService;

    @Override
    public ReturnData updateCommentNum(@RequestBody HomeBlogComment homeBlogComment) {
        HomeBlogComment comment = homeBlogCommentService.findById(homeBlogComment.getId());
        //更新数据库
        if (comment.getReplyNumber() <= 0) {
            if (homeBlogComment.getReplyNumber() > 0) {
                comment.setReplyNumber(comment.getReplyNumber() + homeBlogComment.getReplyNumber());
            }
        } else {
            if (homeBlogComment.getReplyNumber() != 0) {
                comment.setReplyNumber(comment.getReplyNumber() + homeBlogComment.getReplyNumber());
            }
        }
        int count = homeBlogCommentService.updateCommentNum(comment);
        if (count <= 0) {
            return returnData(StatusCode.CODE_SERVER_ERROR.CODE_VALUE, "更新生活圈评论回复数操作失败", new JSONObject());
        }
        //清除缓存
        List list = null;
        list = redisUtils.getList(Constants.REDIS_KEY_EBLOG_COMMENT + comment.getBlogId(), 0, -1);
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                HomeBlogComment comment2 = (HomeBlogComment) list.get(i);
                if (comment2.getId() == comment.getId()) {
                    redisUtils.removeList(Constants.REDIS_KEY_EBLOG_COMMENT + comment.getBlogId(), 1, comment2);
                    //重新将评论加载到缓存
                    redisUtils.addListLeft(Constants.REDIS_KEY_EBLOG_COMMENT + homeBlogComment.getBlogId(), comment, Constants.USER_TIME_OUT);
                    break;
                }
            }
        }
        return returnData(StatusCode.CODE_SUCCESS.CODE_VALUE, "success", new JSONObject());
    }
}
