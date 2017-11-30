
import layers.*;
import mensajesSIP.*;

public class UserLayerUA extends UserLayer{
	
	public static final int OK = 0;
	public static final int CALL_IN_PROGRESS = 1;
	public static final int NO_CALL = 2;

	private boolean callInProgress;
	private boolean isRinging;
	
	public UserLayerUA() {
		super();
		this.callInProgress = false;
		this.isRinging = false;
	}

	@Override
	public void recvFromTransaction(SIPMessage message) {
		if(message instanceof InviteMessage) {
			RingingMessage ringingMessage = (RingingMessage) SIPMessage.createResponse(SIPMessage._180_RINGING, message);
			((TransactionLayerUA)transactionLayer).recvFromUser(ringingMessage);
			OKMessage ok = (OKMessage) SIPMessage.createResponse(SIPMessage._200_OK, message);
			((TransactionLayerUA)transactionLayer).recvFromUser(ok);
			isRinging = true;
			callInProgress = true;
		}else if(message instanceof OKMessage) {
			//send ACK
			notifyUI();
		}
		
	}
	
	public int userInput(String input) {
		String[] command = input.split(" ");
		if(command.length == 2 && command[0].equals("INVITE")) {
			if(callInProgress) {
				return CALL_IN_PROGRESS;
			}else {
				InviteMessage invite = UA.createInvite(command[1]);
				((TransactionLayerUA)transactionLayer).recvFromUser(invite);
				callInProgress = true;
				return OK;
			}
		}else if(command.length == 1){
			if(command[0].equals("BYE")) {
				if(callInProgress && !isRinging) {
					return OK;
				}else if(!callInProgress) {
					return NO_CALL;
				}	
			}else if(command[0].equals("S") && isRinging) {
				return OK;
			}else if(command[0].equals("N") && isRinging) {
				return OK;
			}
		}
		
		return -1;
	
	}
	
	private void notifyUI() {
		
	}

}