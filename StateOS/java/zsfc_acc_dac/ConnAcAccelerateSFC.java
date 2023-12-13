package zsfcaccelerateconnac;

import Server.OperationManager;
import interfaces.msgprocessors.ProcessReceiveMsg;
import interfaces.stepControl.ProcessCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConnAcAccelerateSFC {
    protected static Logger logger = LoggerFactory.getLogger(ConnAcAccelerateSFC.class);

    public static void main(String[] args) {
        ProcessReceiveMsg connMsgProcessors = new ConnMsgProcessor();
        ProcessReceiveMsg actionMsgProcessors = new ActionMsgProcessor();
        OperationManager operationManager = new OperationManager(connMsgProcessors,actionMsgProcessors);

        try {
            operationManager.run1();
            //operationManager.run2();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ProcessCondition accelerateSFCControl = new AccelerateSFCControl(operationManager);
        new Thread((Runnable) accelerateSFCControl).start();

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
