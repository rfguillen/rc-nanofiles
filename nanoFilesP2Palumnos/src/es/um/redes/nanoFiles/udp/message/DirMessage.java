package es.um.redes.nanoFiles.udp.message;

/**
 * Clase que modela los mensajes del protocolo de comunicación entre pares para
 * implementar el explorador de ficheros remoto (servidor de ficheros). Estos
 * mensajes son intercambiados entre las clases DirectoryServer y
 * DirectoryConnector, y se codifican como texto en formato "campo:valor".
 * 
 * @author rtitos
 *
 */
public class DirMessage 
{
	public static final int PACKET_MAX_SIZE = 65507; // 65535 - 8 (UDP header) - 20 (IP header)

	private static final char DELIMITER = ':'; // Define el delimitador
	private static final char END_LINE = '\n'; // Define el carácter de fin de línea

	/**
	 * Nombre del campo que define el tipo de mensaje (primera línea)
	 */
	private static final String FIELDNAME_OPERATION = "operation";
	private static final String FIELDNAME_PROTOCOL = "protocol";
	private static final String FIELDNAME_STATUS = "status";
	private static final String FIELDNAME_MESSAGE = "message";
	private static final String FIELDNAME_FILES = "files";
	private static final String FIELDNAME_SERVER = "server";
	private static final String FIELDNAME_PORT = "port";
	private static final String FIELDNAME_FILENAME = "filename";
	private static final String FIELDNAME_SERVERS = "servers";
	
	/**
	 * Tipo del mensaje, de entre los tipos definidos en PeerMessageOps.
	 */
	private String operation = DirMessageOps.OPERATION_INVALID;
	/**
	 * Identificador de protocolo usado, para comprobar compatibilidad del directorio.
	 */
	private String protocolId;      // Identificador de protocolo (para ping)
    private String status;          // Estado de la respuesta (ok/error)
    private String message;         // Mensaje adicional (errores, etc.)
    private String[] files;         // Lista de archivos (hash;nombre;tamaño)
    private String serverAddress;   // Dirección del servidor (registro)
    private int serverPort;         // Puerto del servidor (registro)
    private String fileName;        // Nombre de fichero (descarga)
    private String[] servers;       // Lista de servidores (ip:puerto)

    /**
     * Constructor genérico: crea un mensaje con la operación indicada.
     * @param op Tipo de operación
     */
	public DirMessage(String op) 
	{
		operation = op;
	}
	
	/**
     * Constructor para mensajes de ping.
     * @param op Debe ser OPERATION_PING
     * @param protocolId Identificador de protocolo
     */
	public DirMessage(String op, String protocolId)
	{
		if (!op.equals(DirMessageOps.OPERATION_PING))
		{
			throw new RuntimeException("Invalid operation for this constructor");
		}
		
		operation = op;
		this.protocolId = protocolId;
	}

	/**
     * Constructor para mensajes de respuesta welcome/denied.
     * @param op Debe ser OPERATION_WELCOME o OPERATION_DENIED
     * @param status Estado de la respuesta
     * @param message Mensaje adicional (solo para denied)
     */
	public DirMessage(String op, String status, String message)
	{
		if (!op.equals(DirMessageOps.OPERATION_WELCOME) && !op.equals(DirMessageOps.OPERATION_DENIED))
		{
			throw new RuntimeException("Invalidad operation for this constructor");
		}
		
		operation = op;
		this.status = status;
		this.message = message;
	}

	/**
     * Constructor para mensajes de lista de archivos.
     * @param op Debe ser OPERATION_FILE_LIST
     * @param files Array de strings con los archivos
     */
	public DirMessage(String op, String[] files)
	{
		if (!op.equals(DirMessageOps.OPERATION_FILE_LIST))
		{
			throw new RuntimeException("Invalidad operation for this constructor");
		}
		operation = op;
		this.files = files;
	}

	/**
     * Constructor para registro de servidor.
     * @param op Debe ser OPERATION_REGISTER_FILES
     * @param serverAddress Dirección IP del servidor
     * @param serverPort Puerto TCP del servidor
     * @param files Lista de archivos publicados
     */
	public DirMessage(String op, String serverAddress, int serverPort, String[] files)
	{
		if (!op.equals(DirMessageOps.OPERATION_REGISTER_FILES))
		{
			throw new RuntimeException("Invalidad operation for this constructor");
		}
		operation = op;
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.files = files;
	}
	
	public String getOperation() 
	{
		return operation;
	}

	// Getters y setters para protocolId (solo valido para ping)
	public void setProtocolID(String protocolIdent) 
	{
		if (!operation.equals(DirMessageOps.OPERATION_PING)) 
		{
			throw new RuntimeException("DirMessage: setProtocolId called for message of unexpected type (" + operation + ")");
		}
		protocolId = protocolIdent;
	}

	public String getProtocolId() 
	{
		if (!operation.equals(DirMessageOps.OPERATION_PING))
		{
			throw new RuntimeException("DirMessage: getProtocolId called for message of unexpected type (" + operation + ")");
		}
		
		return protocolId;
	}

	// Getters y setters para status (valido para respuestas welcome, denied, file_list, register_files, server_list)
	public void setStatus(String status)
	{
		if (!(operation.equals(DirMessageOps.OPERATION_WELCOME) || operation.equals(DirMessageOps.OPERATION_DENIED) || operation.equals(DirMessageOps.OPERATION_FILE_LIST) || operation.equals(DirMessageOps.OPERATION_REGISTER_FILES) || operation.equals(DirMessageOps.OPERATION_SERVER_LIST))) 
		{
			throw new RuntimeException("Status field only valid for response messages");
		}
		
		this.status = status;
	}

	public String getStatus()
	{
		if (!(operation.equals(DirMessageOps.OPERATION_WELCOME) || operation.equals(DirMessageOps.OPERATION_DENIED) || operation.equals(DirMessageOps.OPERATION_FILE_LIST) || operation.equals(DirMessageOps.OPERATION_REGISTER_FILES) || operation.equals(DirMessageOps.OPERATION_SERVER_LIST) || operation.equals(DirMessageOps.OPERATION_INVALID))) 
		{
			throw new RuntimeException("Status field only valid for response messages");
		}
		return status;
	}
	
	// Getters y setters para message (valido para denied/invalid/error)
	public void setMessage(String message)
	{
		if (!operation.equals(DirMessageOps.OPERATION_DENIED))
		{
			throw new RuntimeException("Message field only valid for error responses");
		}
		
		this.message = message;
	}
	
	public String getMessage()
	{
		if (operation.equals("denied") || operation.equals("invalid") || operation.equals("error")) 
		{
	        return message;
	    }
		
		else 
		{
	        return null;
	    }
	}
	
	// Getters y setters para files (valido para file_list y register_files)
	public void setFiles(String[] files)
	{
		if (!operation.equals(DirMessageOps.OPERATION_FILE_LIST) && !operation.equals(DirMessageOps.OPERATION_REGISTER_FILES))
		{
			throw new RuntimeException("Files field only valid for files operations");
		}
		
		this.files = files;
	}
	
	public String[] getFiles()
	{
		if (!operation.equals(DirMessageOps.OPERATION_FILE_LIST) && !operation.equals(DirMessageOps.OPERATION_REGISTER_FILES))
		{
			throw new RuntimeException("Files field only valid for file operations");
		}
		
		return files;
	}
	
	// Getters y setters para serverAddres (valido para register_files)
	public void setServerAddress(String serverAddress)
	{
		if (!operation.equals(DirMessageOps.OPERATION_REGISTER_FILES))
		{
			throw new RuntimeException("Server address only valid for register operations");
		}
		
		this.serverAddress = serverAddress;
	}
	
	public String getServerAddres()
	{
		if (!operation.equals(DirMessageOps.OPERATION_REGISTER_FILES))
		{
			throw new RuntimeException("Server address only valid for register operations");
		}
		
		return serverAddress;
	}
	
	// Getters y setters para serverPort (valido para register_files y serve)
	public void setServerPort(int serverPort)
	{
		if (!(operation.equals(DirMessageOps.OPERATION_REGISTER_FILES) || operation.equals(DirMessageOps.OPERATION_SERVE))) 
		{
			throw new RuntimeException("Server port only valid for register operations");
		}
		
		this.serverPort = serverPort;
	}
	
	public int getServerPort()
	{
		if (!operation.equals(DirMessageOps.OPERATION_REGISTER_FILES))
		{
			throw new RuntimeException("Server port only valid for register operations");
		}
		
		return serverPort;
	}
	
	// Getters y setters para fileName (valido para download)
	public void setFileName(String fileName)
	{
		if (!operation.equals(DirMessageOps.OPERATION_DOWNLOAD))
		{
			throw new RuntimeException("Filename field only valid for download operations");
		}
		
		this.fileName = fileName;
	}
	
	public String getFileName()
	{
		if (!operation.equals(DirMessageOps.OPERATION_DOWNLOAD))
		{
			throw new RuntimeException("Filename field only valid for download operations");
		}
		
		return fileName;
	}
	
	// Getters y setters para servers (valido para server_list)
	public void setServers(String[] servers)
	{
		if (!operation.equals(DirMessageOps.OPERATION_SERVER_LIST))
		{
			throw new RuntimeException("Servers field only valid for server list operations");
		}
		
		this.servers = servers;
	}
	
	public String[] getServers()
	{
		if (!operation.equals(DirMessageOps.OPERATION_SERVER_LIST))
		{
			throw new RuntimeException("Servers field only valid for server list operations");
		}
		
		return servers;
	}
	

	/**
	 * Método que convierte un mensaje codificado como una cadena de caracteres, a
	 * un objeto de la clase PeerMessage, en el cual los atributos correspondientes
	 * han sido establecidos con el valor de los campos del mensaje.
	 * 
	 * @param message El mensaje recibido por el socket, como cadena de caracteres
	 * @return Un objeto PeerMessage que modela el mensaje recibido (tipo, valores,
	 *         etc.)
	 */
	public static DirMessage fromString(String message) 
	{
		// Dividimos el mensaje en líneas
		String[] lines = message.split(END_LINE + "");
		// Local variables to save data during parsing
		DirMessage m = null;

		for (String line : lines) 
		{
			int idx = line.indexOf(DELIMITER); // Posición del delimitador
			String fieldName = line.substring(0, idx).toLowerCase(); // minúsculas
			String value = line.substring(idx + 1).trim();

			switch (fieldName) 
			{
				case FIELDNAME_OPERATION: 
				{
					assert (m == null);
					m = new DirMessage(value);
					break;
				}
				
				case FIELDNAME_PROTOCOL:
				{
					if (m != null)
					{
						m.setProtocolID(value);
					}
					break;
				}
				
				case FIELDNAME_STATUS:
				{
					if (m != null)
					{
						m.setStatus(value);
					}
					break;
				}
				
				case FIELDNAME_FILENAME:
				{
				    if (m != null && m.getOperation().equals(DirMessageOps.OPERATION_DOWNLOAD))
				    {
				        m.setFileName(value);
				    }
				    break;
				}
				
				case FIELDNAME_MESSAGE:
				{
					if(m != null)
					{
						m.setMessage(value);
					}
					break;
				}
				
				case FIELDNAME_FILES:
				{
					if (m != null)
					{
						// Dividimos la lista de archivos por comas
						String[] fileArray = value.split(",");
						m.setFiles(fileArray);
					}
					break;
				}
				
				case FIELDNAME_SERVER:
				{
					if (m != null)
					{
						m.setServerAddress(value);
					}
					break;
				}
				
				case FIELDNAME_SERVERS:
				{
				    if (m != null && m.getOperation().equals(DirMessageOps.OPERATION_SERVER_LIST))
				    {
				        // Dividimos la lista de servidores por comas
				        String[] serverArray = value.split(",");
				        m.setServers(serverArray);
				    }
				    break;
				}
				
				case FIELDNAME_PORT:
				{
					if (m != null)
					{
						try {
							m.setServerPort(Integer.parseInt(value));
						} catch (NumberFormatException e) {
							System.out.println("Error parsing port number: " + value);
						}
					}
					break;
				}

				default:
					System.err.println("PANIC: DirMessage.fromString - message with unknown field name " + fieldName);
					System.err.println("Message was:\n" + message);
					System.exit(-1);
			}
		}

		// Verificamos que se ha creado un mensaje valido
		if (m == null)
		{
			System.err.println("PANIC: Dirmessage.fromString - no operation field found in message");
			System.err.println("Message was:\n" + message);
			System.exit(-1);
		}

		return m;
	}

	/**
     * Convierte el objeto DirMessage a una cadena de texto en formato campo:valor,
     * lista los campos relevantes según el tipo de operación.
     * @return String con el mensaje listo para enviar por el socket.
     */
	public String toString() 
	{
		StringBuffer sb = new StringBuffer();
		sb.append(FIELDNAME_OPERATION + DELIMITER + operation + END_LINE); // Construimos el campo
		
		// Añadimos campos segun el tipo de operacion
		switch (operation)
	    {
	        case DirMessageOps.OPERATION_PING:
	            sb.append(FIELDNAME_PROTOCOL + DELIMITER + protocolId + END_LINE);
	            break;

	        case DirMessageOps.OPERATION_WELCOME:
	        case DirMessageOps.OPERATION_DENIED:
	        case DirMessageOps.OPERATION_FILE_LIST:
	        case DirMessageOps.OPERATION_REGISTER_FILES:
	        case DirMessageOps.OPERATION_SERVER_LIST:
	        case DirMessageOps.OPERATION_INVALID:
	            // Mensajes de respuesta incluyen status y posiblemente un mensaje
	            sb.append(FIELDNAME_STATUS + DELIMITER + status + END_LINE);
	            if (message != null && !message.isEmpty())
	            {
	                sb.append(FIELDNAME_MESSAGE + DELIMITER + message + END_LINE);
	            }
	            break;
	    }

	    // Añadimos los campos específicos de cada tipo de mensaje
	    if (operation.equals(DirMessageOps.OPERATION_FILE_LIST) || operation.equals(DirMessageOps.OPERATION_REGISTER_FILES)) 
	    {
	        if (files != null && files.length > 0) 
	        {
	            sb.append(FIELDNAME_FILES + DELIMITER + String.join(",", files) + END_LINE);
	        }
	    }
	    
	    if (operation.equals(DirMessageOps.OPERATION_REGISTER_FILES)) 
	    {
	        sb.append(FIELDNAME_SERVER + DELIMITER + serverAddress + END_LINE);
	        sb.append(FIELDNAME_PORT + DELIMITER + serverPort + END_LINE);
	    }
	    
	    if (operation.equals(DirMessageOps.OPERATION_SERVER_LIST)) 
	    {
	        if (servers != null && servers.length > 0) 
	        {
	            sb.append(FIELDNAME_SERVERS + DELIMITER + String.join(",", servers) + END_LINE);
	        }
	    }
	    
	    if (operation.equals(DirMessageOps.OPERATION_DOWNLOAD)) 
	    {
	        if (fileName != null && !fileName.isEmpty()) 
	        {
	            sb.append(FIELDNAME_FILENAME + DELIMITER + fileName + END_LINE);
	        }
	    }
	    
	    sb.append(END_LINE); // Marcamos el final del mensaje
	    return sb.toString();
	}
}
