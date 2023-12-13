package zsharestatemove;

import Server.OperationManager;
import interfaces.stepControl.ProcessCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleMove {
    protected static Logger logger = LoggerFactory.getLogger(SingleMove.class);

    public static void main(String[] args) {
        ActionMsgProcessor actionMsgProcessors = new ActionMsgProcessor();
        OperationManager operationManager = new OperationManager(actionMsgProcessors);

        try {
            operationManager.run2();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ProcessCondition moveProcessControl = new MoveProcessControl(operationManager);
        new Thread((Runnable) moveProcessControl).start();

        synchronized (operationManager){
            while(OperationManager.serverSet != 1){
                try {
                    operationManager.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            logger.info("netty server is set up");
        }

    }
}
