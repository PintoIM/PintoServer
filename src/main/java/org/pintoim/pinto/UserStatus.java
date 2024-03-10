package org.pintoim.pinto;

public enum UserStatus {
    ONLINE(0),
    AWAY(1),
    BUSY(2),
    INVISIBLE(3),
    OFFLINE(4);
    
    private int index;
	
    UserStatus(int index) {
    	this.index = index;
    }
    
    public int getIndex() {
    	return this.index;
    }
    
    public static UserStatus fromIndex(int index) {
    	for (UserStatus status : UserStatus.values()) {
    		if (status.getIndex() == index) {
    			return status;
    		}
    	}
    	
    	return UserStatus.OFFLINE;
    }
}
