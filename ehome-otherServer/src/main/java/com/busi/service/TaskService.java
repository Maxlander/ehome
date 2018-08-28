package com.busi.service;

import com.busi.dao.TaskDao;
import com.busi.entity.Task;
import com.busi.entity.PageBean;
import com.busi.entity.TaskList;
import com.busi.utils.PageUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * @program: 任务
 * @author: ZHaoJiaJie
 * @create: 2018-08-15 17:49
 */
@Service
public class TaskService {

    @Autowired
    private TaskDao taskDao;
    /***
     * 新增
     * @param task
     * @return
     */
    @Transactional(rollbackFor={RuntimeException.class, Exception.class})
    public int add( Task task){
        return taskDao.add(task);
    }


    /***
     * 更新
     * @param task
     * @return
     */
    @Transactional(rollbackFor={RuntimeException.class, Exception.class})
    public int update(Task task){
        return  taskDao.update(task);
    }


    /***
     * 根据ID查询
     * @param userId  用户ID
     * @param taskType  任务类型：0、一次性任务   1 、每日任务
     * @param sortTask  任务id
     * @return
     */
    public Task findUserById(long userId,int taskType,long sortTask){

        return taskDao.findUserById(userId,taskType,sortTask);
    }

    /***
     * 分页查询
     * @param userId 用户ID
     * @param taskType  任务类型：0、一次性任务   1 、每日任务
     * @param page  页码 第几页 起始值1
     * @param count 每页条数
     * @return
     */
    public PageBean<Task> findList(long userId, int taskType, int page, int count) {

        List<Task> list;
        Page p = PageHelper.startPage(page,count);//为此行代码下面的第一行sql查询结果进行分页
        list = taskDao.findList(userId,taskType);

        return PageUtils.getPageBean(p,list);
    }

    /***
     * 分页查询
     * @param page  页码 第几页 起始值1
     * @param count 每页条数
     * @return
     */
    public PageBean<TaskList> findTaskList(int taskType,int page, int count) {

        List<TaskList> list;
        Page p = PageHelper.startPage(page,count);//为此行代码下面的第一行sql查询结果进行分页
        list = taskDao.findTaskList(taskType);

        return PageUtils.getPageBean(p,list);
    }
}
