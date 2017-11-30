import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Scanner;

import layers.TransactionLayerUA;

import java.net.InetAddress;
import java.net.ServerSocket;
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
		ServerSocket s = new ServerSocket(0);
		int registerPort = s.getLocalPort();
		s.close();
		DatagramSocket registerSocket = new DatagramSocket(registerPort);
		byte[] buf = new byte[1024];
		DatagramPacket registerPacket = new DatagramPacket(buf, buf.length, IPProxy, puertoEscuchaProxy);
		
		registerMessage.setDestination("sip:registrar@dominio.es");
		vias.add("localhost:" + Integer.toString(registerPort));
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
		
		registerPacket.setData(registerMessage.toStringMessage().getBytes());
		registerSocket.send(registerPacket);
		registerPacket.setData(buf, 0, buf.length);
		registerSocket.receive(registerPacket);
		SIPMessage message;
		try {
			message = SIPMessage.parseMessage(new String(registerPacket.getData()));
			if(message instanceof OKMessage) {
				((OKMessage) message).setSdp(null);
				System.out.println(message.toStringMessage());
			}
			
		} catch (SIPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		registerSocket.close();
		
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
	
	private static boolean checkInput(String input) {
		String[] s = input.split(" ");
		if(s.length == 2 && s[0].equals("INVITE")) {
			return true;
		}else if(s.length == 1 && 
				(s[0].equals("BYE") || s[0].equals("S") || s[0].equals("N"))) 
		{
			return true;
		}
		return false;
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
		
		ul = new UserLayerUA();
		transactionLayer = new TransactionLayerUA();
		transportLayer = new TransportLayer();
		
		ul.setTransactionLayer(transactionLayer);
		
		transactionLayer.setTransportLayer(transportLayer);
		transactionLayer.setUl(ul);
		transactionLayer.setpProxy(p);
		
		transportLayer.setTransactionLayer(transactionLayer);
		transportLayer.setDatagramSocket(datagramSocket);
		
		Thread t = new Thread() {
		    public void run() {
		    	while(true) {
		    		try {
						datagramSocket.receive(p);
						transportLayer.recvFromNetwork(p);
						p.setData(buf, 0, buf.length);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SIPException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    	}
		    }
		};
		t.start();
		
		try {
			register();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Register operation failed");
			System.exit(0);
		}
		
		Scanner reader = new Scanner(System.in);;
		int result;
		String input;
		while(true) {
			System.out.println("Enter a command: ");
			input = reader.nextLine();
			if(!checkInput(input)) {
				System.out.println("Invalid command. Please enter a valid command ... ");
				continue;
			}
			result = ul.userInput(input);
			switch (result) {
			case UserLayerUA.CALL_IN_PROGRESS:
				System.out.println("Command failed. A call is already in progress ...");
				break;
			case UserLayerUA.NO_CALL:
				System.out.println("Command failed. No call to terminate");
				break;
			case UserLayerUA.OK:
				break;
			default:
				System.out.println("Invalid command. Please enter a valid command ... ");
				break;
			}
	
		
		}

	}
}
	
