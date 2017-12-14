package layersUA;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

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
	private Timer timer;
	private TimerTask task;
	
	public TransactionLayerUA() {
		super();
		this.client = ClientStateUA.TERMINATED;
		this.server = ServerStateUA.TERMINATED;
		this.currentTransaction = Transaction.REGISTER_TRANSACTION;
		this.timer = new Timer();
		this.task = null;
	}
	
	public void sendACK(SIPMessage error) {
		ACKMessage ack = new ACKMessage();
		InviteMessage invite = ((UserLayerUA)ul).getOutgoingInvite();
		
		ArrayList<String> vias = invite.getVias();
		String destination = invite.getDestination();
    	String toUri = invite.getToUri();
   	 	String fromUri = invite.getFromUri();
   	 	String callId = invite.getCallId();
   	 	String cSeqNumber = "1";
   	 	String cSeqStr = "ACK";
   	 	
   	 	ack.setDestination(destination);
   	 	ack.setCallId(callId);
	 	ack.setToUri(toUri);
	 	ack.setFromUri(fromUri);
	 	ack.setcSeqStr(cSeqStr);
	 	ack.setcSeqNumber(cSeqNumber);
	 	ack.setVias(vias);
   	 	
   	 	if(task == null) {
   	 		task = new TimerTask() {
			
				@Override
				public void run() {
					client = ClientStateUA.TERMINATED;
					task = null;
					ul.recvFromTransaction(error);
				}
   	 		};
		
			timer.schedule(task, 1000);
   	 	}
   	 	
   	 	try {
			sendToTransport(ack);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void sendError(SIPMessage error) {
		
		if(task == null) {
			
   	 		task = new TimerTask() {
   	 			
   	 			int numTimes = 0;
			
				@Override
				public void run() {
					if(numTimes <= 4) {
						try {
							transportLayer.sendToNetwork(addressProxy, portProxy, error);
							numTimes++;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}else {
						client = ClientStateUA.TERMINATED;
						task.cancel();
						task = null;
					}
				}
   	 		};
		
			timer.schedule(task, 200);
   	 	}
		
	}
	
	public void cancelTimer() {
		if(task != null) {
			task.cancel();
			task = null;
		}
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
				
				if (message instanceof OKMessage || message instanceof NotFoundMessage) {
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
