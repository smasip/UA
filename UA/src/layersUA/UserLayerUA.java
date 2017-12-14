package layersUA;

import java.security.GeneralSecurityException;
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
	private InviteMessage outgoingInvite;
	private boolean successfulRegister;
	
	
	public UserLayerUA() {
		super();
		this.callInProgress = false;
		this.isRinging = false;
		this.currentTrasaction = Transaction.REGISTER_TRANSACTION;
		this.inboudInvite = null;
		this.outgoingInvite = null;
		this.timer = new Timer();
	}
	
	public InviteMessage getInboudInvite() {
		return inboudInvite;
	}


	public InviteMessage getOutgoingInvite() {
		return outgoingInvite;
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
					System.out.println("Successful call!!!");
					currentTrasaction = Transaction.NO_TRANSACTION;
					callInProgress = false;
				}else if(message instanceof BusyHereMessage) {
					System.out.println("Call rejected!!!");
					currentTrasaction = Transaction.NO_TRANSACTION;
					callInProgress = false;
				}
				
				break;
				
			case NO_TRANSACTION:
				
				if(message instanceof InviteMessage) {
					System.out.println("Ringing !!! Please hit S to accept or N to reject ...");
					isRinging = true;
					callInProgress = true;
					currentTrasaction = Transaction.INVITE_TRANSACTION;
					inboudInvite = (InviteMessage) message;
					
					task = new TimerTask() {
						
						int numTimes = 0;
						
						@Override
						public void run() {
							
							if(isRinging) {
								
								if(numTimes < 5) {
									RingingMessage ringing = (RingingMessage) 
											SIPMessage.createResponse(SIPMessage._180_RINGING, inboudInvite);
									((TransactionLayerUA)transactionLayer).recvFromUser(ringing);
									numTimes++;
								}else {
									RequestTimeoutMessage requestTimeout = (RequestTimeoutMessage) 
											SIPMessage.createResponse(SIPMessage._408_REQUEST_TIMEOUT, inboudInvite);
									((TransactionLayerUA)transactionLayer).recvFromUser(requestTimeout);
									isRinging = false;
									callInProgress = false;
									currentTrasaction = Transaction.NO_TRANSACTION;
									inboudInvite = null;
									task.cancel();
									task = null;
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
		if(command.length == 2 && command[0].equals("INVITE")) {
			if(callInProgress) {
				System.out.println("Command failed. A call is already in progress ...");
			}else {
				System.out.println("Calling ...");
				outgoingInvite = UA.createInvite(command[1]);
				((TransactionLayerUA)transactionLayer).recvFromUser(outgoingInvite);
				callInProgress = true;
				currentTrasaction = Transaction.INVITE_TRANSACTION;
			}
		}else if(command.length == 1){
			if(command[0].equals("BYE")) {
				if(callInProgress) {
					System.out.println("Hanging out ...");
				}else {
					System.out.println("Command failed. No call to terminate");
				}	
			}else if(command[0].equals("S") && isRinging) {
				isRinging = false;
				currentTrasaction = Transaction.NO_TRANSACTION;
				if (task != null) {
					task.cancel();
					task = null;
				}
				OKMessage ok = (OKMessage) SIPMessage.createResponse(SIPMessage._200_OK, inboudInvite);
				((TransactionLayerUA)transactionLayer).recvFromUser(ok);
				inboudInvite = null;
				System.out.println("Call accepted !!!");
			}else if(command[0].equals("N") && isRinging) {
				isRinging = false;
				currentTrasaction = Transaction.NO_TRANSACTION;
				if (task != null) {
					task.cancel();
					task = null;
				}
				BusyHereMessage busy = (BusyHereMessage) SIPMessage.createResponse(SIPMessage._486_BUSY_HERE, inboudInvite);
				((TransactionLayerUA)transactionLayer).recvFromUser(busy);
				inboudInvite = null;
				System.out.println("Call rejected !!!");
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
				//task = null;
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