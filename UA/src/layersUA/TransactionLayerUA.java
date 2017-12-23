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
	
	InetAddress addressProxy;
	int portProxy;
	InetAddress sessionAddress;
	int sessionPort;
	ClientStateUA client;
	ServerStateUA server;
	Transaction currentTransaction;
	private Timer timer;
	private TimerTask task;
	boolean sessionInProgress;
	private String callId;
	
	public TransactionLayerUA() {
		super();
		this.client = ClientStateUA.TERMINATED;
		this.server = ServerStateUA.TERMINATED;
		this.currentTransaction = Transaction.REGISTER_TRANSACTION;
		this.timer = new Timer();
		this.task = null;
		this.sessionInProgress = false;
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
   	 	
   	 	ack.setDestination(UA.getProxyContact());
   	 	ack.setVias(UA.getMyVias());
   	 	ack.setCallId(callId);
	 	ack.setToUri(error.getToUri());
	 	ack.setFromUri(error.getFromUri());
	 	ack.setcSeqNumber("1");
	 	ack.setcSeqStr("ACK");
   	 	
   	 	if(task == null) {
   	 		task = new TimerTask() {
			
				@Override
				public void run() {
					client = ClientStateUA.TERMINATED;
					currentTransaction = Transaction.NO_TRANSACTION;
					task = null;
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
	
	public void setProxy(InetAddress addressProxy, int portProxy) {
		this.addressProxy = addressProxy;
		this.sessionAddress = addressProxy;
		this.portProxy = portProxy;
		this.sessionPort = portProxy;
	}

	public void setSessionAddress(InetAddress sessionAddress) {
		this.sessionAddress = sessionAddress;
	}

	public void setSessionPort(int sessionPort) {
		this.sessionPort = sessionPort;
	}

	@Override
	public void recvFromTransport(SIPMessage message) {
		
		if(callId != null) {
			if(!callId.equals(message.getCallId())) {
				return;
			}
		}
		
		switch (currentTransaction) {
		
			case REGISTER_TRANSACTION:
				
				if (message instanceof OKMessage || message instanceof NotFoundMessage) {
					currentTransaction = Transaction.NO_TRANSACTION;
					callId = null;
					ul.recvFromTransaction(message);
				}
				
				break;
		
			case INVITE_TRANSACTION:
				client = client.processMessage(message, this);
				break;
				
			case NO_TRANSACTION:
				
				if(message instanceof InviteMessage) {
					currentTransaction = Transaction.INVITE_TRANSACTION;
					callId = message.getCallId();
					server = ServerStateUA.PROCEEDING;
					server = server.processMessage(message, this);
				}else if(sessionInProgress && (message instanceof ByeMessage)) {
					currentTransaction = Transaction.BYE_TRANSACTION;
					ul.recvFromTransaction(message);
				}
				
				break;
				
			case ACK_TRANSACTION:
				if(message instanceof ACKMessage) {
					currentTransaction = Transaction.NO_TRANSACTION;
					sessionInProgress = true;
					ul.recvFromTransaction(message);
				}
				break;
				
			case BYE_TRANSACTION:
				if(message instanceof OKMessage) {
					currentTransaction = Transaction.NO_TRANSACTION;
					sessionInProgress = false;
					callId = null;
					sessionAddress = addressProxy;
					sessionPort = portProxy;
					ul.recvFromTransaction(message);
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
					callId = message.getCallId();
					transportLayer.sendToNetwork(addressProxy, portProxy, message);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				break;
			
			case INVITE_TRANSACTION:
				server = server.processMessage(message, this);
				break;
				
			case NO_TRANSACTION:
				
				if(message instanceof InviteMessage) {
					currentTransaction = Transaction.INVITE_TRANSACTION;
					callId = message.getCallId();
					client = ClientStateUA.CALLING;
					client = client.processMessage(message, this);
				}else if(sessionInProgress && (message instanceof ByeMessage)) {
					try {
						currentTransaction = Transaction.BYE_TRANSACTION;
						sessionInProgress = true;
						transportLayer.sendToNetwork(sessionAddress, sessionPort, message);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				break;
				
			case ACK_TRANSACTION:
				
				try {
					currentTransaction = Transaction.NO_TRANSACTION;
					sessionInProgress = true;
					transportLayer.sendToNetwork(sessionAddress, sessionPort, message);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				break;
				
			case BYE_TRANSACTION:
				
				try {
					currentTransaction = Transaction.NO_TRANSACTION;
					sessionInProgress = false;
					callId = null;
					transportLayer.sendToNetwork(sessionAddress, sessionPort, message);
					sessionAddress = addressProxy;
					sessionPort = portProxy;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
	}


}
