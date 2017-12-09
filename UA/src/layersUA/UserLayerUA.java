package layersUA;

import java.util.Timer;
import java.util.TimerTask;

import ua.*;
import layers.*;
import mensajesSIP.*;

public class UserLayerUA extends UserLayer{
	
//	public static final int OK = 0;
//	public static final int CALL_IN_PROGRESS = 1;
//	public static final int NO_CALL = 2;
	
	public static enum Result { OK, CALL_IN_PROGRESS, NO_CALL, INVALID_CMD, REGISTERING }
	
	
	private boolean callInProgress;
	private boolean isRinging;
	private Transaction currentTrasaction;
	private Timer timer;
	private TimerTask task;
	private SIPMessage inboudInvite;
	
	
	
	public UserLayerUA() {
		super();
		this.callInProgress = false;
		this.isRinging = false;
		this.currentTrasaction = Transaction.REGISTER_TRANSACTION;
		this.timer = new Timer();
	}

	@Override
	public void recvFromTransaction(SIPMessage message) {
		
		switch (currentTrasaction) {
			case REGISTER_TRANSACTION:
				if(message instanceof OKMessage) {
					currentTrasaction = Transaction.NO_TRANSACTION;
					if (task != null) {
						task.cancel();
						task = null;
					}
				}
				
				break;
			case INVITE_TRANSACTION:
				if(message instanceof OKMessage) {
					System.out.println("Successful call!!!");
					currentTrasaction = Transaction.NO_TRANSACTION;
					callInProgress = false;
				}
				
				break;
			case NO_TRANSACTION:
				if(message instanceof InviteMessage) {
					System.out.println("Ringing. Please hit S to accept or N to reject ...");
					isRinging = true;
					callInProgress = true;
					currentTrasaction = Transaction.INVITE_TRANSACTION;
					inboudInvite = (InviteMessage) message;
					task = new TimerTask() {
						
						@Override
						public void run() {
							
							if(isRinging) {
								RingingMessage ringing = (RingingMessage) SIPMessage.createResponse(SIPMessage._180_RINGING, inboudInvite);
								((TransactionLayerUA)transactionLayer).recvFromUser(ringing);
							}
							
						}
					};
					timer.schedule(task, 0, 10000);
					
				}
				
				break;
	
			default:
				break;
		}
//		if(message instanceof InviteMessage) {
//			RingingMessage ringingMessage = (RingingMessage) SIPMessage.createResponse(SIPMessage._180_RINGING, message);
//			((TransactionLayerUA)transactionLayer).recvFromUser(ringingMessage);
//			OKMessage ok = (OKMessage) SIPMessage.createResponse(SIPMessage._200_OK, message);
//			((TransactionLayerUA)transactionLayer).recvFromUser(ok);
//			isRinging = true;
//			callInProgress = true;
//		}else if(message instanceof OKMessage) {
//			//send ACK
//			if (task != null) {
//			task.cancel();
//			task = null;
//			}
//			
//			notifyUI();
//		}
		
	}
	
	public Result userInput(String input) {
		
		if (currentTrasaction == Transaction.REGISTER_TRANSACTION) {
			return Result.REGISTERING;
		}
		
		String[] command = input.split(" ");
		if(command.length == 2 && command[0].equals("INVITE")) {
			if(callInProgress) {
				return Result.CALL_IN_PROGRESS;
			}else {
				InviteMessage invite = UA.createInvite(command[1]);
				((TransactionLayerUA)transactionLayer).recvFromUser(invite);
				callInProgress = true;
				currentTrasaction = Transaction.INVITE_TRANSACTION;
				return Result.OK;
			}
		}else if(command.length == 1){
			if(command[0].equals("BYE")) {
				if(callInProgress && !isRinging) {
					return Result.OK;
				}else if(!callInProgress) {
					return Result.NO_CALL;
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
				return Result.OK;
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
				return Result.OK;
			}
		}
		
		return Result.INVALID_CMD;
	
	}
	
	public void register() {
		task = new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				RegisterMessage register = UA.createRegister();
				((TransactionLayerUA)transactionLayer).recvFromUser(register);
				//task = null;
			}
		};
		
		timer.schedule(task, 0, 1000);	
	}
	
	private void notifyUI() {
		
	}

}