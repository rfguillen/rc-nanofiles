package es.um.redes.nanoFiles.udp.message;

public class DirMessageOps 
{
    // Operaciones básicas
	// Operación inválida (mensaje no reconocido o sin inicializar)
    public static final String OPERATION_INVALID = "invalid_operation";
    // Operación de ping: comprobar compatibilidad de protocolo
    public static final String OPERATION_PING = "ping";
    // Respuesta de bienvenida: protocolo compatible
    public static final String OPERATION_WELCOME = "welcome";
    // Respuesta de denegación: protocolo incompatible o error
    public static final String OPERATION_DENIED = "denied";
    
    // Operaciones de listado de archivos
    // Solicitud de lista de archivos publicados
    public static final String OPERATION_GET_FILES = "get_files";
    // Respuesta con la lista de archivos publicados
    public static final String OPERATION_FILE_LIST = "file_list";
    
    // Operaciones de registro de servidor
    // Solicitud de registro de servidor de ficheros
    public static final String OPERATION_SERVE = "serve";
    // Respuesta a registro de servidor (o baja)
    public static final String OPERATION_REGISTER_FILES = "register_files";
    
    // Operaciones de búsqueda de servidores
    // Solicitud de búsqueda de servidores que comparten un fichero
    public static final String OPERATION_DOWNLOAD = "download";
    // Respuesta con la lista de servidores que comparten un fichero
    public static final String OPERATION_SERVER_LIST = "server_list";
    
    // Operación de baja del servidor
    public static final String OPERATION_UNREGISTER = "unregister";
    
    // Estados de respuesta
    // operación realizada con éxito
    public static final String STATUS_OK = "ok";
    // Operación fallida
    public static final String STATUS_ERROR = "error";
    
    // Mensajes de error comunes
    // Protocolo incompatible
    public static final String ERROR_PROTOCOL = "incompatible_protocol";
    // Mensaje invalido
    public static final String ERROR_INVALID_MESSAGE = "invalid_message";
    // Recurso no encontrado (archivo o servidor)
    public static final String ERROR_NOT_FOUND = "not_found";
}