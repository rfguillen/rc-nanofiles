package es.um.redes.nanoFiles.logic;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.client.DirectoryConnector;
import es.um.redes.nanoFiles.util.FileInfo;
import java.io.IOException;
import java.net.InetSocketAddress;

public class NFControllerLogicDir 
{

	// Cliente UDP para enviar y recibir mensajes del directorio
	private DirectoryConnector directoryConnector;

	/**
	 * Construye el controlador encargado de implementar la lógica de los comandos
	 * que requieren interactuar con el servidor de directorio dado a través de la
	 * clase DirectoryConnector.
	 * Si no se puede conectar, termina el programa mostrando un error.
	 * 
	 * @param directoryHostname el nombre de host/IP en el que se está ejecutando el
	 *                          directorio
	 */
	protected NFControllerLogicDir(String directoryHostname) 
	{
		try {
			directoryConnector = new DirectoryConnector(directoryHostname);
		} catch (IOException e1) {
			System.err.println("* Check your connection, the directory server at " + directoryHostname + " is not available.");
			System.exit(-1);
		}
	}

	/**
	 * Método para comprobar que la comunicación con el directorio es exitosa (se
	 * pueden enviar y recibir datagramas) haciendo uso de la clase
	 * DirectoryConnector
	 * 
	 * 1. testSendAndReceive: envía "ping" y espera "welcome".
	 * 2. pingDirectoryRaw: envía "ping&protocolID" y espera "welcome" si el protocolo es compatible.
	 * 
	 * Se muestra por pantalla el resultado de cada prueba
	 * 
	 * @return true si se ha conseguido contactar con el directorio.
	 */
	protected void testCommunicationWithDirectory() 
	{
		assert (NanoFiles.testModeUDP);
		System.out.println("[testMode] Testing communication with directory: " + this.directoryConnector.getDirectoryHostname());

		if (directoryConnector.testSendAndReceive()) 
		{
			System.out.println("[testMode] testSendAndReceived - TEST PASSED!");
			
			if (directoryConnector.pingDirectoryRaw()) 
			{
				System.out.println("[testMode] pingDirectoryRaw - SUCCESS!");
			} 
			
			else 
			{
				System.err.println("[testMode] pingDirectoryRaw - FAILED!");
			}
		} 
		
		else 
		{
			System.err.println("[testMode] testSendAndReceived - TEST FAILED!");
		}
	}

	/**
	 * Realiza un "ping" al directorio para comprobar que está disponible y usa un protocolo compatible.
	 * Utiliza el método pingDirectory() de DirectoryConnector, que implementa el protocolo field:value.
	 * 
	 * @return true si el directorio responde correctamente y es compatible, false en caso contrario.
	 */
	public boolean ping() 
	{
		boolean result = false;
		System.out.println("* Checking if the directory at " + directoryConnector.getDirectoryHostname() + " is available...");
		result = directoryConnector.pingDirectory();
		if (result) 
		{
			System.out.println("* Directory is active and uses compatible protocol " + NanoFiles.PROTOCOL_ID);
		} 
		
		else 
		{
			System.err.println("* Ping failed");
		}
		return result;
	}

	/**
	 * Obtiene y muestra la lista de ficheros publicados en el directorio.
	 * Llama a getFileList() de DirectoryConnector, que devuelve un array de FileInfo,
	 * 		y lo imprime por pantalla usando FileInfo.printToSysout().
	 */
	public void getAndPrintFileList() 
	{
		FileInfo[] trackedFiles = directoryConnector.getFileList(); //
		System.out.println("* These are the files tracked by the directory at " + directoryConnector.getDirectoryHostname());
		FileInfo.printToSysout(trackedFiles);
	}

	/**
	 * Método para registrarse en el directorio como servidor de ficheros en un
	 * puerto determinado y enviar al directorio la lista de ficheros que este peer
	 * servidor comparte con el resto (ver método getAndPrintFileList).
	 * 
	 * @param serverPort El puerto TCP en el que está escuchando el servidor de
	 *                   ficheros.
	 * @param filelist   La lista de ficheros a publicar en el directorio
	 * @return Verdadero si el registro se hace con éxito
	 */
	protected boolean registerFileServer(int serverPort, FileInfo[] filelist) 
	{
		boolean result = false;
		if (this.directoryConnector.registerFileServer(serverPort, filelist)) 
		{
			System.out.println("* File server successfully registered with the directory");
			result = true;
		} 
		
		else 
		{
			System.err.println("* File server failed to register with the directory");
		}
		return result;
	}

	/**
	 * Método para consultar al directorio las direcciones de socket de los
	 * servidores que tienen un determinado fichero identificado por una subcadena
	 * del nombre.
	 * 
	 * @param filenameSubstring una subcadena del nombre del fichero por el que se
	 *                          pregunta
	 * @return Una lista de direcciones de socket de los servidores que comparten
	 *         dicho fichero, o null si dicha subcadena del nombre no identifica
	 *         ningún fichero concreto (no existe o es una subcadena ambigua)
	 * 
	 */
	protected InetSocketAddress[] getServerAddressesSharingThisFile(String filenameSubstring) 
	{
		return directoryConnector.getServersSharingThisFile(filenameSubstring);
	}

	/**
	 * Da de baja este servidor de ficheros en el directorio.
	 * Envía un mensaje de "unregister" para eliminar la lista de ficheros compartidos.
	 * 
	 * @param port Puerto TCP en el que estaba escuchando el servidor.
	 * @return true si la baja fue exitosa, false en caso contrario.
	 */
	protected boolean unregisterFileServer(int port) 
	{
		boolean result = false;
		if (this.directoryConnector.unregisterFileServer(port))
		{
			System.out.println("* File server successfully unregistered with the directory");
			result = true;
		} 
		
		else 
		{
			System.err.println("* File server failed to unregister with the directory");
		}
		return result;
	}

	/**
	 * Devuelve el hostname/IP del directorio con el que se está comunicando este peer.
	 */
	protected String getDirectoryHostname() 
	{
		return directoryConnector.getDirectoryHostname();
	}

	/**
	 * Busca información sobre un fichero publicado en el directorio a partir de una subcadena de su nombre.
	 * 
	 * @param filenameSubstring Subcadena del nombre del fichero.
	 * @return FileInfo del fichero encontrado, o null si no hay coincidencias.
	 */
	public FileInfo getFilenameInfo(String filenameSubstring) 
	{
		return directoryConnector.getFilenameInfo(filenameSubstring);
	}

	/**
 	* Devuelve la lista de ficheros publicados en el directorio.
 	* @return Array de FileInfo con los ficheros publicados, o null si hay error.
 	*/
	public FileInfo[] getFileList() 
	{
    	return directoryConnector.getFileList();
	}
}
