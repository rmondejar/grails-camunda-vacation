import org.camunda.bpm.engine.repository.ProcessDefinition

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class VacationRequestController {

    static allowedMethods = [save: "POST", performApproval: "POST", update: "POST"]

    def camundaService

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond VacationRequest.list(params), model: [vacationRequestInstanceCount: VacationRequest.count()]
    }

    def create() {

        def processDefinition = camundaService.getProcessDefinition(params.id)
        session.currentProcDefId = processDefinition.id

        respond new VacationRequest(params)
    }

    @Transactional
    def save(VacationRequest vacationRequestInstance) {
        if (vacationRequestInstance == null) {
            notFound()
            return
        }

        if (vacationRequestInstance.hasErrors()) {
            respond vacationRequestInstance.errors, view: 'create'
            return
        }

        def pi = camundaService.startProcess(session.currentProcDefId, vacationRequestInstance.properties, session.user)
        vacationRequestInstance.processInstanceId = pi.processInstanceId

        vacationRequestInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'vacationRequest.label', default: 'VacationRequest'), vacationRequestInstance.id])
                redirect vacationRequestInstance
            }
            '*' { respond vacationRequestInstance, [status: CREATED] }
        }
    }

    def show(VacationRequest vacationRequestInstance) {
        respond vacationRequestInstance
    }

    def approval() {

        def t = camundaService.getTaskById(params.id)
        VacationRequest vacationRequestInstance = VacationRequest.findByProcessInstanceId(t.processInstanceId)
        def vars = camundaService.getTaskVars(t.id)
        vars.each { k,v ->
            vacationRequestInstance.properties."$k" = v
        }
        render(view: "approval", model: [vacationRequestInstance: vacationRequestInstance, taskId:t.id] )
    }

    def performApproval() {

        VacationRequest vacationRequestInstance = VacationRequest.get(params.id)
        vacationRequestInstance.properties = params
        def t = camundaService.getTaskById(params.taskId)

        if (!vacationRequestInstance.hasErrors() && vacationRequestInstance.save(flush: true)) {
                flash.message = "${message(code: 'default.updated.message', args: [message(code: 'vacationRequest.label', default: 'VacationRequest'), vacationRequestInstance.id])}"

                if (params.save) {
                    camundaService.saveTask(t.id, params, session.user)
                    //redirect(action:'show', params:[id:"${params.id}"])
                }
                else {
                    params.id = vacationRequestInstance.id
                    params.vacationApproved = vacationRequestInstance.approvalStatus.toUpperCase() == "APPROVED"
                    params.approvalRemark = params.approvalRemark && params.approvalRemark != "" ? params.approvalRemark : "No Approval Remark."
                    camundaService.completeTask(t.id, params, session.user)

                    params.isApproval = true
                }
                redirect(action: "show", id: vacationRequestInstance.id, params: params)
        }
        else {
            render(view: "approval", model: [vacationRequestInstance: vacationRequestInstance, taskId:t.id] )
        }

    }

    def edit() {

        def t = camundaService.getTaskById(params.id)
        VacationRequest vacationRequestInstance = VacationRequest.findByProcessInstanceId(t.processInstanceId)
        def vars = camundaService.getTaskVars(t.id)
        vars.each { k,v ->
            vacationRequestInstance.properties."$k" = v
        }
        render(view: "edit", model: [vacationRequestInstance: vacationRequestInstance, taskId:t.id] )
    }

    def update() {

        VacationRequest vacationRequestInstance = VacationRequest.get(params.id)
        vacationRequestInstance.properties = params
        def t = camundaService.getTaskById(params.taskId)

        if (!vacationRequestInstance.hasErrors() && vacationRequestInstance.save(flush: true)) {
            flash.message = "${message(code: 'default.updated.message', args: [message(code: 'vacationRequest.label', default: 'VacationRequest'), vacationRequestInstance.id])}"

            if (params.save) {
                camundaService.saveTask(t.id, params, session.user)
            }
            else {
                params.resendRequest = vacationRequestInstance.resendRequest
                camundaService.completeTask(t.id, params, session.user)
            }
            redirect(action: "show", id: vacationRequestInstance.id, params: [taskId:params.taskId, complete: true])
        }
        else {
            render(view: "edit", model: [vacationRequestInstance: vacationRequestInstance, myTasksCount: assignedTasksCount])
        }

    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'vacationRequest.label', default: 'VacationRequest'), params.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NOT_FOUND }
        }
    }
}
