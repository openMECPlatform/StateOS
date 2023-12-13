package interfaces;


import proto.MyActionMessageProto;
import proto.MyConnMessageProto;
import sun.nio.ch.Net;

public class StateChunk {
    public NetworkFunction dst;
    public MyConnMessageProto.ConnState connState;
    public MyActionMessageProto.ActionState actionState;
    public MyActionMessageProto.ActionMultiState actionMultiState;
    public MyActionMessageProto.ActionAllState actionAllState;
    public MyActionMessageProto.ShareState shareState;

    public StateChunk(NetworkFunction dst, MyConnMessageProto.ConnState connState) {
        this.dst = dst;
        this.connState = connState;
    }

    public StateChunk(NetworkFunction dst, MyActionMessageProto.ActionState actionState) {
        this.dst = dst;
        this.actionState = actionState;
    }

    public StateChunk(NetworkFunction dst, MyActionMessageProto.ShareState shareState){
        this.dst = dst;
        this.shareState = shareState;
    }

    public StateChunk(NetworkFunction dst,MyActionMessageProto.ActionMultiState actionMultiState) {
        this.dst = dst;
        this.actionMultiState = actionMultiState;
    }

    public StateChunk(NetworkFunction dst,MyActionMessageProto.ActionAllState actionAllState ) {
        this.dst = dst;
        this.actionAllState = actionAllState;
    }
}
