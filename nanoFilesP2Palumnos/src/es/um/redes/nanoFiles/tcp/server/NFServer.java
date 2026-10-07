package es.um.redes.nanoFiles.tcp.server;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileDatabase;
import es.um.redes.nanoFiles.util.FileInfo;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

/**
 * Clase que implementa el servidor TCP de ficheros de NanoFiles.
 * Permite a un peer actuar como servidor, aceptando conexiones de otros peers
 * para la descarga y subida de ficheros mediante el protocolo binario PeerMessage.
 * 
 * Soporta modo test (primer plano, un solo cliente) y modo concurrente (varios clientes).
 * Implementa la lógica de transferencia de ficheros por chunks.
 * Utiliza hilos para la concurrencia (NFServerThread).
 */
public class NFServer implements Runnable 
{
	// Puerto por defecto para el servidor de ficheros
	public static final int PORT = 10000;
	// Socket de servidor TCP
	private ServerSocket serverSocket = null;
	// Bandera para controlar si el servidor sigue aceptando conexiones
	private static boolean alive = true;
	// Lista de sockets de clientes conectados (para gestión y cierre ordenado)
	private LinkedList<Socket> sockets = new LinkedList<>();

	/**
     * Constructor por defecto: crea el servidor en el puerto 10000.
     */
	public NFServer() throws IOException 
	{
		// Creamos la dirección de socket con el puerto especificado
		InetSocketAddress serverSocketAddress = new InetSocketAddress(PORT);
		
		// Creamos el socket servidor
		serverSocket = new ServerSocket();
		
		// Ligamos el socket servidor a la dirección creada
		serverSocket.bind(serverSocketAddress);
	}

	/**
     * Constructor que permite especificar el puerto (para soporte de puertos efímeros).
     */
	public NFServer(int port) throws IOException 
	{
		// Creamos el socket servidor
		serverSocket = new ServerSocket();
		
		// Ligamos el socket servidor a la dirección creada
		serverSocket.bind(new InetSocketAddress(port));
	}
	
	/**
 	* Método para ejecutar el servidor de ficheros en primer plano. Atiende conexiones de un cliente a la vez,
 	* procesando cada cliente de forma secuencial. Una vez se lanza, ya no es posible interactuar con la aplicación.
 	*/
	public void test() 
	{
		if (serverSocket == null || !serverSocket.isBound()) 
		{
			System.err.println("[fileServerTestMode] Failed to run file server, server socket is null or not bound to any port");
			return;
		} 
		
		else 
		{
			System.out.println("[fileServerTestMode] NFServer running on " + serverSocket.getLocalSocketAddress() + ".");
		}

		while (true) 
		{
	        Socket clientSocket = null;
	        try {
	            // Esperamos la conexión de un cliente
	            clientSocket = serverSocket.accept();
	            System.out.println("[fileServerTestMode] Cliente conectado desde " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

	            // Delegamos la comunicación con el cliente
	            serveFilesToClient(clientSocket);

	            // Cerramos el socket del cliente al terminar
	            clientSocket.close();

	        } catch (IOException e) {
	            System.err.println("[fileServerTestMode] Error al aceptar o atender cliente: " + e.getMessage());
	            e.printStackTrace();
	            if (clientSocket != null && !clientSocket.isClosed()) 
	            {
	                try {
	                    clientSocket.close();
	                } catch (IOException ex) {
	                    System.err.println("[fileServerTestMode] Error al cerrar el socket del cliente: " + ex.getMessage());
	                }
	            }
	        }
	    }
	}

	/**
     * Método principal del servidor concurrente.
     * Acepta conexiones de clientes y lanza un hilo (NFServerThread) por cada cliente.
     * Permite atender a múltiples clientes simultáneamente.
     */
	public void run() 
	{
		System.out.println("[NFServer] Listening on port " + getPort());
        while (alive) 
        {
            try {
            	// Esperamos la conexión de un cliente
                Socket clientSocket = serverSocket.accept();
                sockets.add(clientSocket);
                // Lanzamos un hilo para atender al cliente
                NFServerThread clientThread = new NFServerThread(clientSocket);
                clientThread.start();
            } catch (IOException e) {
                break;
            }
        }
	}
	
	/**
     * Devuelve el puerto en el que está escuchando el servidor.
     */
	public int getPort() 
	{
	    if (serverSocket != null && serverSocket.isBound()) 
	    {
	        return serverSocket.getLocalPort();
	    }
	    return 0;
	}

	/**
     * Detiene el servidor cerrando el ServerSocket.
     */
	public void terminate() 
	{
	    try {
	        if (serverSocket != null && !serverSocket.isClosed()) 
	        {
	            serverSocket.close();
	        }
	    } catch (IOException e) {

	    }
	}

	/**
     * Indica si el servidor sigue activo.
     */
	public boolean isAlive() 
	{
	    return serverSocket != null && !serverSocket.isClosed();
	}
	

	/**
	 * Método de clase que implementa el extremo del servidor del protocolo de
	 * transferencia de ficheros entre pares.
	 * Gestiona la comunicación con un cliente usando PeerMessage.
	 * 
	 * @param socket El socket para la comunicación con un cliente que desea
	 *               descargar ficheros.
	 */
	public static void serveFilesToClient(Socket socket) 
	{
		try {
	        DataInputStream dis = new DataInputStream(socket.getInputStream());
	        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
	        FileInfo reqFileInfo = null;
	        FileInputStream fileStream = null;
	        RandomAccessFile uploadFile = null;

	        while (true) 
	        {
	        	// Leemos un mensaje del cliente
	            PeerMessage peerMessage = PeerMessage.readMessageFromInputStream(dis);
	            if (peerMessage == null) break;
	            PeerMessage messageToSend = null;

	            switch (peerMessage.getOpcode()) 
	            {
	                case PeerMessageOps.OPCODE_FILEREQUEST: 
	                {
	                	// Buscamos el fichero solicitado por hash
	                    var files = NanoFiles.db.getFiles();
	                    reqFileInfo = FileInfo.lookupHash(files, peerMessage.getReqFileHash());
	                    if (reqFileInfo == null) 
	                    {
	                        messageToSend = new PeerMessage(PeerMessageOps.OPCODE_FILE_NOT_FOUND);
	                        break;
	                    }
	                    File reqFile = new File(reqFileInfo.getPath());
	                    try {
	                        fileStream = new FileInputStream(reqFile);
	                    } catch (FileNotFoundException e) {
	                        messageToSend = new PeerMessage(PeerMessageOps.OPCODE_FILE_NOT_FOUND);
	                        break;
	                    }
	                    messageToSend = new PeerMessage(PeerMessageOps.OPCODE_FILEREQUEST_ACCEPTED);
	                    break;
	                } 
	                
	                case PeerMessageOps.OPCODE_CHUNKREQUEST: 
	                {
	                	// Enviamos un fragmento del fichero abierto
	                    if (fileStream == null) 
	                    {
	                        messageToSend = new PeerMessage(PeerMessageOps.OPCODE_CHUNKREQUEST_OUTOFRANGE);
	                        break;
	                    }
	                    byte[] chunk;
	                    try {
	                        fileStream.getChannel().position(peerMessage.getOffset());
	                        DataInputStream fdis = new DataInputStream(fileStream);
	                        chunk = fdis.readNBytes(peerMessage.getSize());
	                    } catch (IOException e) {
	                        messageToSend = new PeerMessage(PeerMessageOps.OPCODE_CHUNKREQUEST_OUTOFRANGE);
	                        break;
	                    }
	                    messageToSend = new PeerMessage(PeerMessageOps.OPCODE_CHUNK, peerMessage.getOffset(), chunk.length, chunk);
	                    break;
	                } 
	                
	                case PeerMessageOps.OPCODE_UPLOAD: 
	                {
	                	// 1. Comprobar+mos si ya existe el archivo (por hash)
	                    var files = NanoFiles.db.getFiles();
	                    if (FileInfo.lookupHash(files, peerMessage.getReqFileHash()) != null) 
	                    {
	                        messageToSend = new PeerMessage(PeerMessageOps.OPCODE_FILE_ALREADY_EXISTS);
	                        break;
	                    }
	                    // 2. Aceptamos la subida
	                    messageToSend = new PeerMessage(PeerMessageOps.OPCODE_FILEREQUEST_ACCEPTED);
	                    // El flujo continuará con FILENAME_TO_SAVE y luego los CHUNKs
	                    break;
	                }
	                
	                case PeerMessageOps.OPCODE_FILENAME_TO_SAVE: 
	                {
	                	// Preparamos el fichero para escritura (subida)
	                	try {
	                        uploadFile = new RandomAccessFile(NanoFiles.sharedDirname + "/" + peerMessage.getFileName(), "rw");
	                    } catch (FileNotFoundException e) {
	                        messageToSend = new PeerMessage(PeerMessageOps.OPCODE_FILE_NOT_FOUND);
	                        break;
	                    }
	                    messageToSend = new PeerMessage(PeerMessageOps.OPCODE_FILEREQUEST_ACCEPTED);
	                    break;
	                } 
	                
	                case PeerMessageOps.OPCODE_CHUNK: 
	                {
	                	// Escribimos un fragmento recibido en el fichero abierto para subida
	                	try {
	                        uploadFile.seek(peerMessage.getOffset());
	                        uploadFile.write(peerMessage.getChunkData());
	                    } catch (IOException e) {
	                    	System.err.println("[NFServer] Error al escribir chunk: " + e.getMessage());
	                    }
	                    break;
	                } 
	                
	                case PeerMessageOps.OPCODE_STOP: 
	                {
	                	// Finalizamos la subida, cerramos el fichero y actualizamos la base de datos local
	                	try {
	                        if (uploadFile != null) uploadFile.close();
	                        NanoFiles.db = new FileDatabase(NanoFiles.sharedDirname);
	                    } catch (IOException e) {}
	                    return;
	                }
	            }
	            
	            // Enviamos la respuesta al cliente si corresponde
	            if (messageToSend != null) 
	            {
	                messageToSend.writeMessageToOutputStream(dos);
	                dos.flush();
	            }
	        }
	        
	        if (fileStream != null) fileStream.close();
	    } catch (IOException e) {
	    	
	    }
	}
}