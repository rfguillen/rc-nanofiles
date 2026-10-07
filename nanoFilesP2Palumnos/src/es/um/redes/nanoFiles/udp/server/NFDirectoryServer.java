package es.um.redes.nanoFiles.udp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Servidor UDP que implementa el directorio de NanoFiles.
 */
public class NFDirectoryServer 
{
	/**
	 * Número de puerto UDP en el que escucha el directorio
	 */
	public static final int DIRECTORY_PORT = 6868;

	/**
	 * Socket de comunicación UDP con el cliente UDP (DirectoryConnector)
	 */
	private DatagramSocket socket = null;

	// Mapa que asocia direcciones de servidores con los archivos que comparten
	private HashMap<InetSocketAddress, LinkedList<FileInfo>> serverFiles;

	// Lista de todos los archivos publicados al directorio
	private LinkedList<FileInfo> publishedFiles;

	/**
	 * Probabilidad de descartar un mensaje recibido en el directorio (para simular
	 * enlace no confiable y testear el código de retransmisión)
	 */
	private double messageDiscardProbability;

	/**
     * Constructor: inicializa el socket y las estructuras de datos.
     * @param corruptionProbability Probabilidad de descartar mensajes
     */
	public NFDirectoryServer(double corruptionProbability) throws SocketException 
	{
		/*
		 * Guardar la probabilidad de pérdida de datagramas (simular enlace no
		 * confiable)
		 */
		messageDiscardProbability = corruptionProbability;
		/// Inicializamos el socket UDP en el puerto del directorio
		socket = new DatagramSocket(DIRECTORY_PORT);
		System.out.println("Directory server listening on socket address " + socket.getLocalSocketAddress());
    	
    	// Inicializamos las estructuras de datos del directorio
    	serverFiles = new HashMap<InetSocketAddress, LinkedList<FileInfo>>();
    	publishedFiles = new LinkedList<FileInfo>();
    	System.out.println("NFDirectoryServer initialized with message discard probability: " + messageDiscardProbability);

		if (NanoFiles.testModeUDP)
		{
			if (socket == null) 
			{
				System.err.println("[testMode] NFDirectoryServer: code not yet fully functional.\n" + "Check that all TODOs in its constructor and 'run' methods have been correctly addressed!");
				System.exit(-1);
			}
		}
	}

	/**
     * Espera y recibe un datagrama UDP, simulando la pérdida de mensajes según la probabilidad configurada.
     * @return DatagramPacket recibido del cliente 
     */
	public DatagramPacket receiveDatagram() throws IOException 
	{
		DatagramPacket datagramReceivedFromClient = null;
		boolean datagramReceived = false;
		while (!datagramReceived) 
		{
			// Preparamos el buffer y datagrama para recibir
	        byte[] recvBuf = new byte[DirMessage.PACKET_MAX_SIZE];
	        datagramReceivedFromClient = new DatagramPacket(recvBuf, recvBuf.length);
	        
	        // Recibimos el datagrama a través del socket
	        System.out.println("Directory waiting to receive datagram...");
	        socket.receive(datagramReceivedFromClient);
			
			// Simulamos la perdida de mensajes
			double rand = Math.random();
			if (rand < messageDiscardProbability) 
			{
				System.err.println("Directory ignored datagram from " + datagramReceivedFromClient.getSocketAddress());
			} 
				
			else 
			{
				datagramReceived = true;
				System.out.println("Directory received datagram from " + datagramReceivedFromClient.getSocketAddress()+ " of size " + datagramReceivedFromClient.getLength() + " bytes.");
			}
		}
		return datagramReceivedFromClient;
	}

	/**
     * Modo test: recibe dos datagramas y responde según el protocolo de pruebas.
     */
	public void runTest() throws IOException 
	{

		System.out.println("[testMode] Directory starting...");

		System.out.println("[testMode] Attempting to receive 'ping' message...");
		DatagramPacket rcvDatagram = receiveDatagram();
		sendResponseTestMode(rcvDatagram);

		System.out.println("[testMode] Attempting to receive 'ping&PROTOCOL_ID' message...");
		rcvDatagram = receiveDatagram();
		sendResponseTestMode(rcvDatagram);
	}

	/**
     * Responde a los mensajes de test ("ping", "ping&PROTOCOL_ID", etc.).
     */
	private void sendResponseTestMode(DatagramPacket pkt) throws IOException 
	{
	    String messageFromClient = new String(pkt.getData(), 0, pkt.getLength());
	    System.out.println("Data received: " + messageFromClient);

	    String responseMessage;
	    
	    if (messageFromClient.equals("ping")) 
	    {
	        // Caso 1: Mensaje exactamente "ping" -> responder "pingok"
	        responseMessage = "pingok";
	        System.out.println("Ping message received, responding with pingok");
	    } 
	    
	    else if (messageFromClient.startsWith("ping&")) 
	    {
	        // Caso 2: Mensaje que comienza por "ping&" -> extraer y comprobar PROTOCOL_ID
	        String receivedProtocolId = messageFromClient.substring(5); // "ping&" tiene 5 caracteres
	        
	        if (receivedProtocolId.equals(NanoFiles.PROTOCOL_ID)) 
	        {
	            // El ID del protocolo coincide
	            responseMessage = "welcome";
	            System.out.println("Protocol ID verified, responding with welcome");
	        } 
	        
	        else 
	        {
	            // El ID del protocolo no coincide
	            responseMessage = "denied";
	            System.out.println("Invalid protocol ID: " + receivedProtocolId + ", responding with denied");
	        }
	    } 
	    
	    else 
	    {
	        // Caso 3: Cualquier otro mensaje -> responder "invalid"
	        responseMessage = "invalid";
	        System.out.println("Invalid message received: " + messageFromClient);
	    }
	    
	    // Preparamos y enviamos la respuesta al cliente
	    byte[] responseData = responseMessage.getBytes();
	    InetSocketAddress clientAddr = (InetSocketAddress) pkt.getSocketAddress();
	    DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
	    socket.send(responsePacket);
	}

	/**
     * Bucle principal del servidor de directorio: recibe y responde a datagramas de clientes.
     */
	public void run() throws IOException
	{
		System.out.println("Directory starting...");

		while (true) 
		{ 
			DatagramPacket rcvDatagram = receiveDatagram();
			sendResponse(rcvDatagram);
		}
	}

	/**
     * Procesa un datagrama recibido, interpreta el mensaje y envía la respuesta adecuada.
     * Implementa la lógica del protocolo de mensajes field:value
     */
	private void sendResponse(DatagramPacket pkt) throws IOException 
	{
		// 1. Construimos el String a partir de los datos recibidos
		String messageFromClient = new String(pkt.getData(), 0, pkt.getLength());
		System.out.println("Data received: " + messageFromClient);
		
		// 2. Creamos el objeto DirMessage a partir del string recibido
		DirMessage receivedMessage = DirMessage.fromString(messageFromClient);
		
		// 3. Obtenemos la operación del mensaje
		String operation = receivedMessage.getOperation();
		
		// 4. Creamos el mensaje de respuesta
		DirMessage msgToSend = null;
		
		switch (operation)
		{
			case DirMessageOps.OPERATION_PING:
			{
				// El cliente envía su protocolId. Si coincide con el del sistema, respondemos welcome.
                // Si no, respondemos denied con mensaje de error.
				String clientProtocolId = receivedMessage.getProtocolId();
		        if (clientProtocolId.equals(NanoFiles.PROTOCOL_ID))
		        {
		            // Protocolo compatible: responde con welcome y status ok
		            msgToSend = new DirMessage(DirMessageOps.OPERATION_WELCOME);
		            msgToSend.setStatus(DirMessageOps.STATUS_OK);
		            System.out.println("Ping successful: Protocol ID verified for client " + pkt.getSocketAddress());
		        }
		        else
		        {
		            // Protocolo incompatible: responde con denied y mensaje de error
		            msgToSend = new DirMessage(DirMessageOps.OPERATION_DENIED);
		            msgToSend.setStatus(DirMessageOps.STATUS_ERROR);
		            msgToSend.setMessage("Incompatible protocol");
		            System.out.println("Ping failed: Invalid protocol ID " + clientProtocolId + " from client " + pkt.getSocketAddress());
		        }
		        break;
			}
			
			case DirMessageOps.OPERATION_GET_FILES: 
			{
				// Generación de la lista de archivos publicados
				// El directorio responde con todos los archivos publicados y los servidores que los comparten.
			    msgToSend = new DirMessage(DirMessageOps.OPERATION_FILE_LIST);
			    msgToSend.setStatus(DirMessageOps.STATUS_OK);

			    if (publishedFiles != null && !publishedFiles.isEmpty()) 
			    {
			        String[] files = new String[publishedFiles.size()];
			        for (int i = 0; i < publishedFiles.size(); i++) 
			        {
			            FileInfo f = publishedFiles.get(i);
			            // Buscamos servidores que comparten este fichero
			            StringBuilder servers = new StringBuilder();
			            for (InetSocketAddress addr : serverFiles.keySet()) 
			            {
			                LinkedList<FileInfo> filesOfServer = serverFiles.get(addr);
			                for (FileInfo sf : filesOfServer) 
			                {
			                    if (sf.fileHash.equals(f.fileHash)) 
			                    {
			                        if (servers.length() > 0) servers.append("|");
			                        servers.append(addr.getAddress().getHostAddress()).append(":").append(addr.getPort());
			                        break;
			                    }
			                }
			            }
			            // Formato: hash;nombre;tamaño;ip1:puerto1|ip2:puerto2
			            files[i] = String.format("%s;%s;%d;%s", f.fileHash, f.fileName, f.fileSize, servers.toString());
			        }
			        msgToSend.setFiles(files);
			    } 
			    
			    else 
			    {
			        msgToSend.setFiles(new String[0]);
			    }
			    System.out.println("File list requested, sending " + publishedFiles.size() + " files.");
			    break;
	        }

			case DirMessageOps.OPERATION_REGISTER_FILES:
			case DirMessageOps.OPERATION_SERVE: 
			{
				// Registro o baja de archivos de un servidor
				// Si files está vacío, es una baja: eliminamos el servidor y sus archivos si ya no los comparte nadie.
                // Si files tiene contenido, es un alta o actualización: añadimos/actualizamos los archivos publicados por ese servidor.
			    int tcpPort = receivedMessage.getServerPort();
			    InetSocketAddress tcpServerAddr = new InetSocketAddress(((InetSocketAddress) pkt.getSocketAddress()).getAddress(), tcpPort);
			    String[] files = receivedMessage.getFiles();

			    if (files == null || files.length == 0) 
			    {
					// Baja de servidor
			        // 1. Recuperanos la lista de ficheros de este servidor antes de eliminarlo
			        LinkedList<FileInfo> filesOfServer = serverFiles.get(tcpServerAddr);
			        
					if (filesOfServer != null)
					{
						 // 2. Para cada fichero, comprobamos si ya no lo comparte ningún otro servidor y lo eliminamos de los ficheros publicados
						 for (FileInfo file : filesOfServer)
						 {
							boolean sharedByOthers = false;
							for (InetSocketAddress otherAddr : serverFiles.keySet())
							{
								if (otherAddr.equals(tcpServerAddr)) continue;
								LinkedList<FileInfo> otherFiles = serverFiles.get(otherAddr);
								if (otherFiles != null)
								{
									for (FileInfo otherFile : otherFiles)
									{
										if (otherFile.fileHash.equals(file.fileHash))
										{
											sharedByOthers = true;
											break;
										}
									}
								}
								if (sharedByOthers) break;
							}
							if (!sharedByOthers)
							{
								publishedFiles.removeIf(f -> f.fileHash.equals(file.fileHash) && f.fileName.equals(file.fileName));
							}
						 }
					}

					// 3. Eliminamos el servidor
					serverFiles.remove(tcpServerAddr);

					msgToSend = new DirMessage(DirMessageOps.OPERATION_REGISTER_FILES);
					msgToSend.setStatus(DirMessageOps.STATUS_OK);
					System.out.println("Server unregistered: " + tcpServerAddr);
			    } 
			    else 
			    {
					// Alta o actualización de archivos de un servidor
			        LinkedList<FileInfo> fileList = new LinkedList<>();
			        for (String fileStr : files) 
			        {
			            String[] parts = fileStr.split(";");
			            if (parts.length == 3) 
			            {
			                String hash = parts[0];
			                String name = parts[1];
			                long size = Long.parseLong(parts[2]);
			                FileInfo fi = new FileInfo(hash, name, size, null);
			                fileList.add(fi);
			                boolean exists = false;
			                for (FileInfo pf : publishedFiles) 
			                {
			                    if (pf.fileHash.equals(hash) && pf.fileName.equals(name)) 
			                    {
			                        exists = true;
			                        break;
			                    }
			                }
			                if (!exists) publishedFiles.add(fi);
			            }
			        }
			        serverFiles.put(tcpServerAddr, fileList);
			        msgToSend = new DirMessage(DirMessageOps.OPERATION_REGISTER_FILES);
			        msgToSend.setStatus(DirMessageOps.STATUS_OK);
			        System.out.println("Server registered: " + tcpServerAddr + " with " + fileList.size() + " files.");
			    }
			    break;
	        }

	        case DirMessageOps.OPERATION_DOWNLOAD: 
	        {
	            // El directorio busca todos los servidores que comparten el archivo solicitado por nombre exacto.
	            String fileName = receivedMessage.getFileName();
	            LinkedList<InetSocketAddress> serversWithFile = new LinkedList<>();
	            for (InetSocketAddress addr : serverFiles.keySet()) 
	            {
	                LinkedList<FileInfo> files = serverFiles.get(addr);
	                for (FileInfo f : files) 
	                {
	                    if (f.fileName.equals(fileName)) 
	                    {
	                        serversWithFile.add(addr);
	                        break;
	                    }
	                }
	            }
	            msgToSend = new DirMessage(DirMessageOps.OPERATION_SERVER_LIST);
	            msgToSend.setStatus(DirMessageOps.STATUS_OK);
	            String[] serverList = new String[serversWithFile.size()];
	            for (int i = 0; i < serversWithFile.size(); i++) 
	            {
	                InetSocketAddress addr = serversWithFile.get(i);
	                serverList[i] = addr.getAddress().getHostAddress() + ":" + addr.getPort();
	            }
	            msgToSend.setServers(serverList);
	            System.out.println("Server list requested for file: " + fileName + ", found " + serversWithFile.size() + " servers.");
	            break;
	        }

	        default:
				// No se reconoce la operación
				System.err.println("Unexpected message operation: \"" + operation + "\"");
				System.exit(-1);
		}
		
		// 5. Convertimos el mensaje de respuesta a bytes y lo enviamos
		if (msgToSend != null)
		{
			// Convertimos el mensaje a string
			String responseString = msgToSend.toString();
			
			// Convertimos el string a bytes
			byte[] responseData = responseString.getBytes();
			
			// Creamos y enviamos el datagrama de respuesta
			DatagramPacket responsePacket = new DatagramPacket(
					responseData,
					responseData.length,
					pkt.getSocketAddress()
			);
			socket.send(responsePacket);
		}
	}
	
	/**
     * Método main: permite lanzar el servidor desde la línea de comandos
     */
	public static void main(String[] args) 
	{
	    double discardProbability = 0.0;
	    if (args.length == 2 && args[0].equals("-loss")) 
	    {
	        try {
	            discardProbability = Double.parseDouble(args[1]);
	        } catch (NumberFormatException e) {
	            System.err.println("El argumento debe ser un número (probabilidad de descarte entre 0.0 y 1.0).");
	            System.exit(1);
	        }
	    } 
	    
	    else if (args.length == 1) 
	    {
	        try {
	            discardProbability = Double.parseDouble(args[0]);
	        } catch (NumberFormatException e) {
	            System.err.println("El argumento debe ser un número (probabilidad de descarte entre 0.0 y 1.0).");
	            System.exit(1);
	        }
	    }
	    try {
	        NFDirectoryServer server = new NFDirectoryServer(discardProbability);
	        server.run();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
}
