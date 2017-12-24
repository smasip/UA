package layersUA;

import java.io.IOException;
import java.net.DatagramPacket;

import layers.*;
import mensajesSIP.*;

public class TransportLayerUA extends TransportLayer{

	@Override
	public void recvFromNetwork(){
		// TODO Auto-generated method stub
		Thread t = new Thread() {
		    public void run() {
		    	byte[] buf = new byte[2048];
		    	DatagramPacket p = new DatagramPacket(buf, buf.length);
		    	SIPMessage message;
		    	while(true) {
		    		try {
		    			datagramSocket.receive(p);
		    			message = SIPMessage.parseMessage(new String(p.getData()));
		    			System.out.println();
		    			System.out.println("Received Message:");
		    			if (message instanceof OKMessage) {
		    				((OKMessage)message).setSdp(null);
		    			}
		    			System.out.println(message.toStringMessage());
		    			System.out.println();
		    			transactionLayer.recvFromTransport(message);
						p.setData(buf, 0, buf.length);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SIPException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    	}
		    }
		};
		t.start();
		
	}

}
