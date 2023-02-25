package me.vlod.pinto.networking;

import java.util.HashMap;

import me.vlod.pinto.PintoServer;

public class Packets {
    private static HashMap<Integer, Class<?>> packetMap = new HashMap<Integer, Class<?>>();

    static {
    	packetMap.put(0, PacketLogin.class);
    	packetMap.put(1, PacketLogout.class);
    	packetMap.put(2, PacketMessage.class);
    	packetMap.put(3, PacketTyping.class);
    }

    public static Packet getPacketByID(int id) {
    	// Get the packet class by ID
        Class<?> packetClass = packetMap.get(id);

        // Check if the specified ID map was successful
        if (packetClass != null) {
        	// Check if the specified class implements Packet
        	if (Packet.class.isAssignableFrom(packetClass)) {
        		try {
					return (Packet)packetClass.newInstance();
				} catch (Exception ex) {
					// Failure?
					PintoServer.logger.throwable(ex);
				}
        	}
        }
        
        return null;
    }
}
