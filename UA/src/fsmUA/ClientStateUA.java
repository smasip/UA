package fsmUA;

import java.io.IOException;

import layers.*;
import layersUA.TransactionLayerUA;
import mensajesSIP.*;

public enum ClientStateUA {
	CALLING {
		@Override
		public ClientStateUA processMessage(SIPMessage message, TransactionLayer tl) {
			System.out.println("CALLING");
			System.out.println(message.toStringMessage());
			if(message instanceof InviteMessage) {
				try {
					((TransactionLayerUA) tl).sendToTransport(message);
					return this;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if (message instanceof TryingMessage || 
					  message instanceof RingingMessage) {
				return PROCEEDING;
			}else if (message instanceof OKMessage) {
				tl.sendToUser(message);
				return TERMINATED;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) {
				return COMPLETED;
			}
			return this;
		}

	},
	PROCEEDING{
		@Override
		public ClientStateUA processMessage(SIPMessage message, TransactionLayer tl) {
			System.out.println("PROCEEDING");
			System.out.println(message.toStringMessage());
			if (message instanceof TryingMessage || 
			    message instanceof RingingMessage) {
				return this;
			}else if (message instanceof OKMessage) {
				tl.sendToUser(message);
				return TERMINATED;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) {
				// Falta send ACK y resp to TU
				return COMPLETED;
			}
			return this;
		}

	},
	COMPLETED{
		@Override
		public ClientStateUA processMessage(SIPMessage message, TransactionLayer tl) {
			return TERMINATED;
		}
		
	},
	TERMINATED{
		@Override
		public ClientStateUA processMessage(SIPMessage message, TransactionLayer tl) {
			System.out.println("TERMINATED");
			System.out.println(message.toStringMessage());
			return this;
		}
		
	};
	
	public abstract ClientStateUA processMessage(SIPMessage message, TransactionLayer tl);

}
