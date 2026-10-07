package es.um.redes.nanoFiles.logic;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.shell.NFCommands;
import es.um.redes.nanoFiles.shell.NFShell;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

public class NFController 
{
	/**
	 * Diferentes estados del cliente de acuerdo con el autómata
	 */
	private static final byte OFFLINE = 0; 	// Estado inicial: no conectado al directorio
	private static final byte ONLINE = 1; 	// Estado tras ping/filelist/download (exitoso)
	private static final byte SERVING = 2; 	// Estado tras lanzar el servidor de ficheros

	/**
	 * Shell para leer comandos de usuario de la entrada estándar
	 */
	private NFShell shell;
	/**
	 * Último comando proporcionado por el usuario
	 */
	private byte currentCommand;

	/**
	 * Objeto controlador encargado de la comunicación con el directorio
	 */
	private NFControllerLogicDir controllerDir;
	/**
	 * Objeto controlador encargado de la comunicación con otros peers (como
	 * servidor o cliente)
	 */
	private NFControllerLogicP2P controllerPeer;

	/**
	 * El estado en que se encuentra este peer (según el autómata). El estado debe
	 * actualizarse cuando se produce un evento (comando) que supone un cambio en el
	 * autómata.
	 */
	private byte currentState;
	/**
	 * Atributos donde se establecen los argumentos pasados a los distintos comandos
	 * del shell. Estos atributos se establecen automáticamente según la orden y se
	 * deben usar para pasar los valores de los parámetros a las funciones invocadas
	 * desde este controlador.
	 */
	private String targetFilenameSubstring; // Nombre del fichero a descargar/subir (download/upload)
	private String downloadLocalFileName; // Nombre con el que se guardará el fichero descargado (download)
	private String uploadToServer; // Servidor al que se subirá el fichero indicado (upload)

	/**
	 * Constructor principal. Inicializa el shell y el estado inicial.
 	 * El controlador de directorio se inicializa después de elegir el directorio.
	 * El shell solo recoge argumentos, el controlador ejecuta la lógica real.
 	 * @param defaultDirectory Hostname/IP del directorio por defecto.
	 */
	public NFController(String defaultDirectory) 
	{
		controllerPeer = new NFControllerLogicP2P();
		shell = new NFShell(controllerDir, controllerPeer);

		String directory = shell.chooseDirectory(defaultDirectory);

		controllerDir = new NFControllerLogicDir(directory);
		controllerPeer = new NFControllerLogicP2P();
		currentState = OFFLINE; // Estado inicial del autómata
	}

	/**
	 * Método que procesa los comandos introducidos por un usuario. Se encarga
	 * principalmente de invocar los métodos adecuados de NFControllerLogicDir y
	 * NFControllerLogicP2P según el comando.
	 * 
	 * Solo se usa si testModeUDP == true
	 */
	public void testCommunication() 
	{
		assert (NanoFiles.testModeUDP);
		System.out.println("[testMode] Attempting to reach directory server at " + controllerDir.getDirectoryHostname());
		controllerDir.testCommunicationWithDirectory();
		System.out.println("[testMode] Test terminated!");
	}

	/**
	 * Método que procesa los comandos introducidos por un usuario. Se encarga
	 * principalmente de invocar los métodos adecuados de NFControllerLogicDir y
	 * NFControllerLogicP2P según el comando.
	 * 
	 * Actualiza el estado del autómata según el resultado
	 */
	public void processCommand() 
	{

		if (!canProcessCommandInCurrentState()) 
		{
			return;
		}
		/*
		 * En función del comando, invocar los métodos adecuados de NFControllerLogicDir
		 * y NFControllerLogicP2P, ya que son estas dos clases las que implementan
		 * realmente la lógica de cada comando y procesan la información recibida
		 * mediante la comunicación con el directorio u otros pares de NanoFiles
		 * (imprimir por pantalla el resultado de la acción y los datos recibidos,
		 * etc.).
		 */
		boolean commandSucceeded = false;
		switch (currentCommand) 
		{
			case NFCommands.COM_MYFILES:
				showMyLocalFiles(); // Muestra los ficheros en el directorio local compartido
				commandSucceeded = true;
				break;
			case NFCommands.COM_PING:
				/*
				 * Pedir al controllerDir enviar un "ping" al directorio, para comprobar que
				 * está activo y disponible, y comprobar que es compatible.
				 * 
				 * (Comprueba si el directorio está activo y es compatible)
				 */
				commandSucceeded = controllerDir.ping();
				break;
			case NFCommands.COM_FILELIST:
				/*
				 * Pedir al controllerDir que obtenga del directorio la lista de ficheros que
				 * hay publicados (los ficheros que otros peers están sirviendo), y la imprima
				 * por pantalla (método getAndPrintFileList)
				 * 
				 * (Obtiene e imprime la lista de ficheros publicados en el directorio)
				 */
				controllerDir.getAndPrintFileList();
				commandSucceeded = true;
				break;
				
			case NFCommands.COM_SERVE:
				/*
				 * Pedir al controllerPeer que lance un servidor de ficheros. Si el servidor se
				 * ha podido iniciar correctamente, pedir al controllerDir darnos de alta como
				 * servidor de ficheros en el directorio, indicando el puerto en el que nuestro
				 * servidor escucha conexiones de otros peers así como la lista de ficheros
				 * disponibles.
				 * 
				 * (Lanza el servidor de ficheros y lo registra en el directorio)
				 */
			    if (NanoFiles.testModeTCP) {
			        controllerPeer.testTCPServer();
			    } else {
			        int port = 10000;
			        if (shell.getCommandArguments().length == 1) {
			            try {
			                port = Integer.parseInt(shell.getCommandArguments()[0]);
			            } catch (NumberFormatException e) {
			                System.out.println("Invalid port, using default 10000");
			            }
			        }
			        boolean serverRunning = controllerPeer.startFileServer(port);
			        if (serverRunning) {
			            commandSucceeded = controllerDir.registerFileServer(controllerPeer.getServerPort(), NanoFiles.db.getFiles());
			        } else {
			            System.err.println("Cannot start file server");
			        }
			    }
			    break;
			case NFCommands.COM_DOWNLOAD:
				/*
				 * Pedir al controllerDir que obtenga del directorio la lista de servidores que
				 * comparten el fichero cuyo nombre contenga la subcadena indicada (1er
				 * argumento pasado al comando). Si existen servidores que comparten ficheros
				 * con dicho nombre, pedir al controllerPeer que descargue el fichero indicado
				 * de los servidores obtenidos, y lo guarde con el nombre indicado en
				 * downloadLocalFileName (2º argumento)
				 * 
				 * (Descarga un fichero de los servidores que lo comparten)
				 */
				if (NanoFiles.testModeTCP) 
				{
					controllerPeer.testTCPClient();
				} 
			
				else 
				{
					FileInfo[] matchingFiles = FileInfo.lookupFilenameSubstring(controllerDir.getFileList(), targetFilenameSubstring);
					if (matchingFiles.length == 1) 
					{
						FileInfo fileInfo = matchingFiles[0];
						InetSocketAddress[] serverAddressList = controllerDir.getServerAddressesSharingThisFile(fileInfo.fileName);
						commandSucceeded = controllerPeer.downloadFileFromServers(serverAddressList, fileInfo, downloadLocalFileName);
					} 

					else if (matchingFiles.length == 0) 
					{
						System.err.println("No se encontró ningún archivo que contenga: " + targetFilenameSubstring);
					}

					else
					{
						System.err.println("Subcadena ambigua: hay varios archivos que coinciden con '" + targetFilenameSubstring + "'");
						FileInfo.printToSysout(matchingFiles);
					}

				}
				break;
			case NFCommands.COM_QUIT:
				/*
				 * Pedir al controllerPeer que pare el servidor en segundo plano (método método
				 * stopBackgroundFileServer). A continuación, pedir al controllerDir que
				 * solicite al directorio darnos de baja como servidor de ficheros (método
				 * unregisterFileServer).
				 * 
				 * (Detiene el servidor de ficheros y se da de baja en el directorio si estaba sirviendo)
				 */
				if (controllerPeer.serving()) 
				{
					commandSucceeded = controllerDir.unregisterFileServer(controllerPeer.getServerPort());
					controllerPeer.stopFileServer();
				}
				
				else
				{
					commandSucceeded = true;
				}
				break;
			case NFCommands.COM_UPLOAD:
				/*
				 * Pedir al controllerPeer que envíe un fichero a un servidor de ficheros.
				 * Localiza el fichero a enviar de entre los disponibles en NanoFiles.db, a
				 * partir de la subcadena del nombre proporcionada.
				 * 
				 * (Sube un fichero a otro peer servidor)
				 */
				// Buscar todos los archivos que coincidan con la subcadena
		    	FileInfo[] matchingFiles = FileInfo.lookupFilenameSubstring(NanoFiles.db.getFiles(), targetFilenameSubstring);
		    	if (matchingFiles.length == 1) 
		    	{
		    	    // Solo uno: subimos el archivo
		    	    commandSucceeded = controllerPeer.uploadFileToServer(matchingFiles[0], uploadToServer);
		    	} 
		    
		    	else if (matchingFiles.length == 0) 
		    	{
		    	    System.err.println("Cannot locate file to upload! No matching files found");
		    	} 
		    
		    	else 
		    	{
		    	    System.err.println("Ambiguous filename substring! Candidate files are:");
		    	    FileInfo.printToSysout(matchingFiles);
		    	}
			default:
				// Un comando no reconocido o no implementado
		}
		updateCurrentState(commandSucceeded);
	}

	/**
	 * Método que comprueba si se puede procesar un comando introducidos por un
	 * usuario, en función del estado del autómata en el que nos encontramos.
	 */
	private boolean canProcessCommandInCurrentState() 
	{
		boolean commandAllowed = true;
		switch (currentCommand)	
		{
			case NFCommands.COM_MYFILES: 
			{
				commandAllowed = true;
				break;
			}
			
			case NFCommands.COM_UPLOAD:
				commandAllowed = true;
			    break;
			
			default:
				// Otros comandos permitidos por defecto
		}
		return commandAllowed;
	}

	/**
     * Actualiza el estado del autómata tras ejecutar un comando, 
     * según el resultado.
     * @param success true si el comando se ejecutó correctamente, 
     * false en caso contrario.
     */
	private void updateCurrentState(boolean success) 
	{
		if (!success) 
		{
			System.err.println("The command has failed.");
			return;
		}
		
		switch (currentCommand) 
		{
			case NFCommands.COM_PING:
			case NFCommands.COM_FILELIST:
			case NFCommands.COM_DOWNLOAD:
				if (currentState == OFFLINE)
				{
					currentState = ONLINE;
				}
				break;
			
			case NFCommands.COM_SERVE:
				currentState = SERVING;
				break;
		
			default:
				// Otros comandos no cambian el estado
		}

	}

	/**
	 * Muestra la lista de ficheros en la carpeta local compartida.
	 */
	private void showMyLocalFiles() 
	{
		System.out.println("List of files in local folder:");
		FileInfo.printToSysout(NanoFiles.db.getFiles());
	}

	/**
	 * Método que comprueba si el usuario ha introducido el comando para salir de la
	 * aplicación
	 */
	public boolean shouldQuit() 
	{
		return currentCommand == NFCommands.COM_QUIT;
	}

	/**
	 * Establece el comando actual.
	 * @param command el comando tecleado en el shell
	 */
	private void setCurrentCommand(byte command) 
	{
		currentCommand = command;
	}

	/**
	 * Registra en atributos internos los posibles parámetros del comando tecleado
	 * 		por el usuario.
	 * @param args argumentos del comando
	 */
	private void setCurrentCommandArguments(String[] args) 
	{
		switch (currentCommand) 
		{
			case NFCommands.COM_DOWNLOAD:
				targetFilenameSubstring = args[0];
				downloadLocalFileName = args[1];
				break;
			case NFCommands.COM_UPLOAD:
				targetFilenameSubstring = args[0];
				uploadToServer = args[1];
				break;
			default:
		}
	}

	/**
	 * Lee un comando general del shell y actualiza el comando y sus argumentos.
	 */
	public void readGeneralCommandFromShell() 
	{
		// Pedimos el comando al shell
		shell.readGeneralCommand();
		// Establecemos que el comando actual es el que ha obtenido el shell
		setCurrentCommand(shell.getCommand());
		// Analizamos los posibles parámetros asociados al comando
		setCurrentCommandArguments(shell.getCommandArguments());
	}
	
	/**
	 * Descarga un chunk de un fichero remoto usando el protocolo binario PeerMessage.
	 * @param dis DataInputStream del socket TCP
	 * @param dos DataOutputStream del socket TCP
	 * @param fileHash Hash del fichero a descargar
	 * @param offset Desplazamiento inicial del chunk
	 * @param chunkSize Tamaño del chunk a descargar
	 * @return Array de bytes con el chunk descargado, o null si hay error
	 * @throws IOException si ocurre un error de E/S
	 */
	public byte[] downloadChunkByHash(DataInputStream dis, DataOutputStream dos, String fileHash, long offset, int chunkSize) throws IOException 
	{
		// 1. Crear el mensaje de petición de chunk
	    PeerMessage request = new PeerMessage(PeerMessageOps.OPCODE_CHUNKREQUEST);
	    request.setReqFileHash(fileHash); // El hash del fichero como String hexadecimal
	    request.setOffset(offset);
	    request.setSize(chunkSize);

	    // 2. Enviar el mensaje al servidor
	    request.writeMessageToOutputStream(dos);
	    dos.flush();

	    // 3. Esperar la respuesta del servidor
	    PeerMessage response = PeerMessage.readMessageFromInputStream(dis);

	    // 4. Procesar la respuesta
	    if (response.getOpcode() == PeerMessageOps.OPCODE_CHUNK)
	    {
	    	// Respuesta correcta: el servidor envía el chunk solicitado
	        return response.getChunkData();
	    } 
	    
	    else if (response.getOpcode() == PeerMessageOps.OPCODE_FILE_NOT_FOUND) 
	    {
	    	// El servidor no tiene el fichero solicitado
	        System.err.println("El servidor no tiene el fichero solicitado.");
	        return null;
	    } 
	    
	    else if (response.getOpcode() == PeerMessageOps.OPCODE_CHUNKREQUEST_OUTOFRANGE) 
	    {
	    	// El chunk solicitado está fuera de rango
	        System.err.println("Chunk fuera de rango.");
	        return null;
	    } 
	    
	    else 
	    {
	    	// Cualquier otro OPCODE
	        System.err.println("Respuesta inesperada del servidor: " + response.getOpcode());
	        return null;
	    }
	}
}

	
