package me.vlod.pinto;

public enum CallStatus {
    CONNECTED(0),
    CONNECTING(1),
    ERROR(2),
    ENDED(3);
    
    private int index;
	
    CallStatus(int index) {
    	this.index = index;
    }
    
    public int getIndex() {
    	return this.index;
    }
    
    public static CallStatus fromIndex(int index) {
    	for (CallStatus status : CallStatus.values()) {
    		if (status.getIndex() == index) {
    			return status;
    		}
    	}
    	
    	return CallStatus.ENDED;
    }
}
