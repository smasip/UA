package layersUA;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import fsmUA.*;
import layers.*;
import mensajesSIP.*;

public class TransactionLayerUA extends TransactionLayer{
	
	InetAddress addressProxy;
	int portProxy;
	ClientStateUA client;
	ServerStateUA server;
	String currentCallID;
	Transaction currentTransaction;
	
	public TransactionLayerUA() {
		super();
		this.client = ClientStateUA.TERMINATED;
		this.server = ServerStateUA.TERMINATED;
		this.currentTransaction = Transaction.REGISTER_TRANSACTION;
	}
	
	
	public String getCurrentCallID() {
		return currentCallID;
	}

	public void setCurrentCallID(String currentCallID) {
		this.currentCallID = currentCallID;
	}
	
	
	public InetAddress getAddressProxy() {
		return addressProxy;
	}


	public void setAddressProxy(InetAddress addressProxy) {
		this.addressProxy = addressProxy;
	}


	public int getPortProxy() {
		return portProxy;
	}


	public void setPortProxy(int portProxy) {
		this.portProxy = portProxy;
	}


	@Override
	public void recvFromTransport(SIPMessage message) {
		
		switch (currentTransaction) {
		
			case REGISTER_TRANSACTION:
				
				if (message instanceof OKMessage) {
					currentTransaction = Transaction.NO_TRANSACTION;
					ul.recvFromTransaction(message);
				}
				
				break;
		
			case INVITE_TRANSACTION:
				
				client = client.processMessage(message, this);
				if (client == ClientStateUA.TERMINATED) {
					currentTransaction = Transaction.NO_TRANSACTION;
				}
				
				break;
				
			case NO_TRANSACTION:
				
				if(message instanceof InviteMessage) {
					//currentCallID=
					currentTransaction = Transaction.INVITE_TRANSACTION;
					server = ServerStateUA.PROCEEDING;
					server = server.processMessage(message, this);
				}
				
				break;
				
			default:
				break;
				
		}
		
	}

	public void recvFromUser(SIPMessage message) {
		
		switch (currentTransaction) {
		
			case REGISTER_TRANSACTION:
				
				try {
					transportLayer.sendToNetwork(addressProxy, portProxy, message);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				break;
			
			case INVITE_TRANSACTION:
				
				server = server.processMessage(message, this);
				if(server == ServerStateUA.TERMINATED){
					currentTransaction = Transaction.NO_TRANSACTION;
				}
				
				break;
				
			case NO_TRANSACTION:
				
				if(message instanceof InviteMessage) {
					//currentCallID =
					currentTransaction = Transaction.INVITE_TRANSACTION;
					client = ClientStateUA.CALLING;
					client = client.processMessage(message, this);
				}
				
				break;
				
			default:
				break;
				
		}
		
		
	}
	
	public void sendToTransport(SIPMessage message) throws IOException {
		transportLayer.sendToNetwork(addressProxy, portProxy, message);
	}
	
	
	public void resetLayer() {
		client = ClientStateUA.TERMINATED;
		server  = ServerStateUA.TERMINATED;
		currentCallID = null;
	}


}
