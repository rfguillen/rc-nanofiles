package es.um.redes.nanoFiles.application;

import es.um.redes.nanoFiles.logic.NFController;
import es.um.redes.nanoFiles.util.FileDatabase;

public class NanoFiles 
{
	// Carpeta compartida por defecto
    public static final String DEFAULT_SHARED_DIRNAME = "nf-shared";
    /**
     * Identificador único para cada grupo de prácticas. TODO: Establecer a un valor
     * que combine los DNIs de ambos miembros del grupo de prácticas.
     */
    public static final String PROTOCOL_ID = "45536916"; // La suma de los dos DNIs
    
    // Hostname del directorio por defecto
    private static final String DEFAULT_DIRECTORY_HOSTNAME = "localhost";
    // Carpeta compartida actual
    public static String sharedDirname = DEFAULT_SHARED_DIRNAME;
    // Base de datos de ficheros compartidos por este peer
    public static FileDatabase db;
    /**
     * Flag para pruebas iniciales con UDP, desactivado una vez que la comunicación
     * cliente-directorio está implementada y probada.
     */
    public static boolean testModeUDP = false;
    /**
     * Flag para pruebas iniciales con TCP, desactivado una vez que la comunicación
     * cliente-servidor de ficheros está implementada y probada.
     */
    public static boolean testModeTCP = false;

    
    /**
     * Método principal. Procesa argumentos, inicializa la base de datos de ficheros,
     * y lanza el controlador principal.
     * 
     * @param args Argumentos de la línea de comandos. 
     *        [<local_shared_directory>] para especificar la carpeta compartida.
     */
    public static void main(String[] args) 
    {
        // Comprobamos los argumentos
        if (args.length > 1) 
        {
            System.out.println("Usage: java -jar NanoFiles.jar [<local_shared_directory>]");
            return;
        } 
        
        else if (args.length == 1) 
        {
            // Establecemos el directorio compartido si se especifica
            sharedDirname = args[0];
        }
        
        // Inicializamos la base de datos de ficheros compartidos
        db = new FileDatabase(sharedDirname);

        // Creamos el controlador que aceptará y procesará los comandos
        NFController controller = new NFController(DEFAULT_DIRECTORY_HOSTNAME);
        
        // Si está activado el modo test UDP, ejecutamos la prueba de comunicación UDP
        if (testModeUDP) 
        {
            controller.testCommunication();
        } 
        
        else 
        {
        	// Bucle principal: leemos y procesamos comandos del shell hasta que el usuario sale
            do 
            {
                controller.readGeneralCommandFromShell();
                controller.processCommand();
            } while (controller.shouldQuit() == false);
            System.out.println("Bye.");
        }
    }
}