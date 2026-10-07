package es.um.redes.nanoFiles.udp.client;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

/**
 * Cliente UDP para la comunicación con el servidor de directorio de NanoFiles.
 * Implementa los métodos para enviar solicitudes y procesar respuestas del directorio,
 * usando el protocolo de mensajes ASCII (field:value).
 */
public class DirectoryConnector
{
	/**
	 * Puerto en el que atienden los servidores de directorio
	 */
	private static final int DIRECTORY_PORT = 6868;
	/**
	 * Tiempo máximo en milisegundos que se esperará a recibir una respuesta por el
	 * socket antes de que se deba lanzar una excepción SocketTimeoutException para
	 * recuperar el control
	 */
	private static final int TIMEOUT = 1000;
	/**
	 * Número de intentos máximos para obtener del directorio una respuesta a una
	 * solicitud enviada. Cada vez que expira el timeout sin recibir respuesta se
	 * cuenta como un intento.
	 */
	private static final int MAX_NUMBER_OF_ATTEMPTS = 5;

	/**
	 * Socket UDP usado para la comunicación con el directorio
	 */
	private DatagramSocket socket;
	/**
	 * Dirección de socket del directorio (IP:puertoUDP)
	 */
	private InetSocketAddress directoryAddress;
	/**
	 * Nombre/IP del host donde se ejecuta el directorio
	 */
	private String directoryHostname;
	
	/**
     * Constructor: inicializa el socket UDP y la dirección del directorio.
     * @param hostname Nombre o IP del host donde se ejecuta el directorio.
     */
	public DirectoryConnector(String hostname) throws IOException 
	{
		// Guardamos el string con el nombre/IP del host
		directoryHostname = hostname;
		
		// Convertimos el hostname a InetAddress
		InetAddress directoryInetAddress = InetAddress.getByName(hostname);
		
		// Creamos el SocketAddress combinando la IP y el puerto
		directoryAddress = new InetSocketAddress(directoryInetAddress, DIRECTORY_PORT);
		
		// Creamos el socket UDP (puerto efímero local)
		socket = new DatagramSocket();
	}
	

	/**
     * Envía un datagrama al directorio y espera la respuesta, con reintentos y timeout.
     * @param requestData Datos a enviar (mensaje codificado en bytes)
     * @return Respuesta recibida (bytes), o null si no hay respuesta tras los reintentos.
     */
	private byte[] sendAndReceiveDatagrams(byte[] requestData) 
	{
		byte[] responseBuffer = new byte[DirMessage.PACKET_MAX_SIZE];
		if (directoryAddress == null) 
		{
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP server destination address is null!");
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"directoryAddress\"");
			System.exit(-1);

		}
		
		if (socket == null) 
		{
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP socket is null!");
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"socket\"");
			System.exit(-1);
		}
		
		// Creamos el datagrama para enviar los datos
		DatagramPacket requestPacket = new DatagramPacket(
				requestData,
				requestData.length,
				directoryAddress
		);

		// Contador de los intentos
		int attempts = 0;
		
		// Bucle para volver a intentar en caso de timeout
		while (attempts < MAX_NUMBER_OF_ATTEMPTS)
		{
			try{
				// Establecemos timeout para la recepción
				socket.setSoTimeout(TIMEOUT);
				
				// Enviamos el datagrama
				socket.send(requestPacket);
				
				// Preparamos el datagrama para recibir la respuesta
				DatagramPacket responsePacket = new DatagramPacket(
						responseBuffer,
						responseBuffer.length
				);
				
				// Recibimos la respuesta
				socket.receive(responsePacket);
				
				// Extraemos solo los datos recibidos (no todo el buffer)
				byte[] actualData = new byte[responsePacket.getLength()];
				System.arraycopy(responsePacket.getData(), responsePacket.getOffset(), actualData, 0, responsePacket.getLength());
				
				return actualData;
				
			} catch(SocketTimeoutException e) {
				// Si hay timeout, incrementamos el contador y volvemos a intentar
				attempts++;
				System.out.println("Timeout: reintento " + attempts + " de " + MAX_NUMBER_OF_ATTEMPTS);
				
			} catch (IOException e) {
				// Error de E/S, termina el programa
				System.err.println("Error de entrada/salida en el socket: " + e.getMessage());
				System.exit(-1);
			}
		}
		return null;
	}

	/**
     * Prueba básica de comunicación: envía "ping" y espera "pingok" como respuesta.
     * @return true si la comunicación es correcta.
     */
	public boolean testSendAndReceive() 
	{
		boolean success = false;

		// Creamos el mensaje a enviar
		byte[] messageBytes = "ping".getBytes();
		
		// Usamos sendAndReceiveDatagrams para enviar y recibir
		byte[] responseBytes = sendAndReceiveDatagrams(messageBytes);
		
		// Comprobamos si hay respuesta
		if (responseBytes != null)
		{
			// Convertimos los bytes de respuesta a string
			String response = new String(responseBytes);
			
			// Comprobamos si la respuesta empieza por "pingok"
			if (response.startsWith("pingok"))
			{
				success = true;
			}
		}

		return success;
	}

	public String getDirectoryHostname()
	{
		return directoryHostname;
	}

	/**
     * Realiza un "ping" al directorio usando el formato "ping&PROTOCOL_ID".
     * @return true si el directorio responde "welcome".
     */
	public boolean pingDirectoryRaw() 
	{
	    boolean success = false;
	    
	    // PASO 1: Creamos el mensaje con formato "ping&PROTOCOL_ID"
	    String message = "ping&" + NanoFiles.PROTOCOL_ID;
	    
	    // PASO 2: Convertimos el mensaje a bytes
	    byte[] messageBytes = message.getBytes();
	    
	    // PASO 3: Enviamos los bytes y recibimos respuesta
	    byte[] responseBytes = sendAndReceiveDatagrams(messageBytes);
	    
	    // PASO 4: Procesamos la respuesta
	    if (responseBytes != null) 
	    {
	        String response = new String(responseBytes);
	        
	        if (response.equals("welcome")) 
	        {
	            success = true;
	            System.out.println("Ping exitoso: Directorio compatible");
	        } 
	        
	        else if (response.equals("denied")) 
	        {
	            System.out.println("Ping fallido: Directorio con protocolo incompatible");
	        }
	        
	        else 
	        {
	            System.out.println("Ping fallido: Respuesta inesperada: " + response);
	        }
	    } 
	    
	    else 
	    {
	        System.out.println("Ping fallido: No se recibió respuesta del directorio");
	    }
	    
	    return success;
	}

	
	/**
	 * Método para "hacer ping" al directorio, comprobar que está operativo y que es
	 * compatible. Usa el formato field:value
	 * 
	 * @return Verdadero si el directorio está operativo y es compatible (Si el directorio
	 * responde con OPERATION_WELCOME)s
	 */ 
	public boolean pingDirectory() 
	{
		boolean success = false;
		
		try {
			// 1. Creamos el mensaje a enviar con los atributos adecuados
			DirMessage requestMessage = new DirMessage(DirMessageOps.OPERATION_PING, NanoFiles.PROTOCOL_ID);
			
			// 2. Convertimos el objeto DirMessage a String
			String messageString = requestMessage.toString();
			
			// 3. Creamos un datagrama con los bytes en los que se codifica la cadena
			byte [] messageBytes = messageString.getBytes();
			
			// 4. Enviamos datagrama y recibimos respuesta
			byte[] responseBytes = sendAndReceiveDatagrams(messageBytes);
			
			if (responseBytes != null)
			{
				// 5. Convertimos la respuesta recibida en un objeto DirMessage
				String responseString = new String(responseBytes);
				DirMessage responseMessage = DirMessage.fromString(responseString);
				
				// 6. Extraemos los datos y los procesamos
				String operation = responseMessage.getOperation();
				
				if (operation.equals(DirMessageOps.OPERATION_WELCOME))
				{
					success = true;
					System.out.println("Ping exitoso: Directorio compatible");
				}
				
				else if (operation.equals(DirMessageOps.OPERATION_DENIED))
				{
					String errorMessage = responseMessage.getMessage();
					System.out.println("Ping fallido: " + errorMessage);
				}
				
				else
				{
					System.out.println("Ping fallido: Operación inesperada: " + operation);
				}
			}
			
			else
			{
				System.out.println("Ping fallido: No se recibió respuesta del directorio");
			}
			
		} catch (Exception e) {
			System.err.println("Error durante el ping al directorio: " + e.getMessage());
		}

		return success;
	}

	/**
	 * Método para dar de alta como servidor de ficheros en el puerto indicado y
	 * publicar los ficheros que este peer servidor está sirviendo.
	 * 
	 * @param serverPort El puerto TCP en el que este peer sirve ficheros a otros
	 * @param files      La lista de ficheros que este peer está sirviendo.
	 * @return Verdadero si el directorio tiene registrado a este peer como servidor
	 *         y acepta la lista de ficheros, falso en caso contrario.
	 */
	public boolean registerFileServer(int serverPort, FileInfo[] files)
	{
		boolean success = false;

		// Hacemos ping antes de registrar
		if (!pingDirectory())
		{
			System.out.println("No se puede contactar con el directorio o protocolo incompatible");
			return false;
		}
		
		try{
			// 1. Creamos el mensaje de registro de servidor
			DirMessage requestMessage = new DirMessage(DirMessageOps.OPERATION_REGISTER_FILES);
			requestMessage.setServerPort(serverPort);
			
			// Convertimos los FileInfo a formato string (hash;nombre;tamaño)
			String[] fileStrings = new String[files.length];
			for (int i = 0; i < files.length; i++)
			{
				FileInfo file = files[i];
				fileStrings[i] = String.format("%s;%s;%d", file.fileHash, file.fileName, file.fileSize);
			}
			requestMessage.setFiles(fileStrings);
			
			// 2. Enviamos el mensaje y procesamos la respuesta
			byte[] responseBytes = sendAndReceiveDatagrams(requestMessage.toString().getBytes());
			
			if (responseBytes != null)
			{
				DirMessage responseMessage = DirMessage.fromString(new String(responseBytes));
				
				if (responseMessage.getOperation().equals(DirMessageOps.OPERATION_REGISTER_FILES))
				{
					if (responseMessage.getStatus().equals(DirMessageOps.STATUS_OK))
					{
						success = true;
						System.out.println("Servidor registrado exitosamente");
					}
					
					else if (requestMessage.getStatus().equals(DirMessageOps.STATUS_ERROR))
					{
						System.out.println("Error al registrar servidor: " + responseMessage.getMessage());
					}
					
					else
					{
						System.out.println("Error: operacion inesperada: " + responseMessage.getOperation());
					}
				}
			}
			
		} catch (Exception e) {
			System.err.println("Error al registrar servidor: " + e.getMessage());
		}

		return success;
	}

	/**
	 * Método para obtener la lista de ficheros que los peers servidores han
	 * publicado al directorio. Para cada fichero se obtiene un objeto FileInfo
	 * con nombre, tamaño, hash y su lista de peers servidores que lo están compartiendo.
	 * 
	 * 
	 * @return Los ficheros publicados al directorio, o null si el directorio no
	 *         pudo satisfacer nuestra solicitud
	 */
	public FileInfo[] getFileList() 
	{
		// Hacemos ping antes de pedir la lista
	    if (!pingDirectory())
	    {
	        System.out.println("No se puede contactar con el directorio o protocolo incompatible");
	        return null;
	    }

	    // Inicializamos en null en vez de array vacio
	    FileInfo[] filelist = null;

	    try{
	        // 1. Creamos el mensaje de solicitud de la lista de archivos
	        DirMessage requestMessage = new DirMessage(DirMessageOps.OPERATION_GET_FILES);

	        // 2. Convertimos el mensaje a String
	        String messageString = requestMessage.toString();

	        // 3. Enviamos el mensaje y recibimos la respuesta
	        byte [] responseBytes = sendAndReceiveDatagrams(messageString.getBytes());

	        if (responseBytes != null)
	        {
	            // 4. Convertimos la respuesta a un objeto DirMessage
	            String responseString = new String(responseBytes);
	            DirMessage responseMessage = DirMessage.fromString(responseString);

	            // 5. Procesamos la respuesta
	            if (responseMessage.getOperation().equals(DirMessageOps.OPERATION_FILE_LIST))
	            {
	                if (responseMessage.getStatus().equals(DirMessageOps.STATUS_OK))
	                {
	                    String[] files = responseMessage.getFiles();
	                    if (files != null && files.length > 0)
	                    {
	                        filelist = new FileInfo[files.length];

	                        // Creamos objetos FileInfo con los datos recibidos
	                        for (int i = 0; i < files.length; i++)
	                        {
	                            // El formato esperado es: hash;nombre;tamaño;servidores
	                            String[] fileData = files[i].split(";");
	                            if (fileData.length >= 3)
	                            {
	                                String hash = fileData[0];
	                                String name = fileData[1];
	                                long size = Long.parseLong(fileData[2]);
	                                String[] serversList = null;
	                                // Si hay una lista de servidores, la extraemos
	                                if (fileData.length >= 4 && !fileData[3].isEmpty()) {
	                                    serversList = fileData[3].split("\\|");
	                                }
	                                // Creamos el FileInfo con la lista de servidores para la mejora "filelist ampliado"
	                                filelist[i] = new FileInfo(hash, name, size, null, serversList);
	                            }
	                            else
	                            {
	                                // Si no tenemos todos los datos, creamos un FileInfo vacio
	                                filelist[i] = new FileInfo();
	                                // Y establecemos el nombre si lo tenemos
	                                if (fileData.length > 0)
	                                {
	                                    filelist[i].fileName = fileData[0];
	                                }
	                            }
	                        }
	                        System.out.println("Lista de archivos recibida exitosamente: " + files.length + " archivos encontrados");
	                    }
	                    else
	                    {
	                        System.out.println("No hay archivos publicados en el directorio");
	                        filelist = new FileInfo[0];
	                    }
	                }
	                else
	                {
	                    System.out.println("Error al obtener la lista de archivos: " + responseMessage.getMessage());
	                }
	            }
	            else
	            {
	                System.out.println("Error: Operacion inesperada: " + responseMessage.getOperation());
	            }
	        }
	        else
	        {
	            System.out.println("Error: No se recibio respuesta del directorio");
	        }
	    } catch (Exception e){
	        System.err.println("Error al obtener la lista de archivos: " + e.getMessage());
	    }

	    return filelist;
	}

	/**
	 * Método para obtener la lista de servidores que tienen un fichero cuyo nombre
	 * contenga la subcadena dada.
	 * 
	 * @param filenameSubstring Subcadena del nombre del fichero a buscar
	 * 
	 * @return La lista de direcciones de los servidores que han publicado al
	 *         directorio el fichero indicado.
	 */
	public InetSocketAddress[] getServersSharingThisFile(String filenameSubstring) 
	{
	    // Hacemos ping antes de buscar servidores
	    if (!pingDirectory())
	    {
	        System.out.println("No se puede contactar con el directorio o protocolo incompatible");
	        return new InetSocketAddress[0];
	    }

	    InetSocketAddress[] serversList = new InetSocketAddress[0];

	    try{
	        // 1. Creamos mensaje de búsqueda de servidores
	        DirMessage requestMessage = new DirMessage(DirMessageOps.OPERATION_DOWNLOAD);
	        // En la petición, SÍ se usa el campo fileName
	        requestMessage.setFileName(filenameSubstring);

	        // 2. Enviamos el mensaje y recibimos la respuesta
	        byte[] responseBytes = sendAndReceiveDatagrams(requestMessage.toString().getBytes());

	        if (responseBytes != null)
	        {
	            DirMessage responseMessage = DirMessage.fromString(new String(responseBytes));

	            // SOLO accedemos a getServers() si la operación es OPERATION_SERVER_LIST
	            if (responseMessage.getOperation().equals(DirMessageOps.OPERATION_SERVER_LIST))
	            {
	                if (responseMessage.getStatus().equals(DirMessageOps.STATUS_OK))
	                {
	                    String[] servers = responseMessage.getServers();
	                    if (servers != null && servers.length > 0)
	                    {
	                        serversList = new InetSocketAddress[servers.length];

	                        // Procesamos cada servidor (ip:puerto)
	                        for (int i = 0; i < servers.length; i++)
	                        {
	                            String[] serverData = servers[i].split(":");
	                            if (serverData.length == 2)
	                            {
	                                String host = serverData[0];
	                                int port = Integer.parseInt(serverData[1]);
	                                serversList[i] = new InetSocketAddress(host, port);
	                            }
	                        }
	                        System.out.println("Lista de servidores recibida: " + servers.length + " servidores encontrados");
	                    }
	                    
	                    else
	                    {
	                        System.out.println("No se encontraron servidores para el archivo");
	                    }
	                }
	                
	                else if (responseMessage.getStatus().equals(DirMessageOps.STATUS_ERROR))
	                {
	                    System.out.println("Error al buscar servidores: " + responseMessage.getMessage());
	                }
	            }
	            
	            else
	            {
	                // Si la operación no es la esperada, mostramos un error claro
	                System.out.println("Error: Operación inesperada: " + responseMessage.getOperation());
	            }
	        }
	    } catch (NumberFormatException e) {
	        System.err.println("Error al procesar el puerto: " + e.getMessage());
	    } catch (Exception e) {
	        System.err.println("Error al obtener lista de servidores: " + e.getMessage());
	    }

	    return serversList;
	}

	/**
	 * Método para darse de baja como servidor de ficheros.
	 * 
	 * @return Verdadero si el directorio tiene registrado a este peer como servidor
	 *         y ha dado de baja sus ficheros.
	 */
	public boolean unregisterFileServer(int serverPort) 
	{
		boolean success = false;

	    // Hacemos ping antes de intentar darnos de baja
	    if (!pingDirectory()) 
	    {
	        System.out.println("No se puede contactar con el directorio o protocolo incompatible");
	        return false;
	    }

	    try {
	        // 1. Creamos el mensaje de baja (igual que el de registro, pero sin archivos)
	        DirMessage requestMessage = new DirMessage(DirMessageOps.OPERATION_REGISTER_FILES);
	        requestMessage.setServerPort(serverPort);
	        requestMessage.setFiles(new String[0]); // Lista vacía de archivos

	        // 2. Enviamos el mensaje y procesamos la respuesta
	        byte[] responseBytes = sendAndReceiveDatagrams(requestMessage.toString().getBytes());

	        if (responseBytes != null) 
	        {
	            DirMessage responseMessage = DirMessage.fromString(new String(responseBytes));

	            // Comprobamos la operación y el estado
	            if (responseMessage.getOperation().equals(DirMessageOps.OPERATION_REGISTER_FILES)) 
	            {
	                if (responseMessage.getStatus().equals(DirMessageOps.STATUS_OK)) 
	                {
	                    success = true;
	                    System.out.println("Servidor dado de baja exitosamente");
	                } 
	                
	                else 
	                {
	                    System.out.println("Error al darse de baja: " + responseMessage.getMessage());
	                }
	            }
	            
	            else 
	            {
	                System.out.println("Error: operación inesperada: " + responseMessage.getOperation());
	            }
	        } 
	        
	        else 
	        {
	            System.out.println("Error: No se recibió respuesta del directorio");
	        }
	    } catch (Exception e) {
	        System.err.println("Error al darse de baja como servidor: " + e.getMessage());
	    }

	    return success;
	}

	/**
     * Busca un fichero por subcadena en la lista de ficheros publicados.
     * @param filenameSubstring Subcadena a buscar.
     * @return FileInfo del fichero encontrado, o null si no hay coincidencia única.
     */
	public FileInfo getFilenameInfo(String filenameSubstring) 
	{
	    // 1. Hacemos ping antes de consultar
	    if (!pingDirectory()) 
	    {
	        System.out.println("No se puede contactar con el directorio o protocolo incompatible");
	        return null;
	    }

	    // 2. Obtenemos la lista de ficheros publicados en el directorio
	    FileInfo[] fileList = getFileList();
	    if (fileList == null || fileList.length == 0) 
	    {
	        System.out.println("No hay archivos publicados en el directorio");
	        return null;
	    }

	    // 3. Buscamos coincidencias por subcadena (ignorando mayúsculas/minúsculas)
	    FileInfo match = null;
	    int count = 0;
	    String needle = filenameSubstring.toLowerCase();

	    for (FileInfo file : fileList) 
	    {
	        if (file.fileName != null && file.fileName.toLowerCase().contains(needle)) 
	        {
	            match = file;
	            count++;
	        }
	    }

	    if (count == 0) 
	    {
	        System.out.println("No se encontró ningún archivo que contenga: " + filenameSubstring);
	        return null;
	    } 
	    
	    else if (count > 1) 
	    {
	        System.out.println("Subcadena ambigua: hay varios archivos que coinciden con '" + filenameSubstring + "'");
	        for (FileInfo file : fileList) 
	        {
	            if (file.fileName != null && file.fileName.toLowerCase().contains(needle)) 
	            {
	                System.out.println(" - " + file.fileName);
	            }
	        }
	        return null;
	    }
	    // Solo hay una coincidencia
	    return match;
	}
}
