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
				try {
					System.out.println("CALLING -> CALLING");
					((TransactionLayerUA) tl).sendToTransport(message);
					return this;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if (message instanceof TryingMessage || 
					  message instanceof RingingMessage) {
				System.out.println("CALLING -> PROCEEDING");
				return PROCEEDING;
			}else if (message instanceof OKMessage) {
				System.out.println("CALLING -> TERMINATED");
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
		
			if (message instanceof TryingMessage || 
			    message instanceof RingingMessage) {
				System.out.println("PROCEEDING -> PROCEEDING");
				return this;
			}else if (message instanceof OKMessage) {
				System.out.println("PROCEEDING -> TERMINATED");
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
				System.out.println("COMPLETED -> TERMINATED");
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
