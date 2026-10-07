package es.um.redes.nanoFiles.logic;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.tcp.client.NFConnector;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.tcp.server.NFServer;
import es.um.redes.nanoFiles.util.FileDigest;
import es.um.redes.nanoFiles.util.FileInfo;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class NFControllerLogicP2P 
{
	// Servidor de ficheros de este peer
	private NFServer fileServer = null;
	// Hilo en el que se ejecuta el servidor de ficheros
	private Thread serverThread = null;
	
	// Tamaño de chunk por defecto
	public final int CHUNK_SIZE = 1024; // 1 KB

	/**
     * Constructor por defecto.
     * Inicializa el controlador de lógica P2P.
     */
	protected NFControllerLogicP2P() {}

	/**
     * Lanza el servidor de ficheros en segundo plano, en un nuevo hilo.
     * Si ya hay un servidor en marcha, no hace nada.
     * 
     * @param port Puerto TCP en el que escuchar.
     * @return true si el servidor se ha lanzado correctamente, false si hay error.
     */
	public boolean startFileServer(int port) 
	{
		/*
		 * Comprobar que no existe ya un objeto NFServer previamente creado, en cuyo
		 * caso el servidor ya está en marcha.
		 */
		if (fileServer != null) 
		{
	        System.err.println("File server is already running");
	        return true;
	    }
		
	    try {
	        fileServer = new NFServer(port);
	    } catch (IOException e) {
	        System.out.println("Unable to bind socket");
	        return false;
	    }
	    serverThread = new Thread(fileServer);
	    serverThread.start();
	    return true;
	}

	
	/**
     * Modo test TCP: lanza el servidor en primer plano y atiende una única conexión.
     * Solo se usa si NanoFiles.testModeTCP == true.
     */
	protected void testTCPServer() 
	{
		assert (NanoFiles.testModeTCP);
		/*
		 * Comprobar que no existe ya un objeto NFServer previamente creado, en cuyo
		 * caso el servidor ya está en marcha.
		 */
		assert (fileServer == null);
		try {

			fileServer = new NFServer();
			/*
			 * (Boletín SocketsTCP) Inicialmente, se creará un NFServer y se ejecutará su
			 * método "test" (servidor minimalista en primer plano, que sólo puede atender a
			 * un cliente conectado). Posteriormente, se desactivará "testModeTCP" para
			 * implementar un servidor en segundo plano, que se ejecute en un hilo
			 * secundario para permitir que este hilo (principal) siga procesando comandos
			 * introducidos mediante el shell.
			 */
			fileServer.test();
		} catch (IOException e1) {
			e1.printStackTrace();
			System.err.println("Cannot start the file server");
			fileServer = null;
		}
	}

	/**
     * Modo test TCP: lanza el cliente TCP y prueba la comunicación con el servidor.
     * Solo se usa si NanoFiles.testModeTCP == true.
     */
	public void testTCPClient() 
	{
		assert (NanoFiles.testModeTCP);
		/*
		 * (Boletín SocketsTCP) Inicialmente, se creará un NFConnector (cliente TCP)
		 * para conectarse a un servidor que esté escuchando en la misma máquina y un
		 * puerto fijo. Después, se ejecutará el método "test" para comprobar la
		 * comunicación mediante el socket TCP. Posteriormente, se desactivará
		 * "testModeTCP" para implementar la descarga de un fichero desde múltiples
		 * servidores.
		 */

		try {
			NFConnector nfConnector = new NFConnector(new InetSocketAddress(NFServer.PORT));
			nfConnector.test();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
     * Descarga un fichero de uno o varios peers servidores.
     * 
     * 1. Comprueba que hay servidores disponibles.
     * 2. Crea el fichero local donde se guardará la descarga.
     * 3. Divide la descarga en chunks y los solicita secuencialmente a los servidores disponibles.
     * 4. Verifica la integridad del fichero descargado comparando el hash.
     * 5. Cierra las conexiones y muestra estadísticas.
     * 
     * @param serverAddressList Lista de direcciones de los servidores que comparten el fichero.
     * @param fileInfo Metadatos del fichero a descargar (nombre, tamaño, hash).
     * @param localFileName Nombre local para guardar el fichero descargado.
     * @return true si la descarga fue exitosa e íntegra, false en caso contrario.
     */
	protected boolean downloadFileFromServers(InetSocketAddress[] serverAddressList, FileInfo fileInfo, String localFileName) 
	{
		if (serverAddressList.length == 0)
		{
            System.err.println("* Cannot start download - No list of server addresses provided");
            return false;
        }
		
        String filePath = NanoFiles.sharedDirname + "/" + localFileName;
        java.io.File localFile = new java.io.File(filePath);
        if (localFile.exists()) 
        {
            System.err.println("* Cannot download: file \"" + localFileName + "\" already exists locally.");
            return false;
        }
        
        long fileSize = fileInfo.fileSize;
        int chunkSize = CHUNK_SIZE;
        int chunks = (int) (fileSize / chunkSize + (fileSize % chunkSize == 0 ? 0 : 1));
        ArrayList<NFConnector> connectors = new ArrayList<>();
        HashMap<NFConnector, Integer> chunkCounter = new HashMap<>();
        // 1. Conectamos a todos los servidores y negociamos la descarga
        for (InetSocketAddress addr : serverAddressList) 
        {
            try {
                NFConnector nfc = new NFConnector(addr);
                PeerMessage filereq = new PeerMessage(PeerMessageOps.OPCODE_FILEREQUEST, fileInfo.fileHash);
                nfc.sendMessage(filereq);
                PeerMessage resp = nfc.receiveMessage();
                if (resp.getOpcode() == PeerMessageOps.OPCODE_FILEREQUEST_ACCEPTED) 
                {
                    connectors.add(nfc);
                    chunkCounter.put(nfc, 0);
                } 
                
                else 
                {
                    nfc.close();
                }
            } catch (IOException e) {
                System.out.println("Could not contact peer " + addr);
            }
        }
        
        if (connectors.isEmpty()) 
        {
            System.out.println("No peers left - stopping");
            return false;
        }
        
        // 2. Descargamos los chunks secuencialmente, repartiendo la carga entre los servidores disponibles
        try (RandomAccessFile raf = new RandomAccessFile(localFile, "rw")) {
            for (int chunk = 0; chunk < chunks;) {
                NFConnector conn = connectors.get(chunk % connectors.size());
                long offset = (long) chunk * chunkSize;
                int thisChunkSize = (chunk == chunks - 1) ? (int) (fileSize - offset) : chunkSize;
                // Solicitamos el chunk al servidor que corresponde: incluye hash, offset y tamaño
                PeerMessage chunkReq = new PeerMessage(PeerMessageOps.OPCODE_CHUNKREQUEST, offset, thisChunkSize);
                chunkReq.setReqFileHash(fileInfo.fileHash);
                conn.sendMessage(chunkReq);
                PeerMessage resp = conn.receiveMessage();
                if (resp.getOpcode() == PeerMessageOps.OPCODE_CHUNK) 
                {
                    raf.seek(offset);
                    raf.write(resp.getChunkData());
                    chunk++;
                    chunkCounter.put(conn, chunkCounter.get(conn) + 1);
                } 
                
                else 
                {
                    System.out.println("Peer had an error or bad offset");
                    connectors.remove(conn);
                    if (connectors.isEmpty()) break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error during file download: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        // 3. Verificamos la integridad del fichero descargado comparando el hash
        String localHash = FileDigest.computeFileChecksumString(filePath);
        if (!localHash.equals(fileInfo.fileHash)) 
        {
            System.out.println("File corrupted.");
            return false;
        }
        
        // 4. Cerramos las conexiones y mostramos las estadísticas
        // Estadísticas: dirección de cada peer que ha participado en la descarga y el número de chunks que ese peer ha enviado
        System.out.println("File downloaded.");
        for (NFConnector conn : connectors) 
        {
            System.out.println(conn.getServerAddr() + "\t" + chunkCounter.get(conn));
            conn.close();
        }
        return true;
	}

	/**
     * Devuelve el puerto TCP en el que está escuchando el servidor de ficheros.
     * @return Puerto de escucha, o 0 si el servidor no está activo.
     */
	protected int getServerPort() 
	{
		if (fileServer != null) 
		{
	        return fileServer.getPort();
	    }
	    return 0;
	}

	/**
     * Detiene el servidor de ficheros en segundo plano, liberando el puerto y el hilo.
     * Si el servidor no está activo, no hace nada.
     */
	public void stopFileServer() 
	{
		if (fileServer != null) 
		{
	        fileServer.terminate();
	        fileServer = null;
	        serverThread = null;
	        System.out.println("File server stopped.");
	    }
	}

	/**
     * Indica si el servidor de ficheros está activo y escuchando.
     * @return true si el servidor está activo, false en caso contrario.
     */
	protected boolean serving() 
	{
		return fileServer != null && fileServer.isAlive();

	}

	/**
     * Sube un fichero local a otro peer servidor.
     * 
     * 1. Valida el formato de la dirección destino (hostname:puerto).
     * 2. Conecta al servidor remoto.
     * 3. Solicita la subida por hash.
     * 4. Envía el nombre de fichero.
     * 5. Envía los chunks del fichero.
     * 6. Cierra la conexión y muestra el resultado.
     * 
     * @param matchingFile Fichero local a subir (FileInfo).
     * @param uploadToServer Dirección del servidor destino (hostname:puerto).
     * @return true si la subida fue exitosa, false en caso contrario.
     */
	public boolean uploadFileToServer(FileInfo matchingFile, String uploadToServer) 
	{
		if (!uploadToServer.trim().matches("[\\w.-]+:\\d{1,5}"))
	    {
	        System.err.println("La cadena aportada no casa con el formato \"hostname:puerto\"");
	        return false;
	    }
	    String[] serverField = uploadToServer.trim().split(":");
	    NFConnector link = null;
	    try {
	        link = new NFConnector(new InetSocketAddress(serverField[0], Integer.parseInt(serverField[1])));
	    } catch (NumberFormatException | IOException e) {
	        System.out.println("Could not contact peer " + uploadToServer);
	        return false;
	    }
	    // 1. Solicitamos la subida (por hash)
	    PeerMessage msgToPeer = new PeerMessage(PeerMessageOps.OPCODE_UPLOAD, matchingFile.fileHash);
	    PeerMessage msgFromPeer = null;
	    try {
	        link.sendMessage(msgToPeer);
	        msgFromPeer = link.receiveMessage();
	    } catch (IOException e) {
	        System.out.println("Client died: " + link);
	        link.close(); return false;
	    }
	    if (msgFromPeer.getOpcode() == PeerMessageOps.OPCODE_FILE_ALREADY_EXISTS)
	    {
	        System.out.println("The file already exists on the remote host");
	        link.close(); return true;
	    }
	    // 2. Enviamos el nombre de fichero
	    msgToPeer = new PeerMessage(PeerMessageOps.OPCODE_FILENAME_TO_SAVE, matchingFile.fileName.length(), matchingFile.fileName);
	    try {
	        link.sendMessage(msgToPeer);
	        msgFromPeer = link.receiveMessage();
	    } catch (IOException e) {
	        System.out.println("Client died: " + link);
	        link.close(); return false;
	    }
	    if (msgFromPeer.getOpcode() != PeerMessageOps.OPCODE_FILEREQUEST_ACCEPTED) 
	    {
	        System.err.println("The remote path is inaccessible");
	        link.close(); return false;
	    }
	    // 3. Enviamos los chunks
	    try (RandomAccessFile file = new RandomAccessFile(NanoFiles.sharedDirname + "/" + matchingFile.fileName, "r")) {
	        int chunks = (int)(matchingFile.fileSize / (long)CHUNK_SIZE + (matchingFile.fileSize % (long)CHUNK_SIZE == 0 ? 0 : 1));
	        for (int chunk = 0; chunk < chunks; chunk++) 
	        {
	            byte[] data = new byte[chunk == chunks - 1 ? (int) (matchingFile.fileSize % (long)CHUNK_SIZE) : CHUNK_SIZE];
	            file.readFully(data);
	            msgToPeer = new PeerMessage(PeerMessageOps.OPCODE_CHUNK, (long)chunk*CHUNK_SIZE, data.length, data);
	            link.sendMessage(msgToPeer);
	        }
	        link.sendMessage(new PeerMessage(PeerMessageOps.OPCODE_STOP));
	    } catch (IOException e) {
	        System.err.println("Error when opening " + matchingFile.fileName + " file");
	        link.close(); return false;
	    }
	    System.out.println("Upload completed successfully");
	    link.close();
	    return true;
    }
}
