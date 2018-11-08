package org.flowable;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author Jorge Morando
 */
public class HolidayRequest {

    private static final Logger logger = LoggerFactory.getLogger(HolidayRequest.class);

    public static void main(String[] args) {

        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1")
                .setJdbcUsername("sa")
                .setJdbcPassword("")
                .setJdbcDriver("org.h2.Driver")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        ProcessEngine processEngine = cfg.buildProcessEngine();
        /* DEPLOYMENT */
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("process.bpmn")
                .deploy();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        logger.info("Found process definition : {}", processDefinition.getName());

        /* START INSTANCE */
        RuntimeService runtimeService = processEngine.getRuntimeService();
        final Map<String,Object> variables = gatherInitVariables();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinition.getKey(), variables);

        logger.info("Instance \"{}\" of process \"{}\" started", processInstance.getId(),processDefinition.getName());

        /* TASK MANAGEMENT */
        TaskService taskService = processEngine.getTaskService();
        String group = "managers";
        logger.info("Retrieving \"{}\" task list",group);
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(group).list();
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i=0; i<tasks.size(); i++) {
            System.out.println((i+1) + ") " + tasks.get(i).getName());
        }

        final int taskIndex = askWichTask();

        Task task = tasks.get(taskIndex - 1);
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " wants " +
                processVariables.get("nrOfHolidays") + " of holidays. Do you approve this?");

        final boolean approved = askApproveHolidays();
        taskService.complete(task.getId(), new HashMap<String, Object>(){{put("approved", approved);}});


        List<Execution> executions = runtimeService.createExecutionQuery().onlyProcessInstanceExecutions().processInstanceId(processInstance.getId()).list();
        for (Execution e: executions){
            String pdk = ((ExecutionEntityImpl)e).getProcessDefinitionKey();
            ProcessDefinition pd =repositoryService.createProcessDefinitionQuery().processDefinitionKey(pdk).singleResult();
            System.out.println("Execution \""+pd.getName()+"\" has ended? "+e.isEnded());
        }

        String taskId = taskService.createTaskQuery().list().get(0).getId();
        taskService.complete(taskId);

        executions = runtimeService.createExecutionQuery().onlyProcessInstanceExecutions().processInstanceId(processInstance.getId()).list();
        for (Execution e: executions){
            String pdk = ((ExecutionEntityImpl)e).getProcessDefinitionKey();
            ProcessDefinition pd =repositoryService.createProcessDefinitionQuery().processDefinitionKey(pdk).singleResult();
            System.out.println("Execution \""+pd.getName()+"\" has ended? "+e.isEnded());
        }

        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricActivityInstance> activities =
                historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .finished()
                        .orderByHistoricActivityInstanceEndTime().asc()
                        .list();

        for (HistoricActivityInstance activity : activities) {
            System.out.println(activity.getActivityId() + " took "
                    + activity.getDurationInMillis() + " milliseconds");
        }

    }

    private static boolean askApproveHolidays(){
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine().toLowerCase().equals("y");
    }

    private static int askWichTask(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Which task would you like to complete?");
        return Integer.valueOf(scanner.nextLine());
    }

    private static Map<String,Object> gatherInitVariables(){
        Scanner scanner= new Scanner(System.in);

        System.out.println("Who are you?");
        String employee = scanner.nextLine();

        System.out.println("How many holidays do you want to request?");
        Integer nrOfHolidays = Integer.valueOf(scanner.nextLine());

        System.out.println("Why do you need them?");
        String description = scanner.nextLine();

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("employee", employee);
        variables.put("nrOfHolidays", nrOfHolidays);
        variables.put("description", description);

        return  variables;
    }

}
