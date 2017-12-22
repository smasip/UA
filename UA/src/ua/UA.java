package ua;

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Scanner;

import layersUA.TransactionLayerUA;
import layersUA.TransportLayerUA;
import layersUA.UserLayerUA;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import mensajesSIP.*;
import layers.*;
import utils.*;

public class UA {
	
	private static DatagramSocket datagramSocket;
	public static InetAddress myAddress;
	private static InetAddress IPProxy;
	public static int puertoEscuchaUA;
	public static int puertoEscuchaProxy;
	private static boolean debug;
	public static String usuarioSIP;
	private static UserLayerUA ul;
	private static TransactionLayerUA transactionLayer;
	private static TransportLayer transportLayer;
	
	
	public static String getContact() {
		return usuarioSIP + "@" + myAddress.getHostAddress() + ":" + Integer.toString(puertoEscuchaUA);
	}
	
	public static String getProxyContact() {
		return  "proxy@" + IPProxy.getHostAddress() + ":" + Integer.toString(puertoEscuchaProxy);
	}
	
	public static ArrayList<String> getMyVias() {
		ArrayList<String> vias = new ArrayList<String>();
		vias.add(myAddress.getHostAddress() + ":" + Integer.toString(puertoEscuchaUA));
		return vias;
	}
	
	public static String getMyURI() {
		return createURI(usuarioSIP);
	}
	
	public static String createURI(String user) {
		return "sip:" + user + "@dominio.es";
	}
	
	public static RegisterMessage createRegister()
	{
		RegisterMessage registerMessage = new RegisterMessage();
		
		registerMessage.setDestination("sip:registrar@dominio.es");
		registerMessage.setVias(getMyVias());
		registerMessage.setMaxForwards(70);
		registerMessage.setToUri(getMyURI());
		registerMessage.setFromUri(getMyURI());
		registerMessage.setCallId(Integer.toString(123456789) + "@localhost");
		registerMessage.setcSeqNumber("1234");
		registerMessage.setcSeqStr("REGISTER");
		registerMessage.setContact(getContact());
		registerMessage.setExpires("7200");
		registerMessage.setContentLength(0);
		
		return registerMessage;
		
	}
	
	public static InviteMessage createInvite(String callee) {
		InviteMessage invite = new InviteMessage();
		SDPMessage sdp = new SDPMessage();
		
		invite.setDestination(createURI(callee));
		invite.setVias(getMyVias());
		invite.setMaxForwards(70);
		invite.setToUri(createURI(callee));
		invite.setFromUri(getMyURI());
		invite.setCallId(Integer.toString(123456789) + "@localhost");
		invite.setcSeqNumber("1234");
		invite.setcSeqStr("INVITE");
		invite.setContact(getContact());
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
	
	public static void  printlnDebugMessage(SIPMessage message) {
		String stringMessage;
		if(debug) {
			stringMessage = message.toStringMessage();
		}else {
			stringMessage = message.toStringMessage().split("\n")[0];
		}
		System.out.println(stringMessage);
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
		
//		try {
//			myAddress = Utils.getMyAddress();
//			System.out.println(myAddress.getHostAddress());
//		} catch (SocketException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		try {
			myAddress = InetAddress.getByName("localhost");
			System.out.println(myAddress.getHostAddress());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ul = new UserLayerUA();
		transactionLayer = new TransactionLayerUA();
		transportLayer = new TransportLayerUA();
		
		ul.setTransactionLayer(transactionLayer);
		
		transactionLayer.setTransportLayer(transportLayer);
		transactionLayer.setUl(ul);
		transactionLayer.setProxy(IPProxy, puertoEscuchaProxy);
		
		transportLayer.setTransactionLayer(transactionLayer);
		transportLayer.setDatagramSocket(datagramSocket);
		transportLayer.recvFromNetwork();
		
		if(ul.register()) {
			System.out.println("User successfully registered ...");
		}else {
			System.out.println("User not found ...");
			System.exit(0);
		}
		
		Scanner reader = new Scanner(System.in);
		String input;
		System.out.println("Enter a command:");
		while(true) {
			input = reader.nextLine();
			if(!checkInput(input)) {
				System.out.println("Invalid command. Please enter a valid command ... ");
				continue;
			}
			ul.userInput(input);
		}
	

	}
}
	
