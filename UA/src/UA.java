import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Scanner;

import layers.TransactionLayerUA;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import mensajesSIP.*;
import layers.*;

public class UA {
	
	private static DatagramSocket datagramSocket;
	private static InetAddress IPProxy;
	public static int puertoEscuchaUA;
	public static int puertoEscuchaProxy;
	private static boolean debug;
	public static String usuarioSIP;
	private static UserLayerUA ul;
	private static TransactionLayerUA transactionLayer;
	private static TransportLayer transportLayer;
	private static DatagramPacket p;
	
	
	private static void register() throws IOException
	{
		RegisterMessage registerMessage = new RegisterMessage();
		ArrayList<String> vias = new ArrayList<String>();
		//boolean keepTrying = true;
		
		registerMessage.setDestination("sip:registrar@dominio.es");
		vias.add("localhost:" + Integer.toString(puertoEscuchaUA));
		registerMessage.setVias(vias);
		registerMessage.setMaxForwards(70);
		registerMessage.setToUri("sip:" + usuarioSIP + "@dominio.es");
		registerMessage.setFromUri("sip:" + usuarioSIP + "@dominio.es");
		registerMessage.setCallId(Integer.toString(123456789) + "@localhost");
		registerMessage.setcSeqNumber("1234");
		registerMessage.setcSeqStr("REGISTER");
		registerMessage.setContact(usuarioSIP + "@localhost:" + Integer.toString(puertoEscuchaUA));
		registerMessage.setExpires("7200");
		registerMessage.setContentLength(0);
		
		p.setData(registerMessage.toStringMessage().getBytes());
		datagramSocket.send(p);
		datagramSocket.receive(p);
		SIPMessage message;
		try {
			message = SIPMessage.parseMessage(new String(p.getData()));
			if(message instanceof OKMessage) {
				((OKMessage) message).setSdp(null);
				System.out.println(message.toStringMessage());
			}
			
		} catch (SIPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static InviteMessage createInvite(String callee) {
		InviteMessage invite = new InviteMessage();
		SDPMessage sdp = new SDPMessage();
		
		invite.setDestination("sip:" + callee + "@dominio.es");
		ArrayList<String> vias = new ArrayList<String>();
		vias.add("localhost:" + Integer.toString(puertoEscuchaUA));
		invite.setVias(vias);
		invite.setMaxForwards(70);
		invite.setToUri("sip:" + callee + "@dominio.es");
		invite.setFromUri("sip:" + usuarioSIP + "@dominio.es");
		invite.setCallId(Integer.toString(123456789) + "@localhost");
		invite.setcSeqNumber("1234");
		invite.setcSeqStr("INVITE");
		invite.setContact(usuarioSIP + "@localhost:" + Integer.toString(puertoEscuchaUA));
		sdp.setIp("123.123.123.123");
		sdp.setPort(555);
		ArrayList<Integer> options = new ArrayList<Integer>();
		options.add(96);
		options.add(97);
		options.add(98);
		sdp.setOptions(options);
		invite.setContentType("application/sdp");
		invite.setContentLength(sdp.toStringMessage().getBytes().length);
		invite.setSdp(sdp);
		
		return invite;
	}

	public static void main(String[] args) {
		
		// TODO Auto-generated method stub
		if(args.length != 5) {
			System.out.println("Incorrect number of arguments. Received " + Integer.toString(args.length) + " expected 5");
			System.exit(0);
		}
		
		usuarioSIP = args[0];
		puertoEscuchaUA = Integer.valueOf(args[1]);
		try {
			IPProxy= InetAddress.getByName(args[2]);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Incorrect Proxy IP");
			System.exit(0);
		}
		puertoEscuchaProxy = Integer.valueOf(args[3]);
		debug = Boolean.getBoolean(args[4]);
		
		try {
			datagramSocket = new DatagramSocket(puertoEscuchaUA);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Creation of DatagramSocket failed");
			System.exit(0);
		}
		
		byte[] buf = new byte[1024];
		p = new DatagramPacket(buf, buf.length, IPProxy, puertoEscuchaProxy);
		
		try {
			register();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Register operation failed");
			System.exit(0);
		}
		
		
		ul = new UserLayerUA();
		transactionLayer = new TransactionLayerUA();
		transportLayer = new TransportLayer();
		
		ul.setTransactionLayer(transactionLayer);
		
		transactionLayer.setTransportLayer(transportLayer);
		transactionLayer.setUl(ul);
		transactionLayer.setpProxy(p);
		
		transportLayer.setTransactionLayer(transactionLayer);
		transportLayer.setDatagramSocket(datagramSocket);
		
		Scanner reader = new Scanner(System.in);
		System.out.println("Pulsa C para Client, S para Server: ");
		String input = reader.nextLine();
		p.setData(buf, 0, buf.length);
		if(input.equals("C")) {
			System.out.println("Sending an invite ...");
			ul.userInput("INVITE asdf2");
			try {
				datagramSocket.receive(p);
				transportLayer.recvFromNetwork(p);
				p.setData(buf, 0, buf.length);
				datagramSocket.receive(p);
				transportLayer.recvFromNetwork(p);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SIPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if(input.equals("S")) {
			try {
				System.out.println("Waiting for an invite ...");
				datagramSocket.receive(p);
				System.out.println(new String(p.getData()));
				transportLayer.recvFromNetwork(p);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SIPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
//		while(true) {
//			transaction = new ServerTransaction(datagramSocket, p, ul);
//			System.out.println("Enter a command: ");
//			String input = reader.nextLine();
//			ul.userInput(input);
//			transaction = ul.getTransaction();
//		}
		
		
		

	}

}
