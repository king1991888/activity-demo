import com.king.activity.demo.ActivityApplication;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author king
 * 2019/3/17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ActivityApplication.class)
/**
 * 多人审批流程测试，包括顺序、并发(最先一人通过即结束)、并发(全部通过才能结束)
 * */
public class TestActivitiSpringboot2 {



    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private HistoryService historyService;



    @Test
    /**
     * 部署流程定义
     * */
    public void deployProcDef(){
        //获取部署对象
        Deployment deployment=repositoryService.createDeployment()//创建deploymentBuilder对象
                .addClasspathResource("processes/addWorkBill.bpmn")//加载类路径的流程定义bpmn文件
                .name("加班流程")//给部署的流程定义取个名字
                .category("办公流程")//给部署的定成定义设个类别
                .deploy();//部署
        System.out.println("部署的id："+deployment.getId());
        System.out.println("部署的名字："+deployment.getName());
        System.out.println("部署的类别："+deployment.getCategory());
    }


    /**
     * 执行流程
     * */
    @Test
    public void startProcess(){

        //指定流程定义的key值，即流程定义id值，启动流程，产生一个流程实例
        String processDefKey="addWorkBill";
        //设置审批人的流程变量
        Map<String,Object> audits=new HashMap();
        List<String> managerList= new ArrayList();
        managerList.add("张三");
        managerList.add("李四");
        audits.put("managerList",managerList);
        List<String> leaderList= new ArrayList();
        leaderList.add("王五");
        leaderList.add("赵六");
        audits.put("leaderList",leaderList);
        List<String> bosses= new ArrayList();
        bosses.add("韩七");
        bosses.add("林八");
        audits.put("bosses",bosses);
        audits.put("creator","郑九");
        //通过流程定义key值启动流程，取得流程实例，默认是启动该流程定义的最新版本的
        ProcessInstance processInstance=runtimeService
                .startProcessInstanceByKey(processDefKey,"表单地址url",audits);
        System.out.println("流程实例的id："+processInstance.getId());
        System.out.println("对应流程定义id："+processInstance.getProcessDefinitionId());
    }

    /**
     * 办理任务
     * */
    @Test
    public void complete(){
        //活动中的任务对应的流程实例
        String processInstanceId="102501";
        //指定待办任务id
        String taskId="115004";
        //办理任务,提交审批意见并指定审批类型
        taskService.addComment(taskId,processInstanceId,"审批意见","同意;无补充意见");
        //设置是否同意的流程变量
        Map<String,Object> params=new HashMap();
        //params.put("isMaster",1);
        params.put("isManager",1);
        //params.put("leaderChoose",1);
        params.put("isLeader",1);
        params.put("isBoss",1);
        taskService.complete(taskId,params);
        System.out.println("办理完成");
    }

    /**
     * 查询待理任务
     * */
    @Test
    public void queryUnfinishTask(){
        String assignee="林八";
        //创建一个查询对象
        TaskQuery taskQuery=taskService.createTaskQuery();
        //查询待办人的任务列表，办理人或者候选人
        List<Task> tasks=taskQuery.taskCandidateOrAssigned(assignee)
                .list();
        //遍历代办任务列表
        if(tasks!=null&&tasks.size()>0){
            for (Task task:tasks
                    ) {
                System.out.println("任务办理人："+task.getAssignee());
                System.out.println("任务id："+task.getId());
                System.out.println("任务名称："+task.getName());
            }
        }
    }

    /**
     * 查询流程实例对应下的所有任务节点
     * */
    @Test
    public void queryAllTaskByProcessInstanceId(){

        String proicessInstanceId="37501";
        List<HistoricTaskInstance> historicTaskInstances=historyService
                .createHistoricTaskInstanceQuery()
                .processInstanceId(proicessInstanceId)
                //.unfinished()//查询未完成的流程节点，即正在活动中的流程节点
                .list();
        if(null!=historicTaskInstances&&historicTaskInstances.size()>0){
            for (HistoricTaskInstance historicTaskInstance: historicTaskInstances
                    ) {
                System.out.println(historicTaskInstance);
                System.out.println(historicTaskInstance.getAssignee());
            }
        }
        //查询审批意见，可根据历史任务节点对象historicTaskInstance和审批意见对象comment的taskId进行对应的匹配
        List<Comment> comments=taskService
                .getProcessInstanceComments(proicessInstanceId);
        if(null!=comments&&comments.size()>0){
            for (Comment comment:comments
                    ) {
                System.out.println("审批意见:"+comment.getFullMessage());
            }
        }



    }

}
