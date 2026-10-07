package es.um.redes.nanoFiles.tcp.message;

import java.util.Map;
import java.util.TreeMap;


/**
 * Clase de utilidades y constantes para el protocolo binario entre peers de NanoFiles.
 * Define los opcodes (tipos de mensaje) y proporciona métodos para convertir entre
 * opcodes y sus representaciones textuales.
 * Los opcodes aquí definidos corresponden a los distintos tipos de mensajes binarios
 * que intercambian cliente y servidor de ficheros.
 * Esta clase es utilizada por PeerMessage, NFConnector y NFServer para identificar
 * y procesar los mensajes del protocolo TCP.
 */
public class PeerMessageOps 
{
	// Opcode para mensajes inválidos o desconocidos
	public static final byte OPCODE_INVALID_CODE = 0x00;

    // Opcodes del protocolo binario (mensajes de petición y control)
	// Solicitud de fichero por hash
    public static final byte OPCODE_FILEREQUEST = 0x01;
    // Solicitud de chunk de fichero
    public static final byte OPCODE_CHUNKREQUEST = 0x02;
    // Petición de parada
    public static final byte OPCODE_STOP = 0x03;
    // Solicitud de subida de fichero
    public static final byte OPCODE_UPLOAD = 0x04;
    // Envío de nombre de fichero para subida
    public static final byte OPCODE_FILENAME_TO_SAVE = 0x05;

    // Opcodes de respuesta y control
    // El servidor acepta la solicitud de fichero
    public static final byte OPCODE_FILEREQUEST_ACCEPTED = 0x11;
    // El fichero solicitado no existe
    public static final byte OPCODE_FILE_NOT_FOUND = 0x12;
    // Respuesta con datos de un chunk
    public static final byte OPCODE_CHUNK = 0x13;
    // chunk solicitado está fuera de rango
    public static final byte OPCODE_CHUNKREQUEST_OUTOFRANGE = 0x14;
    // El fichero ya existe en el servidor
    public static final byte OPCODE_FILE_ALREADY_EXISTS = 0x15;

    // Listas de opcodes y nombres de operación válidos
	private static final Byte[] _valid_opcodes = {
		OPCODE_INVALID_CODE,
		OPCODE_FILEREQUEST,
		OPCODE_CHUNKREQUEST,
		OPCODE_STOP,
		OPCODE_UPLOAD,
		OPCODE_FILENAME_TO_SAVE,
		OPCODE_FILEREQUEST_ACCEPTED,
		OPCODE_FILE_NOT_FOUND,
		OPCODE_CHUNK,
		OPCODE_CHUNKREQUEST_OUTOFRANGE,
		OPCODE_FILE_ALREADY_EXISTS
	};
	
	private static final String[] _valid_operations_str = {
		"INVALID_CODE",
		"FILEREQUEST",
		"CHUNKREQUEST",
		"STOP",
		"UPLOAD",
		"FILENAME_TO_SAVE",
		"FILEREQUEST_ACCEPTED",
		"FILE_NOT_FOUND",
		"CHUNK",
		"CHUNKREQUEST_OUTOFRANGE",
		"FILE_ALREADY_EXISTS"
	};

	// Mapas para conversión entre opcode y nombre textual
	private static Map<String, Byte> _operation_to_opcode;
	private static Map<Byte, String> _opcode_to_operation;

	// Inicialización estática de los mapas de conversión
	static 
	{
		_operation_to_opcode = new TreeMap<>();
		_opcode_to_operation = new TreeMap<>();
		for (int i = 0; i < _valid_operations_str.length; ++i) 
		{
			_operation_to_opcode.put(_valid_operations_str[i].toLowerCase(), _valid_opcodes[i]);
			_opcode_to_operation.put(_valid_opcodes[i], _valid_operations_str[i]);
		}
	}

	/**
     * Transforma una cadena (nombre de operación) en el opcode correspondiente.
     * Si la operación no es válida, devuelve OPCODE_INVALID_CODE.
     */
	protected static byte operationToOpcode(String opStr) 
	{
		return _operation_to_opcode.getOrDefault(opStr.toLowerCase(), OPCODE_INVALID_CODE);
	}

	/**
     * Transforma un opcode en la cadena correspondiente (nombre de operación).
     * Si el opcode no es válido, devuelve null.
     */
	public static String opcodeToOperation(byte opcode) 
	{
		return _opcode_to_operation.getOrDefault(opcode, null);
	}
	
	/**
     * Comprueba si un opcode es válido según el protocolo.
     */
	public static boolean isValidOpcode(byte opcode)
	{
		return _opcode_to_operation.containsKey(opcode);
	}
}

