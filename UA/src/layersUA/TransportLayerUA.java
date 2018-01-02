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
		    	TransportLayerUA.super.recvFromNetwork();
		    }
		};
		t.start();
		
	}

}
