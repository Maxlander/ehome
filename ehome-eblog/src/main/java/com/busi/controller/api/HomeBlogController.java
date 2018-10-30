package com.busi.controller.api;

import com.alibaba.fastjson.JSONObject;
import com.busi.controller.BaseController;
import com.busi.entity.HomeBlog;
import com.busi.entity.ReturnData;
import com.busi.entity.UserInfo;
import com.busi.service.HomeBlogService;
import com.busi.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 生活圈相关接口
 * author：SunTianJie
 * create time：2018/10/23 10:35
 */
@RestController
public class HomeBlogController extends BaseController implements HomeBlogApiController {

    @Autowired
    private HomeBlogService homeBlogService;

    @Autowired
    private UserInfoUtils userInfoUtils;

    @Autowired
    RedisUtils redisUtils;

    @Autowired
    private MqUtils mqUtils;

    /***
     * 生活圈发布接口
     * @param homeBlog
     * @return
     */
    @Override
    public ReturnData addBlog(@Valid @RequestBody HomeBlog homeBlog, BindingResult bindingResult) {
        //验证参数
        if(bindingResult.hasErrors()){
            return returnData(StatusCode.CODE_PARAMETER_ERROR.CODE_VALUE,checkParams(bindingResult),new JSONObject());
        }
        //验证发布人权限
        if(CommonUtils.getMyId()!=homeBlog.getUserId()){
            return returnData(StatusCode.CODE_PARAMETER_ERROR.CODE_VALUE,"参数有误，当前用户["+CommonUtils.getMyId()+"]无权限以用户["+homeBlog.getUserId()+"]的身份发布生活圈",new JSONObject());
        }
        //根据发布类型 判断部分参数格式 发布博文类型：0纯文 1图片 2视频 3音频
        if(homeBlog.getSendType()==0){
            if(CommonUtils.checkFull(homeBlog.getContent())){
                return returnData(StatusCode.CODE_PARAMETER_ERROR.CODE_VALUE,"参数有误，content不能为空",new JSONObject());
            }
        }
        if(homeBlog.getSendType()==1){
            if(CommonUtils.checkFull(homeBlog.getImgUrl())){
                return returnData(StatusCode.CODE_PARAMETER_ERROR.CODE_VALUE,"参数有误，imgUrl不能为空",new JSONObject());
            }
        }
        if(homeBlog.getSendType()==2){
            if(CommonUtils.checkFull(homeBlog.getVideoUrl())||CommonUtils.checkFull(homeBlog.getVideoCoverUrl())){
                return returnData(StatusCode.CODE_PARAMETER_ERROR.CODE_VALUE,"参数有误，videoUrl和videoCoverUrl不能为空",new JSONObject());
            }
        }
        if(homeBlog.getSendType()==3){
            if(CommonUtils.checkFull(homeBlog.getAudioUrl())){
                return returnData(StatusCode.CODE_PARAMETER_ERROR.CODE_VALUE,"参数有误，audioUrl不能为空",new JSONObject());
            }
        }
        //处理特殊字符
        String title = homeBlog.getTitle();
        if(!CommonUtils.checkFull(title)){
            title = title.replaceAll("\"","&quot;");
            title = title.replaceAll("&","&amp;");
            title = title.replaceAll(" ","&nbsp;");
            title = title.replaceAll("<","&lt;");
            title = title.replaceAll(">","&gt;");
            homeBlog.setTitle(title);
        }
        String content = homeBlog.getContent();
        if(!CommonUtils.checkFull(content)){
            //String content = "<p>&8228_139&发读&88_6&</p><p>&88_2&发奋读书发奋读&23_6&</p>";
            int maxlen = 140;
            //表情匹配
            String rep ="";
            boolean flag = false;
            List<String> list = new ArrayList<String>();
            String [] con = content.split("</p>");
            for (int i = 0; i < con.length; i++) {
                if(maxlen>0){
                    String [] newCon = con[i].split("<p>");
                    //处理特殊没有<p>情况
                    String userall ="";
                    if(newCon!=null &&newCon.length>0){
                        if(newCon.length<=1){
                            userall = newCon[0];
                        }else{
                            userall = newCon[1];
                        }
                    }
                    //开头P
                    list.add("<p>");
                    //定义截取140字符长度
                    for (int j = 0; j < userall.length(); j++) {
                        if(maxlen<=0){
                            break;
                        }
                        if("&".equals(userall.charAt(j)+"") || (flag && ";".equals(userall.charAt(j)+""))){
                            if(flag){
                                flag= false;
                                rep+=userall.charAt(j);
                                if(rep.indexOf("_")!=-1){
                                    //列如：&222_8&
                                    //拆分 用户ID 和 用户Id对应名称长度
                                    String [] s = rep.split("_");
                                    rep = s[0]+"&";
                                    //8&
                                    String [] c = s[1].split("&");
                                    //减去名称长度
                                    maxlen -= Integer.valueOf(c[0]);
                                }else if(rep.indexOf(";")!=-1){
                                    //列如：&amp;
                                    maxlen --;
                                }
                                //名字长度计算后超maxlen 舍去
                                if(maxlen<=0){
                                    continue;
                                }
                                list.add(rep+"");
                                rep="";
                                continue;
                            }else{
                                flag= true;
                            }
                        }
                        if(flag){
                            rep+=userall.charAt(j);
                        }else{
                            list.add(userall.charAt(j)+"");
                            maxlen --;
                        }
                    }
                    //结尾P
                    list.add("</p>");
                }
            }
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i));
            }
            homeBlog.setContentTxt(sb.toString());	//截取基本内容
            //删除内容中的ID_字符长度数字
            if(content.indexOf("&")!=-1){
                String regex = "&(\\d+_\\d+)&";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(content);
                StringBuffer sb2 = new StringBuffer();
                while (matcher.find()) {
                    String uid = matcher.group(1).split("_")[0]; //&888_3&
                    matcher.appendReplacement(sb2, "&"+uid+"&");
                }
                matcher.appendTail(sb2);
                content = sb2.toString();
            }
            homeBlog.setContent(content);//截取全部内容
        }
        //开始新增
        homeBlog.setTime(new Date());
        homeBlogService.add(homeBlog);
        //添加足迹
        String t = homeBlog.getTitle();
        if(CommonUtils.checkFull(t)){
            t = homeBlog.getContentTxt();
        }
        mqUtils.sendFootmarkMQ(homeBlog.getUserId(), t, homeBlog.getImgUrl(), null, null, homeBlog.getId()+"", 2);
        //添加任务
        mqUtils.sendTaskMQ(homeBlog.getUserId(), 1, 1);
        return returnData(StatusCode.CODE_SUCCESS.CODE_VALUE,"success",new JSONObject());
    }

    /***
     * 根据生活圈ID查询生活圈详情接口
     * @param userId 被查询用户ID
     * @param blogId 被查询生活圈ID
     * @return
     */
    @Override
    public ReturnData findBlogInfo(@PathVariable long userId,@PathVariable long blogId) {
        Map<String,Object> blogInfoMap = redisUtils.hmget(Constants.REDIS_KEY_EBLOG+userId);
        if(blogInfoMap==null||blogInfoMap.size()<=0){
            HomeBlog homeBlog = homeBlogService.findBlogInfo(blogId);
            if(homeBlog!=null){
                //放到缓存中
                blogInfoMap = CommonUtils.objectToMap(homeBlog);
                redisUtils.hmset(Constants.REDIS_KEY_USER_ACCOUNT_SECURITY+userId,blogInfoMap,Constants.USER_TIME_OUT);
            }
        }
        HomeBlog homeBlog = (HomeBlog) CommonUtils.mapToObject(blogInfoMap,HomeBlog.class);
        if(homeBlog==null){
            return returnData(StatusCode.CODE_SUCCESS.CODE_VALUE, "success", new JSONObject());
        }
        if(CommonUtils.getMyId()!=userId){//排除自己查看自己
            if(homeBlog.getClassify()==1){//私密
                return returnData(StatusCode.CODE_BLOG_NOT_ACCESS.CODE_VALUE, "您无权限查看该条生活圈", blogInfoMap);
            }
            if(homeBlog.getClassify()==2){//2给谁看
                //判断自己是否在 给谁看 的列表中
                homeBlog.getAccessId();

                return returnData(StatusCode.CODE_BLOG_NOT_ACCESS.CODE_VALUE, "您无权限查看该条生活圈", blogInfoMap);
            }
            if(homeBlog.getClassify()==3){//3不给谁看
                //判断自己是否在 不给谁看 的列表中
                homeBlog.getAccessId();

                return returnData(StatusCode.CODE_BLOG_NOT_ACCESS.CODE_VALUE, "您无权限查看该条生活圈", blogInfoMap);
            }
        }
        //设置用户信息
        UserInfo userInfo = userInfoUtils.getUserInfo(userId);
        if(userInfo!=null){
            homeBlog.setUserName(userInfo.getName());
            homeBlog.setUserHead(userInfo.getHead());
            homeBlog.setProTypeId(userInfo.getProType());
            homeBlog.setHouseNumber(userInfo.getHouseNumber());
        }
        //设置是否喜欢过

        //检测当前登录用户是否有权限查看
        return returnData(StatusCode.CODE_SUCCESS.CODE_VALUE, "success", homeBlog);
    }

    /***
     * 删除指定生活圈接口
     * @param userId 生活圈发布者用户ID
     * @param blogId 将要被删除的生活圈
     * @return
     */
    @Override
    public ReturnData delBlog(@PathVariable long userId,@PathVariable long blogId) {
        //判断操作人权限
        if(CommonUtils.getMyId()!=userId){
            return returnData(StatusCode.CODE_PARAMETER_ERROR.CODE_VALUE,"参数有误，当前用户["+CommonUtils.getMyId()+"]无权限操作用户["+userId+"]的生活圈",new JSONObject());
        }
        //查询该条生活圈信息
        Map<String,Object> blogInfoMap = redisUtils.hmget(Constants.REDIS_KEY_EBLOG+userId);
        if(blogInfoMap==null||blogInfoMap.size()<=0){
            HomeBlog homeBlog = homeBlogService.findBlogInfo(blogId);
            if(homeBlog!=null){
                blogInfoMap = CommonUtils.objectToMap(homeBlog);
            }
        }
        HomeBlog hb = (HomeBlog) CommonUtils.mapToObject(blogInfoMap,HomeBlog.class);
        if(hb==null){//不存在直接返回成功
            return returnData(StatusCode.CODE_SUCCESS.CODE_VALUE, "success", new JSONObject());
        }
        //开始更新删除状态
        homeBlogService.delBlog(blogId,userId);
        //判断是否为生活秀首页推荐数据
        if(hb.getSendType()==2&&hb.getLikeCount()>=Constants.EBLOG_LIKE_COUNT){
            //更新生活秀首页推荐列表
            List list = null;
            list = redisUtils.getList(Constants.REDIS_KEY_EBLOGLIST, 0, 1001);
            for (int j = 0; j < list.size(); j++) {
                HomeBlog homeBlog = (HomeBlog) list.get(j);
                if (homeBlog.getUserId() == userId && homeBlog.getId() == blogId) {
                    redisUtils.removeList(Constants.REDIS_KEY_EBLOGLIST, 1, homeBlog);
                }
            }
        }
        //清除缓存中的信息
        redisUtils.expire(Constants.REDIS_KEY_EBLOG + userId, 0);
        return returnData(StatusCode.CODE_SUCCESS.CODE_VALUE, "success", new JSONObject());
    }

    /***
     * 条件查询生活圈接口（查询所有类型的生活圈）
     * @param userId     被查询用户ID 默认0查询所有
     * @param searchType 查询类型 0查看朋友圈 1查看关注 2查看兴趣话题 3查询指定用户
     * @param tags       被查询兴趣标签ID组合，逗号分隔例如：1,2,3 仅当searchType=2 时有效 默认传null
     * @param page       页码 第几页 起始值1
     * @param count      每页条数
     * @return
     */
    @Override
    public ReturnData findBlogList(@PathVariable long userId,@PathVariable int searchType,
                                   @PathVariable String tags,@PathVariable int page,@PathVariable int count) {
        return null;
    }

    /***
     * 条件查询生活秀接口（只查询生活秀内容）
     * @param userId     被查询用户ID 默认0查询所有
     * @param searchType 查询类型 0查询首页推荐 1查同城 2查看朋友 3查询关注 4查询兴趣标签 5查询指定用户
     * @param tags       被查询兴趣标签ID组合，逗号分隔例如：1,2,3  仅当searchType=4 时有效 默认传null
     * @param page       页码 第几页 起始值1
     * @param count      每页条数
     * @return
     */
    @Override
    public ReturnData findBlogVideoList(@PathVariable long userId,@PathVariable int searchType,
                                        @PathVariable String tags,@PathVariable int page,@PathVariable int count) {
        return null;
    }

    public static boolean ckcometnP(String str){

        if(!CommonUtils.checkFull(str)){

            String regex = "<p>(.*?)</p>";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(str);

            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                matcher.appendReplacement(sb, "");
            }
            matcher.appendTail(sb);

            if(CommonUtils.checkFull(sb.toString())){
                return true;
            }
            return false;
        }
        return true;
    }

}