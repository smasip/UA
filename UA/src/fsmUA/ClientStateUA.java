package fsmUA;

import java.io.IOException;

import layers.*;
import layersUA.TransactionLayerUA;
import mensajesSIP.*;

public enum ClientStateUA {
	CALLING {
		@Override
		public ClientStateUA processMessage(SIPMessage message, TransactionLayer tl) {
		
			if(message instanceof InviteMessage) {
				System.out.println("CALLING -> CALLING");
				((TransactionLayerUA) tl).sendToTransportProxy(message);
				return this;
			}else if (message instanceof TryingMessage || message instanceof RingingMessage) {
				System.out.println("CALLING -> PROCEEDING");
				return PROCEEDING;
			}else if (message instanceof OKMessage) {
				System.out.println("CALLING -> TERMINATED");
				((TransactionLayerUA) tl).setCurrentTransaction(Transaction.ACK_TRANSACTION);
				tl.sendToUser(message);
				return TERMINATED;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) 
			{
				System.out.println("CALLING -> COMPLETED");
				tl.sendACK(message);
				return COMPLETED;
			}
			return this;
		}

	},
	PROCEEDING{
		@Override
		public ClientStateUA processMessage(SIPMessage message, TransactionLayer tl) {
		
			if (message instanceof TryingMessage || message instanceof RingingMessage) {
				System.out.println("PROCEEDING -> PROCEEDING");
				return this;
			}else if (message instanceof OKMessage) {
				System.out.println("PROCEEDING -> TERMINATED");
				((TransactionLayerUA)tl).setCurrentTransaction(Transaction.ACK_TRANSACTION);
				tl.sendToUser(message);
				return TERMINATED;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) 
			{
				System.out.println("PROCEEDING -> COMPLETED");
				tl.sendACK(message);
				return COMPLETED;
			}
			return this;
		}

	},
	COMPLETED{
		@Override
		public ClientStateUA processMessage(SIPMessage message, TransactionLayer tl) {
			if (message instanceof NotFoundMessage || 
				message instanceof ProxyAuthenticationMessage ||
				message instanceof RequestTimeoutMessage ||
				message instanceof BusyHereMessage ||
				message instanceof ServiceUnavailableMessage)
			{
				tl.sendACK(message);
			}
				return this;
		}
		
	},
	TERMINATED{
		@Override
		public ClientStateUA processMessage(SIPMessage message, TransactionLayer tl) {
			return this;
		}
		
	};
	
	public abstract ClientStateUA processMessage(SIPMessage message, TransactionLayer tl);

}
