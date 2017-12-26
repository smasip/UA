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
import ua.UA;

public class TransactionLayerUA extends TransactionLayer{
	
	ClientStateUA client;
	ServerStateUA server;
	Transaction currentTransaction;
	private Timer timer;
	private TimerTask task;
	private String callId;
	
	public TransactionLayerUA() {
		super();
		this.client = ClientStateUA.TERMINATED;
		this.server = ServerStateUA.TERMINATED;
		this.currentTransaction = Transaction.NO_TRANSACTION;
		this.timer = new Timer();
		this.task = null;
		this.callId = null;
	}
	

	public void setCurrentTransaction(Transaction currentTransaction) {
		this.currentTransaction = currentTransaction;
		if(currentTransaction == Transaction.NO_TRANSACTION) {
			callId = null;
		}
	}

	public void sendACK(SIPMessage error) {
		
		ACKMessage ack = new ACKMessage();
   	 	
   	 	ack.setDestination("sip:proxy@dominio.es");
   	 	ack.setVias(UA.getMyVias());
   	 	ack.setCallId(callId);
	 	ack.setToUri(error.getToUri());
	 	ack.setFromUri(error.getFromUri());
	 	ack.setcSeqNumber("1");
	 	ack.setcSeqStr("ACK");
	 	
	 	sendRequest(ack);
   	 	
   	 	if(task == null) {
   	 		
   	 		ul.recvFromTransaction(error);
   	 		
   	 		task = new TimerTask() {
			
				@Override
				public void run() { 
					System.out.println("CLIENT: COMPLETED -> TERMINATED");
					client = ClientStateUA.TERMINATED;
					currentTransaction = Transaction.NO_TRANSACTION;
					callId = null;
					task.cancel();
					task = null;
				}
				
   	 		};
		
			timer.schedule(task, 1000);
   	 	}
   	 	
		
	}
	
	public void sendError(SIPMessage error) {
		
		if(task == null) {
			
   	 		task = new TimerTask() {
   	 			
   	 			int numTimes = 0;
			
				@Override
				public void run() {
					if(numTimes < 4) {
						sendResponse(error);
						numTimes++;
					}else {
						System.out.println("SERVER: COMPLETED -> TERMINATED");
						client = ClientStateUA.TERMINATED;
						currentTransaction = Transaction.NO_TRANSACTION;
						callId = null;
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
	

	@Override
	public void recvFromTransport(SIPMessage message) {
		
		switch (currentTransaction) {
		
			case REGISTER_TRANSACTION:
				
				if (message instanceof OKMessage || message instanceof NotFoundMessage) {
					currentTransaction = Transaction.NO_TRANSACTION;
					callId = null;
					ul.recvFromTransaction(message);
				}
				
				break;
		
			case INVITE_TRANSACTION:
				
				if(message instanceof ACKMessage) {
					server = server.processMessage(message, this);
				}else {
					client = client.processMessage(message, this);
				}
				
				break;
				
			case NO_TRANSACTION:
				
				if(message instanceof InviteMessage) {
					currentTransaction = Transaction.INVITE_TRANSACTION;
					callId = message.getCallId();
					server = server.processMessage(message, this);
				}else{
					ul.recvFromTransaction(message);
				}
				
				break;
				
			default:
				break;
				
		}
		
	}

	public void recvRequestFromUser(SIPMessage request, InetAddress requestAddress, int requestPort) {
		
		this.requestAddress = requestAddress;
		this.requestPort = requestPort;
		
		if(request instanceof InviteMessage) {
			currentTransaction = Transaction.INVITE_TRANSACTION;
			callId = ((InviteMessage)request).getCallId();
			client = client.processMessage(request, this);
		}else {
			sendRequest(request);
		}
		
	}
	
	public void recvResponseFromUser(SIPMessage response) {
		
		switch (currentTransaction) {
		
			case INVITE_TRANSACTION:
				server = server.processMessage(response, this);
				break;
				
			case NO_TRANSACTION:
				sendResponse(response);
				break;
				
			default:
				break;
		}
	}


}
