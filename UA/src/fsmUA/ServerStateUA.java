package fsmUA;

import layers.*;
import layersUA.TransactionLayerUA;
import mensajesSIP.ACKMessage;
import mensajesSIP.BusyHereMessage;
import mensajesSIP.InviteMessage;
import mensajesSIP.NotFoundMessage;
import mensajesSIP.OKMessage;
import mensajesSIP.ProxyAuthenticationMessage;
import mensajesSIP.RequestTimeoutMessage;
import mensajesSIP.RingingMessage;
import mensajesSIP.SIPMessage;
import mensajesSIP.ServiceUnavailableMessage;

public enum ServerStateUA {
	
	
	PROCEEDING{
		@Override
		public ServerStateUA processMessage(SIPMessage message, TransactionLayer tl) {
			
			if (message instanceof RingingMessage) {
				System.out.println("SERVER: PROCEEDING -> PROCEEDING");
				tl.sendResponse(message);
				return this;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) 
			{
				System.out.println("SERVER: PROCEEDING -> COMPLETED");
				tl.sendError(message);
				return COMPLETED;
			}else if (message instanceof OKMessage) {
				System.out.println("SERVER: PROCEEDING -> TERMINATED");
				((TransactionLayerUA) tl).setCurrentTransaction(Transaction.NO_TRANSACTION);
				tl.sendResponse(message);
				return TERMINATED;
			}
			return this;
		}

	},
	COMPLETED{
		@Override
		public ServerStateUA processMessage(SIPMessage message, TransactionLayer tl) {
			
			if(message instanceof ACKMessage) {
				System.out.println("SERVER: COMPLETED -> TERMINATED");
				((TransactionLayerUA) tl).setCurrentTransaction(Transaction.NO_TRANSACTION);
				tl.cancelTimer();
				return TERMINATED;
			}
			
			return this;
			
		}
		
	},
	TERMINATED{
		@Override
		public ServerStateUA processMessage(SIPMessage message, TransactionLayer tl) {
			if (message instanceof InviteMessage) {
				System.out.println("SERVER: TERMINATED -> PROCEEDING");
				tl.sendToUser(message);
				return PROCEEDING;
			}
			return this;
		}
		
	};
	
	public abstract ServerStateUA processMessage(SIPMessage message, TransactionLayer tl);


}
