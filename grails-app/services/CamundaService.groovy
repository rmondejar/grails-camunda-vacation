import grails.util.GrailsNameUtils
import org.camunda.bpm.engine.FormService
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.IdentityService
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.repository.ProcessDefinition


class CamundaService {

    RuntimeService runtimeService
    TaskService taskService
    IdentityService identityService
    FormService formService
    RepositoryService repositoryService
    HistoryService historyService

    def startProcess(String processDefinitionId, Map vars, String user) {
        vars.user = user
        runtimeService.startProcessInstanceById(processDefinitionId, vars)
    }

    def startProcess(String deploymentId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult()
        runtimeService.startProcessInstanceById(processDefinition.id)
    }

    def getUnassignedTasks() {
        taskService.createTaskQuery().taskUnassigned().list()
    }

    def getAssignedTasks(String user) {
        taskService.createTaskQuery().taskAssignee(user).list()
    }

    def getCandidateTasks(groups) {
        taskService.createTaskQuery().taskCandidateGroupIn(groups).list()
    }


    def getUserTaskById(String id) {
        taskService.createTaskQuery().taskId(id)
    }

    def deploymentList() {
        repositoryService.createProcessDefinitionQuery().active().list()
    }

    def getDeploymentById(String id) {
        repositoryService.createDeploymentQuery().deploymentId(id).singleResult()
    }


    String getStartFormKey(String processDefinitionId) {
        formService.getStartFormKey(processDefinitionId)
    }


    String getTaskFormKey(String taskId) {

        def t = getTaskById(taskId)
        formService.getTaskFormKey(t.processDefinitionId, t.taskDefinitionKey)
    }

    def getProcessDefinition(String deploymentId) {
        repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
    }

    def getProcessDefinitionById(String processDefinitionId) {
        repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult()

    }

    def getStartFormData(String processDefinitionId) {
        def startFormData  = formService.getStartFormData(processDefinitionId)
        startFormData?.formFields
    }

    def getFormData(String taskId) {
        def formData = formService.getTaskFormData(taskId)
        formData?.formFields
    }

    private findTasks(String methodName, String username, int firstResult, int maxResults, Map orderBy) {
        def taskQuery = taskService.createTaskQuery()
        if (methodName) {
            taskQuery."${methodName}"(username)
        }

        if (orderBy) {
            orderBy.each { k, v ->
                taskQuery."orderByTask${GrailsNameUtils.getClassNameRepresentation(k)}"()."${v}"()
            }
        }
        taskQuery.listPage(firstResult, maxResults)
    }

    def getProcessInstance(id) {
        def q = runtimeService.createProcessInstanceQuery().processInstanceId(id)
        def pi = q.singleResult()
        if (!pi) {
            q = historyService.createHistoricProcessInstanceQuery().processInstanceId(id)
            pi = q.singleResult()
        }
        pi
    }

    def getNumInstances(ProcessDefinition processDefinition) {
        historyService.createHistoricProcessInstanceQuery().processDefinitionId(processDefinition.getId()).count();
    }

    def getProcessInstanceTasks(processInstanceId) {
        historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list()
    }

    def getProcessInstanceOpenTasks(processInstanceId) {
        taskService.createTaskQuery().processInstanceId(processInstanceId).orderByTaskCreateTime().asc().list()

    }

    def getPIOpenTasks(List processInstanceIds) {
        processInstanceIds.collectEntries({ [it.id, getPIOpenTasks(it.id)] })
    }


    def getTask(String processInstanceId, String methodName = null, String user = '') {
        def q = taskService.createTaskQuery().processInstanceId(processInstanceId)
        if (methodName)
            q = q."${methodName}"(user)
        q.singleResult()
    }

    def getTaskById(String taskId) {
        taskService.createTaskQuery().taskId(taskId).singleResult()
    }

    def claimTask(String taskId, String username) {
        taskService.claim(taskId, username)
    }

    def unclaimTask(String taskId, String username) {
        taskService.claim(taskId, null)
    }

    def getTaskVars(String taskId) {
        taskService.getVariables(taskId)
    }

    def saveTask(String taskId, Map vars, String user) {
        vars.user = user
        taskService.setVariables(taskId, vars)
    }

    def completeTask(String taskId, Map vars, String user) {
        vars.user = user
        taskService.setVariablesLocal(taskId, vars)
        println "complete $taskId -> $vars"
        taskService.complete(taskId, vars)
    }

    def setAssignee(String taskId, String username) {
        taskService.setAssignee(taskId, username)
    }

    def setPriority(String taskId, int priority) {
        taskService.setPriority(taskId, priority)
    }

    def getCandidateUserIds(String taskId) {
        def identityLinks = getIdentityLinksForTask(taskId)
        def userIds = []
        def users
        identityLinks.each { identityLink ->
            if (identityLink.groupId) {
                users = identityService.createUserQuery()
                        .memberOfGroup(identityLink.groupId)
                        .orderByUserId().asc().list()

                userIds << users?.collect { it.id }
            } else {
                userIds << identityLink.userId
            }
        }
        userIds.flatten().unique()
    }

    def getIdentityLinksForTask(String taskId) {
        taskService.getIdentityLinksForTask(taskId)
    }

    def syncUser(userId, groupIds) {

        def user = identityService.createUserQuery().userId(userId).list()
        if (!user) identityService.saveUser(identityService.newUser(userId))

        groupIds.each { gid ->
            def group = identityService.createGroupQuery().groupId(gid).list()
            if (!group) identityService.saveGroup(identityService.newGroup(gid))
        }

        def userGroups = identityService.createGroupQuery().groupMember(userId).list()

        //new membership?
        def newGroups = groupIds - userGroups*.id
        newGroups.each { group -> identityService.createMembership(userId, group) }

        //leave membership?
        def oldGroups = userGroups*.id - groupIds
        oldGroups.each { group -> identityService.deleteMembership(userId, group) }

    }


 }