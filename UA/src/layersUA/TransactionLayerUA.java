package layersUA;

import java.io.IOException;
import java.net.InetAddress;

import fsm.*;
import layers.*;
import mensajesSIP.*;
import ua.UA;

public class TransactionLayerUA extends TransactionLayer{

	public TransactionLayerUA() {
		super();
		this.currentTransaction = Transaction.NO_TRANSACTION;
		this.callId = null;
		this.destination =  UA.createURI("proxy");
		this.myVias = UA.getMyVias();
		this.client = new ClientFSM(this);
		this.server = new ServerFSM(this) {
			
			@Override
			public void onInvite(InviteMessage invite) {
				transactionLayer.sendToUser(invite);	
			}
		};
	}
	

	@Override
	public void resetLayer() {
		if(client.isTerminated() && server.isTerminated() && (currentTransaction == Transaction.INVITE_TRANSACTION)) {
			currentTransaction = Transaction.NO_TRANSACTION;
			callId = null;
		}
	}
	

	@Override
	public void recvFromTransport(SIPMessage message) {
		
		switch (currentTransaction) {
		
			case INVITE_TRANSACTION:
				
				if(message instanceof ACKMessage) {
					server.processMessage(message);
				}else {
					client.processMessage(message);
				}
				
				
				break;
				
			case NO_TRANSACTION:
				
				if(message instanceof InviteMessage) {
					currentTransaction = Transaction.INVITE_TRANSACTION;
					callId = message.getCallId();
					server.processMessage(message);
				}else{
					ul.recvFromTransaction(message);
				}
				
				break;
				
			default:
				break;
				
		}
		
	}

	
	@Override
	public void recvRequestFromUser(SIPMessage request) {
		
		if(request instanceof InviteMessage) {
			currentTransaction = Transaction.INVITE_TRANSACTION;
			callId = ((InviteMessage)request).getCallId();
			client.processMessage(request);
		}else if(request instanceof RegisterMessage) {
			try {
				transportLayer.sendToNetwork(requestAddress, requestPort, request);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	
	public void recvSessionRequestFromUser(SIPMessage request, InetAddress sessionAddress, int sessionPort) {
		try {
			transportLayer.sendToNetwork(sessionAddress, sessionPort, request);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
