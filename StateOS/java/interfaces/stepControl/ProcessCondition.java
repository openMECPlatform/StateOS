package interfaces.stepControl;

import interfaces.NetworkFunction;

/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:30
 * @Description:
 */
public interface ProcessCondition {
    void NFConnected(NetworkFunction nf);
    void addNetworkFunction(NetworkFunction nf);
}
