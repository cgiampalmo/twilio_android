package com.glaciersecurity.glaciercore.api;

public class CoreConnectionState {
    private static CoreConnectionState coreConnectionState = null;

    public enum CoreState {
        UNKNOWN(2),ACCEPTED(1),DENIED(0);

        private int value;

        CoreState(int value){
            this.value = value;
        }

        public int getValue(){
            return value;
        }
    }

    private CoreState coreState;

    private CoreConnectionState(){
        coreState = CoreState.UNKNOWN;
    }

    public static CoreConnectionState getInstance(){
        if (coreConnectionState == null) {
            coreConnectionState = new CoreConnectionState();
        }
        return coreConnectionState;

    }

    public boolean hasCorePermission(){
        return coreState == CoreState.ACCEPTED;
    }

    public void setCoreState(CoreState coreState) {
        this.coreState = coreState;
    }





}