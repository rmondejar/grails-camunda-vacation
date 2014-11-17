import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.test.mock.Mocks
import spock.lang.Specification

/**
 * Integration Test for camunda SimpleProcess 
 */
class VacationProcessSpec extends Specification {

    /**
     * 1) Inject camunda process engine API service beans
     */
    RuntimeService runtimeService
    TaskService taskService

    /**
     * 2) Mock your Grail(s) services called from SimpleProcess
     */
    def sampleSimpleProcessService = Mock(SampleSimpleProcessService)

    /*
     * Sample service to get started quickly. For real testing, mock your actual
     * Grails Service(s) called from SimpleProcess, then delete this!
     */
    class SampleSimpleProcessService {
        void serviceMethod() {}
    }

    /**
     * 3) Register your service mocks to make them accessible via SimpleProcess
     */
    def setup() {
        Mocks.register("sampleSimpleProcessService", sampleSimpleProcessService)
    }

    def cleanup() {
        Mocks.reset()
    }

    /**
     * 4) Test the various aspects and behaviour of SimpleProcess
     */
    void "Testing a happy walk through SimpleProcess"() {

        given: "a new instance of SimpleProcess"
        runtimeService.startProcessInstanceByKey("SimpleProcess")

        when: "completing the user task"
        def task = taskService.createTaskQuery().singleResult()
        taskService.complete(task.id)

        then: "the service method defined for the subsequent service task was called exactly once"
        1 * sampleSimpleProcessService.serviceMethod()

        and: "nothing else was called"
        0 * _

        and: "the process instance finished"
        !runtimeService.createProcessInstanceQuery().singleResult()

    }

}
