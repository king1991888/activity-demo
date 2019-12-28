import com.king.activity.demo.pojo.TaskLink;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * @author king
 * 2019/3/3
 */
public class TestActiviti {

    public static final String DB_SCHEMA_UPDATE_FALSE = "false";//不会自动创建表，没有表，则抛异常
    public static final String DB_SCHEMA_UPDATE_CREATE_DROP = "create-drop";//先删除，再创建表
    public static final String DB_SCHEMA_UPDATE_TRUE = "true";//假如没有表，则自动创建

    ProcessEngine processEngine=createProcessEngine();

    @Test
    public void testCreateProcessEngine(){
        System.out.println(processEngine);
    }

    private ProcessEngine createProcessEngine(){
        /**
         * 创建工作流引擎，三种方式
         * 方式一：java代码形式
         * */
        //取得ProcessEngineConfiguration对象
        ProcessEngineConfiguration processEngineConfiguration = ProcessEngineConfiguration.
                createStandaloneProcessEngineConfiguration();
        //设置数据库连接属性，如果有数据源，可以直接设置数据源，不必一个个设置参数
        processEngineConfiguration.setJdbcDriver("com.mysql.jdbc.Driver");
        processEngineConfiguration.setJdbcUrl("jdbc:mysql://localhost:3306/activitiDB?" +
                "createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8"
                +"&serverTimezone=Hongkong");
        processEngineConfiguration.setJdbcUsername("root");
        processEngineConfiguration.setJdbcPassword("root");
        //processEngineConfiguration.setDataSource(dataSource);
        //创建表的策略，假如没有表时，自动创建
        processEngineConfiguration.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_TRUE);
        //通过ProcessEngineConfiguration对象创建 ProcessEngine 对象
        return processEngineConfiguration.buildProcessEngine();

        /**
         * 方式二，加载配置文件的形式
         * */
        //找类路径下的activiti.cfg.xml配置文件进行加载
        /*return ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("activiti.cfg.xml")
        .buildProcessEngine();*/

        /**
         * 方式三：通过默认配置进行创建
         * */
        /*//默认加载类路径下的activiti.cfg.xml配置文件
        return ProcessEngines.getDefaultProcessEngine();*/
    }

    /**
     * 部署流程定义，这步操作将同时操作act_re_deployment部署表和act_re_procdef流程定义表
     * 并且会将流程定义文件bpmn文件以二进制流的形式存进act_ge_bytearray二进制数据表
     * */
    @Test
    public void deployProcDel(){
        //获取存储流程定义的仓库服务
        RepositoryService repositoryService=processEngine.getRepositoryService();
        //获取部署对象
        Deployment deployment=repositoryService.createDeployment()//创建deploymentBuilder对象
                .addClasspathResource("processes/leaveBill.bpmn")//加载类路径的流程定义bpmn文件
                .addClasspathResource("processes/leaveBill.png")//加载类路径的流程定义png文件
                .name("请假单流程")//给部署的流程定义取个名字
                .category("办公流程")//给部署的定成定义设个类别
                .deploy();//部署
        System.out.println("部署的id："+deployment.getId());
        System.out.println("部署的名字："+deployment.getName());
        System.out.println("部署的类别："+deployment.getCategory());
    }

    /**
     * 以zip文件方式部署流程定义
     * */
    @Test
    public void deployProcDefByZip(){
        //加载zip资源文件作为输入流，zip文件里面包含bpmn和png文件
        InputStream in=this.getClass()
                .getClassLoader()
                .getResourceAsStream("processes/addWorkBill.zip");
        //部署
        Deployment deployment=processEngine
                .getRepositoryService()
                .createDeployment()
                .addZipInputStream(new ZipInputStream(in))
                .name("加班流程")
                .category("办公流程")
                .deploy();
        System.out.println(deployment);
    }

    /**
     * 执行流程，将在act_hi_procinst历史流程实例表新增一条流程实例的数据
     * 并且会在act_hi_taskinst历史任务实例表新增历史任务数据
     * 然后会在act_ru_task表新增正在办理中的任务数据
     * */
    @Test
    public void process(){
        //指定流程定义的key值，即流程定义id值，启动流程，产生一个流程实例
        String processDefKey="addWorkBill";
        //获得运行时服务
        RuntimeService runtimeService=processEngine.getRuntimeService();
        //通过流程定义key值启动流程，取得流程实例，默认是启动该流程定义的最新版本的
        ProcessInstance processInstance=runtimeService
                .startProcessInstanceByKey(processDefKey);
        System.out.println("流程实例的id："+processInstance.getId());
        System.out.println("对应流程定义id："+processInstance.getProcessDefinitionId());
    }

    /**
     * 查询流程定义
     * */
    @Test
    public void queryProcDef(){
        String processDefKey="addWorkBill";
        List<ProcessDefinition> processDefinitions=processEngine
                .getRepositoryService()
                .createProcessDefinitionQuery()//创建流程定义查询对象
                //.processDefinitionId("")//根据流程定义id查询,流程定义id:addWorkBill:1:15004 组成 ： proDefikey（流程定义key）+version(版本)+自动生成id
                .processDefinitionKey(processDefKey)//根据流程定义key查询
                //.processDefinitionName(name)//根据流程定义name查询，name=xml文件的name属性
                //.processDefinitionVersion(version)//根据流程定义版本查询
                .latestVersion()//查询最新版本
                .orderByProcessDefinitionVersion().desc()//根据版本降序排序
                //.count()//统计数量
                //.listPage(firstResult,maxResults)//分页查询,与mysqllimit等同，偏移量和最大结果数
                .list();
        System.out.println(processDefinitions);
    }

    /**
     * 查看bpmn资源图片
     * */
    @Test
    public void queryBpmnPng() throws Exception{

        //根据部署id查询，部署id可以根据流程定义实体获得
        String delpoymentId="15001";
        String pngName="";
        //根据部署id获取流程定义的资源名称集合
        List<String> resourceNames=processEngine
                .getRepositoryService().getDeploymentResourceNames(delpoymentId);
        //提取图片名称
        if(null!=resourceNames&&resourceNames.size()>0){
            for (String resourceName:resourceNames
                 ) {
                if(resourceName.substring(resourceName.lastIndexOf(".")+1)
                        .equalsIgnoreCase("png")){
                    pngName=resourceName;
                    break;
                }
            }
        }

        //通过部署id和资源名字获取资源
        InputStream in=processEngine.getRepositoryService()
                .getResourceAsStream(delpoymentId,pngName);
        File png=new File("d:/addWorkBill.png");
        FileOutputStream out=new FileOutputStream(png);
        byte[] b=new byte[1024];
        int count=0;

        while((count=in.read(b))!=-1){
            out.write(b,0,count);
            out.flush();
        }
        out.close();
        in.close();
    }

    /**
     * 删除流程定义，同时会将流程实例和流程节点任务数据一并删除
     * */
    @Test
    public void deleteProcess(){
        //通过部署id删除
        String deploymentId="";
        processEngine.getRepositoryService().deleteDeployment(deploymentId);

    }

    /**
     * 查询任务
     * */
    @Test
    public void queryProcess(){
        //指定待办人
        String assignee="李四";
        //取得任务服务
        TaskService taskService=processEngine.getTaskService();
        //创建一个查询对象
        TaskQuery taskQuery=taskService.createTaskQuery();
        //查询待办人的任务列表
        List<Task> tasks=taskQuery.taskAssignee(assignee)
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
     * 完成任务，完成后将删除运行时表act_ru_task的任务节点数据
     * */
    @Test
    public void completeTask(){
        //活动中的任务对应的流程实例
        String processInstanceId="22501";
        //指定待办任务id
        String taskId="25002";
        TaskService taskService=processEngine.getTaskService();
        //办理任务,提交审批意见并指定审批类型
        taskService.addComment(taskId,processInstanceId,"审批意见","同意;无补充意见");
        taskService.complete(taskId);
        System.out.println("办理完成");
    }


    /**
     * 查询流程实例状态
     * */
    @Test
    public void queryProcessInstan(){
        //流程实例id
        String processInstanId="12501";
        ProcessInstance processInstance=processEngine.getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(processInstanId)
                .singleResult();

        //判断流程实例的状态:如果实例id不为空，则该流程实例未完成，如果为空，说明该流程实例已完成。
        if(processInstance!=null){
            System.out.println("该流程实例"+processInstanId+"正在运行...  ");
        }else{
            System.out.println("当前的流程实例"+processInstanId+" 已经结束！");
        }
    }

    /**
     * 查询历史执行流程实例信息(全部)
     * */
    @Test
    public void queryHistoryProcessInstances(){
       List<HistoricProcessInstance> historicProcessInstances=processEngine.getHistoryService()
                .createHistoricProcessInstanceQuery()
                .list();

       if(null!=historicProcessInstances&&historicProcessInstances.size()>0){
           for (HistoricProcessInstance historicProcessInstance:historicProcessInstances
                ) {
               System.out.println(historicProcessInstance);
           }
       }
    }


    /**
     * 根据流程实例id查询对应历史流程所有任务节点
     * */
    @Test
    public void queryHistoricTasksByProcessInstanceId(){
        String proicessInstanceId="22501";
        TaskService taskService=processEngine.getTaskService();
        List<HistoricTaskInstance> historicTaskInstances=processEngine.getHistoryService()
                .createHistoricTaskInstanceQuery()
                .processInstanceId(proicessInstanceId)
                //.unfinished()//查询未完成的流程节点，即正在活动中的流程节点
                .list();
        if(null!=historicTaskInstances&&historicTaskInstances.size()>0){
            for (HistoricTaskInstance historicTaskInstance: historicTaskInstances
                 ) {
                System.out.println(historicTaskInstance);
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


    /**
     *设置流程变量,流程变量是为了bpmn流程模型图形中的排他网关流程走向变量设置以及设置下游节点审批人而用，例如：
     * 是否同意，是否走下一步等。
     * 不需要用于保存表单业务数据，业务数据可以查询数据库获取
     * 将在act_hi_varinst历史流程变量表以及运行时流程变量表act_ru_variable新增数据，对于流程变量值是个对象的，将序列化保存为二进制文件在
     * act_ge_bytearray二进制数据表
     * */
    @Test
    public void setProcessVariables(){

        String taskId="";
        /**
         * 四种方式设置流程变量
         * */
        Map<String, Object> variables=new HashMap();//设置多个流程变量
        //第一种：通过runtimeService对象设置
        String executionId="execution对象的ID";
        RuntimeService runtimeService=processEngine.getRuntimeService();
        /*runtimeService.setVariable(executionId,"变量Key","变量值");//设置单个流程变量，作用域：整个流程
        runtimeService.setVariableLocal(executionId,"变量Key","变量值");//设置单个流程变量。作用域：当前任务
        runtimeService.setVariables(executionId,variables);//设置多个流程变量Map
        *//**
         * 流程变量支持类型：String 、boolean、Integer、double、date，自定义类型
         * *//*
        runtimeService.setVariableLocal(executionId,"审批链",new TaskLink());*/
        //第二种，通过taskService设置，一般在流程过程中使用这种
        TaskService taskService=processEngine.getTaskService();
        taskService.setVariable(taskId,"变量Key","变量值");//设置单个流程变量，作用域：整个流程
        taskService.setVariableLocal(taskId,"变量Key","变量值");//设置单个流程变量。作用域：当前任务
        taskService.setVariables(taskId,variables);//设置多个流程变量Map
        /**
         * 流程变量支持类型：String 、boolean、Integer、double、date，自定义bean,注意bean必须实现Serializable接口，支持序列化
         * 因为是序列化到数据库中
         * */
        taskService.setVariableLocal(taskId,"审批链",new TaskLink());

        //第三种：开始执行流程时设置流程变量,只能设置map对象
        runtimeService.startProcessInstanceByKey("",variables);

        //第四种：完成任务时设置流程变量
        taskService.complete(taskId,variables);
    }

    /**
     * 查询流程变量
     * */
    @Test
    public void queryProcessVariables(){

        RuntimeService runtimeService=processEngine.getRuntimeService();
        runtimeService.getVariable("","");//取某个变量
        runtimeService.getVariables("");//取全部变量
        runtimeService.getVariableLocal("","");//取本节点的某个变量
        runtimeService.getVariablesLocal("");//取本节点的全部变量

        TaskService taskService=processEngine.getTaskService();

        taskService.getVariable("","");//取某个变量
        taskService.getVariables("");//取全部变量
        taskService.getVariableLocal("","");//取本节点的某个变量
        taskService.getVariablesLocal("");//取本节点的全部变量
        //取自定义bean变量
        TaskLink taskLink=(TaskLink)taskService.getVariable("","");

    }


    /**
     * 设置审批人
     * */
    @Test
    public void setProcessAssignee(){

        TaskService taskService=processEngine.getTaskService();
        taskService.setAssignee("","");

    }






}
