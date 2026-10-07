package es.um.redes.nanoFiles.tcp.client;

import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Cliente TCP para la transferencia de ficheros entre peers en NanoFiles.
 * Se encarga de conectar con un servidor, enviar y recibir mensajes binarios PeerMessage,
 * y gestionar la descarga de fragmentos de ficheros.
 */
public class NFConnector 
{
	// Socket TCP para la conexión con el servidor
	private Socket socket;
	// Dirección del servidor a la que se conecta el cliente
	private InetSocketAddress serverAddr;
	// Streams de entrada y salida para mensajes binarios
	private DataInputStream dis;
	private DataOutputStream dos;

	/**
     * Crea un nuevo cliente TCP y conecta con el servidor especificado.
     * Inicializa los streams de entrada y salida para el intercambio de mensajes binarios.
     * 
     * @param fserverAddr Dirección del servidor (IP y puerto)
     * @throws UnknownHostException Si la IP no es válida
     * @throws IOException Si hay error de E/S al conectar
     */
	public NFConnector(InetSocketAddress fserverAddr) throws UnknownHostException, IOException
	{
		serverAddr = fserverAddr;
		
		// Creamos el socket y conectamos el servidor
		socket = new Socket(serverAddr.getAddress(), serverAddr.getPort());
		
		// Creamos los streams de entrada y salida
		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());
	}

	/**
     * Método de test: envía un mensaje FILEREQUEST y espera la respuesta del servidor.
     * Imprime el resultado por pantalla.
     */
	public void test() 
	{
		// Enviamos y recibimos un mensaje PeerMessage
		try {
	        // Creamos un mensaje PeerMessage de tipo FILEREQUEST
	        PeerMessage msg = new PeerMessage(PeerMessageOps.OPCODE_FILEREQUEST, "0123456789abcdef0123"); // hash de ejemplo de 20 bytes (40 hex)
	        System.out.println("[NFConnector] Enviando mensaje FILEREQUEST...");
	        sendMessage(msg);

	        // Esperamos la respuesta del servidor
	        PeerMessage respuesta = receiveMessage();
	        System.out.println("[NFConnector] Respuesta recibida: " + PeerMessageOps.opcodeToOperation(respuesta.getOpcode()));

	        // Procesamos la respuesta según el código de operación
	        if (respuesta.getOpcode() == PeerMessageOps.OPCODE_FILEREQUEST_ACCEPTED) 
	        {
	            System.out.println("[NFConnector] Test OK: recibido FILEREQUEST_ACCEPTED");
	        } 
	        
	        else if (respuesta.getOpcode() == PeerMessageOps.OPCODE_FILE_NOT_FOUND) 
	        {
	            System.out.println("[NFConnector] Test FAIL: recibido FILE_NOT_FOUND");
	        } 
	        
	        else 
	        {
	            System.out.println("[NFConnector] Test FAIL: opcode inesperado (" + respuesta.getOpcode() + ")");
	        }
	    } catch (IOException e) {
	        System.err.println("[NFConnector] Error en test: " + e.getMessage());
	    }
	}

	/**
     * Devuelve la dirección del servidor al que está conectado este cliente.
     */
	public InetSocketAddress getServerAddr() 
	{
		return serverAddr;
	}
	
	/**
     * Solicita y descarga un fragmento (chunk) de un fichero identificado por su hash.
     * 
     * @param fileHash Hash del fichero a descargar.
     * @param offset Desplazamiento inicial del chunk.
     * @param chunkSize Tamaño del chunk a descargar.
     * @return Array de bytes con el chunk descargado, o null si hay error.
     * @throws IOException Si ocurre un error de E/S
     */
	public byte[] downloadChunkByHash(String fileHash, long offset, int chunkSize) throws IOException 
	{
		PeerMessage request = new PeerMessage(PeerMessageOps.OPCODE_CHUNKREQUEST, offset, chunkSize);
	    request.setReqFileHash(fileHash);
	    sendMessage(request);

	    PeerMessage response = receiveMessage();
	    if (response.getOpcode() == PeerMessageOps.OPCODE_CHUNK) 
	    {
	        return response.getChunkData();
	    } 
	    
	    else if (response.getOpcode() == PeerMessageOps.OPCODE_FILE_NOT_FOUND) 
	    {
	        System.err.println("El servidor no tiene el fichero solicitado.");
	        return null;
	    } 
	    
	    else 
	    {
	        System.err.println("Respuesta inesperada del servidor: " + response.getOpcode());
	        return null;
	    }
	}

	/**
     * Envía un mensaje PeerMessage al servidor.
     * 
     * @param msg Mensaje a enviar
     * @throws IOException Si ocurre un error de E/S
     */
	public void sendMessage(PeerMessage msg) throws IOException 
	{
	    msg.writeMessageToOutputStream(dos);
	    dos.flush();
	}

	/**
     * Recibe un mensaje PeerMessage del servidor.
     * 
     * @return El mensaje recibido
     * @throws IOException Si ocurre un error de E/S
     */
	public PeerMessage receiveMessage() throws IOException 
	{
	    return PeerMessage.readMessageFromInputStream(dis);
	}

	/**
     * Cierra el socket y los streams asociados.
     */
	public void close() 
	{
	    try {
	        if (dis != null) dis.close();
	        if (dos != null) dos.close();
	        if (socket != null && !socket.isClosed()) socket.close();
	    } catch (IOException e) 
	    {
	    	System.err.println("Error al cerrar el socket: " + e.getMessage());
	    }
	}
}
