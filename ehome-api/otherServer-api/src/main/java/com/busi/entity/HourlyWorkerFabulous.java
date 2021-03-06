package com.busi.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import java.util.Date;

/**
 * @program: 小时工点赞
 * @author: ZHaoJiaJie
 * @create: 2019-1-7 15:23:55
 */
@Setter
@Getter
public class HourlyWorkerFabulous {

    private long id;                //主键ID

    @Min(value = 1, message = "myId参数有误")
    private long myId;            //主动点赞用户ID

    @Min(value = 1, message = "userId参数有误")
    private long userId;            //被点赞用户ID

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date time;            //点赞时间

    private int status;                //0正常 1删除

    private long typeId;        //工作类型ID

}
