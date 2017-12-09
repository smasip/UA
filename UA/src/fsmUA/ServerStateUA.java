package fsmUA;

import java.io.IOException;

import layers.*;
import layersUA.TransactionLayerUA;
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
			System.out.println("PROCEEDING");
			System.out.println(message.toStringMessage());
			if (message instanceof InviteMessage) {
				tl.sendToUser(message);
				return this;
			}else if (message instanceof RingingMessage) {
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
					  message instanceof ServiceUnavailableMessage) {
				// Falta send response
				return COMPLETED;
			}else if (message instanceof OKMessage) {
				try {
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
			return TERMINATED;
		}
		
	},
	TERMINATED{
		@Override
		public ServerStateUA processMessage(SIPMessage message, TransactionLayer tl) {
			System.out.println("TERMINATED");
			System.out.println(message.toStringMessage());
			return this;
		}
		
	};
	
	public abstract ServerStateUA processMessage(SIPMessage message, TransactionLayer tl);


}
