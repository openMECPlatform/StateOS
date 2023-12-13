package zsharestatecopy;

import Server.OperationManager;
import interfaces.stepControl.ProcessCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SingleCopy {
    protected static Logger logger = LoggerFactory.getLogger(SingleCopy.class);

    public static void main(String[] args) {
        ActionMsgProcessor actionMsgProcessors = new ActionMsgProcessor();
        OperationManager operationManager = new OperationManager(actionMsgProcessors);

        try {
            //operationManager.run1();
            operationManager.run2();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ProcessCondition copyProcessControl = new CopyProcessControl(operationManager);
        new Thread((Runnable) copyProcessControl).start();

        synchronized (operationManager){
            while(OperationManager.serverSet != 1){
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
