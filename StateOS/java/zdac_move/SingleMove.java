package zadconnacmove;

import Server.OperationManager;
import interfaces.stepControl.ProcessCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
;

public class SingleMove {
    protected static Logger logger = LoggerFactory.getLogger(zconnacmove.SingleMove.class);

    public static void main(String[] args) {
        ConnMsgProcessor connMsgProcessors = new ConnMsgProcessor();
        ActionMsgProcessor actionMsgProcessors = new ActionMsgProcessor();
        OperationManager operationManager = new OperationManager(connMsgProcessors,actionMsgProcessors);

        try {
            operationManager.run1();
            //operationManager.run2();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ProcessCondition moveProcessControl = new MoveProcessControl(operationManager);
        new Thread((Runnable) moveProcessControl).start();

        synchronized (operationManager){
            while(OperationManager.serverSet != 2){
                try {
                    operationManager.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //System.out.println("netty server is set up");
            logger.info("netty server is set up");
        }

    }
}
