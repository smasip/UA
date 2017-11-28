
import layers.*;
import mensajesSIP.*;

public class UserLayerUA extends UserLayer{
	

	@Override
	public void recvFromTransaction(SIPMessage message) {
		if(message instanceof InviteMessage) {
			RingingMessage ringingMessage = (RingingMessage) SIPMessage.createResponse(SIPMessage._180_RINGING, message);
			((TransactionLayerUA)transactionLayer).recvFromUser(ringingMessage);
			OKMessage ok = (OKMessage) SIPMessage.createResponse(SIPMessage._200_OK, message);
			((TransactionLayerUA)transactionLayer).recvFromUser(ok);
		}else if(message instanceof OKMessage) {
			//send ACK
			notifyUI();
		}
		
	}
	
	public void userInput(String input) {
		String[] command = input.split(" ");
		if(command.length == 2 && command[0].equals("INVITE")) {
			InviteMessage invite = UA.createInvite(command[1]);
			((TransactionLayerUA)transactionLayer).recvFromUser(invite);
		}
		
	}
	
	private void notifyUI() {
		
	}

}