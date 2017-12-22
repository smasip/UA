package layersUA;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import ua.*;
import layers.*;
import mensajesSIP.*;

public class UserLayerUA extends UserLayer{
	
	
	private boolean callInProgress;
	private boolean isRinging;
	private Transaction currentTrasaction;
	private Timer timer;
	private TimerTask task;
	private InviteMessage inboudInvite;
	private String callId;
	private String toURI;
	private String destination;
	private String route;
	private boolean successfulRegister;
	
	
	public UserLayerUA() {
		super();
		this.callInProgress = false;
		this.isRinging = false;
		this.currentTrasaction = Transaction.REGISTER_TRANSACTION;
		this.inboudInvite = null;
		this.callId = null;
		this.toURI = null;
		this.destination = null;
		this.route = null;
		this.successfulRegister = false;
		this.timer = new Timer();
	}


	@Override
	public synchronized void recvFromTransaction(SIPMessage message) {
		
		switch (currentTrasaction) {
		
			case REGISTER_TRANSACTION:
			
				currentTrasaction = Transaction.NO_TRANSACTION;
				if (task != null) {
					task.cancel();
					task = null;
				}
				successfulRegister = (message instanceof OKMessage);
				notify();
				
				break;
				
			case INVITE_TRANSACTION:
				
				if(message instanceof OKMessage) {
	
					currentTrasaction = Transaction.NO_TRANSACTION;
					route = ((OKMessage)message).getRecordRoute();
					ACKMessage sessionACK = new ACKMessage();
					
					sessionACK.setDestination(destination);
					sessionACK.setVias(UA.getMyVias());
					sessionACK.setToUri(toURI);
					sessionACK.setFromUri(UA.getMyURI());
					sessionACK.setMaxForwards(70);
					sessionACK.setCallId(callId);
					sessionACK.setRoute(route);
					sessionACK.setcSeqNumber("2");
					sessionACK.setcSeqStr("ACK");
					sessionACK.setContentLength(0);
					
					if(route == null) {
						String[] s = ((OKMessage)message).getContact().split("@")[1].split(":");
						try {
							((TransactionLayerUA)transactionLayer).setSessionAddress(InetAddress.getByName(s[0]));
							((TransactionLayerUA)transactionLayer).setSessionPort(Integer.valueOf(s[1]));
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					((TransactionLayerUA)transactionLayer).recvFromUser(sessionACK);
					
				}else if(message instanceof BusyHereMessage) {
					
					currentTrasaction = Transaction.NO_TRANSACTION;
					callInProgress = false;
					inboudInvite = null;
					destination = null;
					callId = null;
					toURI = null;
					destination = null;
					route = null;
					
				}
				
				break;
				
			case NO_TRANSACTION:
				
				if(message instanceof InviteMessage) {
					
					System.out.println("Ringing !!! Please hit S to accept or N to reject ...");
					
					isRinging = true;
					callInProgress = true;
					currentTrasaction = Transaction.INVITE_TRANSACTION;
					inboudInvite = (InviteMessage) message;
					route = inboudInvite.getRecordRoute();
					callId = inboudInvite.getCallId();
					toURI = inboudInvite.getToUri();
					destination = inboudInvite.getDestination();
					
					task = new TimerTask() {
						
						int numTimes = 0;
						
						@Override
						public void run() {
							
							if(isRinging) {
								
								if(numTimes < 5) {
									RingingMessage ringing = (RingingMessage) SIPMessage.createResponse(
											SIPMessage._180_RINGING, inboudInvite, UA.getContact());
									((TransactionLayerUA)transactionLayer).recvFromUser(ringing);
									numTimes++;
								}else {
									
									RequestTimeoutMessage requestTimeout = (RequestTimeoutMessage) SIPMessage.createResponse(
											SIPMessage._408_REQUEST_TIMEOUT, inboudInvite, UA.getContact());
									
									isRinging = false;
									callInProgress = false;
									currentTrasaction = Transaction.NO_TRANSACTION;
									inboudInvite = null;
									callId = null;
									toURI = null;
									destination = null;
									route = null;
									task.cancel();
									task = null;	
									
									((TransactionLayerUA)transactionLayer).recvFromUser(requestTimeout);
									
								}
								
							}
							
						}
						
					};
					
					timer.schedule(task, 0, 2000);
					
				}
				
				break;
	
			default:
				break;
		}
		
	}
	
	public void userInput(String input) {
		
		String[] command = input.split(" ");
		InviteMessage outgoingInvite;
		
		if(command.length == 2 && command[0].equals("INVITE")) {
			
			if(callInProgress) {
				System.out.println("Command failed. A call is already in progress ...");
			}else {
				
				System.out.println("Calling ...");
				callInProgress = true;
				currentTrasaction = Transaction.INVITE_TRANSACTION;
				outgoingInvite = UA.createInvite(command[1]);
				callId = outgoingInvite.getCallId();
				toURI = outgoingInvite.getToUri();
				
				((TransactionLayerUA)transactionLayer).recvFromUser(outgoingInvite);
				
			}
			
		}else if(command.length == 1){
			
			if(command[0].equals("BYE")) {
				
				if(callInProgress) {
					
					System.out.println("Hanging out ...");
					currentTrasaction = Transaction.BYE_TRANSACTION;
					ByeMessage bye = new ByeMessage();
					bye.setDestination(destination);
					bye.setVias(UA.getMyVias());
					bye.setFromUri(UA.getMyURI());
					bye.setToUri(toURI);
					bye.setCallId(callId);
					bye.setcSeqNumber("1");
					bye.setcSeqStr("BYE");
					bye.setContentLength(0);
					
					((TransactionLayerUA)transactionLayer).recvFromUser(bye);
					
				}else {
					System.out.println("Command failed. No call to terminate");
				}	
				
			}else if(command[0].equals("S") && isRinging) {
				
				isRinging = false;
				if (task != null) {
					task.cancel();
					task = null;
				}
				
				OKMessage ok = (OKMessage) SIPMessage.createResponse(
						SIPMessage._200_OK, inboudInvite, UA.getContact());
			
				currentTrasaction = Transaction.NO_TRANSACTION;
				inboudInvite = null;
				
				((TransactionLayerUA)transactionLayer).recvFromUser(ok);
				
			}else if(command[0].equals("N") && isRinging) {
				
				isRinging = false;
				if (task != null) {
					task.cancel();
					task = null;
				}
				
				BusyHereMessage busy = (BusyHereMessage) SIPMessage.createResponse(
						SIPMessage._486_BUSY_HERE, inboudInvite, UA.getContact());
				
				currentTrasaction = Transaction.NO_TRANSACTION;
				callInProgress = false;
				inboudInvite = null;
				callId = null;
				toURI = null;
				destination = null;
				route = null;
				
				((TransactionLayerUA)transactionLayer).recvFromUser(busy);
				
			}else{
				System.out.println("Please hit S to accept or N to reject ...");
			}
		}
		
	
	}
	
	public synchronized boolean register() {
		
		System.out.println("Trying to register. Please wait ...");
		
		task = new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				RegisterMessage register = UA.createRegister();
				((TransactionLayerUA)transactionLayer).recvFromUser(register);
			}
		};
		
		timer.schedule(task, 0, 2000);
		
		try {
			wait();
			return successfulRegister;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
		
	}
	

}