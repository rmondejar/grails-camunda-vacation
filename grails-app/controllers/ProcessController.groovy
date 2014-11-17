import org.camunda.bpm.engine.form.FormField
import org.camunda.bpm.engine.repository.ProcessDefinition

class ProcessController {

    def camundaService

    def index() {

        def processDefinitions = camundaService.deploymentList()
        def data = []
        processDefinitions.each { ProcessDefinition p ->

            def numInstances = camundaService.getNumInstances(p)
            Expando exp = new Expando(id:p.deploymentId, name:p.name, key:p.id, version:p.version, numInstances:numInstances)
            data << exp
        }

        [processes: data]
    }

    def show() {
        def processDefinition = camundaService.getProcessDefinition(params.id)

        def numInstances = camundaService.getNumInstances(processDefinition)
        [process: processDefinition, numInstances: numInstances]
    }

    def create(String id){

        def processDefinition = camundaService.getProcessDefinition(id)

        //external form ?
        def formKey = camundaService.getStartFormKey(processDefinition.id)

        if (formKey) {
            redirect uri:"$formKey/$id"
        }
        else { //or generate form
            def startFormData = camundaService.getStartFormData(processDefinition.id)
            println startFormData
            startFormData.each { FormField d ->
                println d.id
                println d.label
                println d
            }

            render(view: 'create.gsp', model: [startFormData: startFormData, processDefinitionId: processDefinition.deploymentId])
        }
    }

    def save(){

        def processDefinition = camundaService.getProcessDefinition(params.id)
        def startFormData = camundaService.getStartFormData(processDefinition.id)

        def vars = [:]
        startFormData.each { FormField d ->
            d.defaultValue = params[d.id]
            vars.put(d.id, d.defaultValue)
        }

        camundaService.startProcess(processDefinition.id, vars, session.user)

        redirect(action:'index')

    }


}
