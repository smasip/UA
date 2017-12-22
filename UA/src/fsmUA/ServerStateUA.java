package fsmUA;

import java.io.IOException;

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
import mensajesSIP.TryingMessage;

public enum ServerStateUA {
	
	
	PROCEEDING{
		@Override
		public ServerStateUA processMessage(SIPMessage message, TransactionLayer tl) {
			
			if (message instanceof InviteMessage) {
				System.out.println("PROCEEDING -> PROCEEDING");
				tl.sendToUser(message);
				return this;
			}else if (message instanceof RingingMessage) {
				System.out.println("PROCEEDING -> PROCEEDING");
				try {
					((TransactionLayerUA) tl).sendToTransport(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return this;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) 
			{
				System.out.println("PROCEEDING -> COMPLETED");
				tl.sendError(message);
				return COMPLETED;
			}else if (message instanceof OKMessage) {
				System.out.println("PROCEEDING -> TERMINATED");
				try {
					((TransactionLayerUA) tl).setCurrentTransaction(Transaction.ACK_TRANSACTION);
					((TransactionLayerUA) tl).sendToTransport(message);
					return TERMINATED;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return this;
		}

	},
	COMPLETED{
		@Override
		public ServerStateUA processMessage(SIPMessage message, TransactionLayer tl) {
			
			if(message instanceof ACKMessage) {
				System.out.println("COMPLETED -> TERMINATED");
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
			return this;
		}
		
	};
	
	public abstract ServerStateUA processMessage(SIPMessage message, TransactionLayer tl);


}
