package es.um.redes.nanoFiles.application;

import es.um.redes.nanoFiles.udp.server.NFDirectoryServer;
import java.io.IOException;
import java.net.SocketException;

/**
 * Clase principal para lanzar el servidor de directorio de NanoFiles.
 * Procesa argumentos de línea de comandos, inicializa el servidor UDP y ejecuta el bucle principal.
 */
public class Directory 
{
	// Probabilidad por defecto de corrupción de datagramas UDP (sin pérdidas)
    public static final double DEFAULT_CORRUPTION_PROBABILITY = 0.0;

    
    /**
    * El Método principal. Procesa los argumentos, inicializa el servidor de directorio
    * 	y lanza el bucle principal.
    * 
 	* @param args Argumentos de la línea de comandos. 
 	*        -loss <probabilidad> para simular pérdidas de datagramas UDP.
 	*/
    public static void main(String[] args) 
    {
        double datagramCorruptionProbability = DEFAULT_CORRUPTION_PROBABILITY;

        /**
         * El argumento de la línea de comandos es opcional. Si no se específica,
         * 		se utiliza el valor por defecto
         * Uso: - loss <probabilidad> para indicar la probabilidad de corrupcion
         * 		de los datagramas recibidos
         */
        
        // Procesamiento de argumentos de línea de comandos
        String arg;
        if (args.length > 0 && args[0].startsWith("-")) 
        {
            arg = args[0];
            // // Si el argumento es "-loss", procesamos la probabilidad de descarte
            if (arg.equals("-loss")) 
            {
                if (args.length == 2) 
                {
                    try {
                        // El segundo argumento contiene la probabilidad de descarte
                        datagramCorruptionProbability = Double.parseDouble(args[1]);
                    } catch (NumberFormatException e) {
                        System.err.println("Wrong value passed to option " + arg);
                        return;
                    }
                } 
                
                else 
                {
                    System.err.println("option " + arg + " requires a value");
                }
            } 

            else 
            {
                System.err.println("Illegal option " + arg);
            }
        }
        
        // Mostramos la probabilidad de corrupción configurada
        System.out.println("Probability of corruption for received datagrams: " + datagramCorruptionProbability);
        try {
        	// Creamos el servidor de directorio, pasándole la probabilidad de corrupción
            NFDirectoryServer dir = new NFDirectoryServer(datagramCorruptionProbability);
            // Si está activado el modo test UDP, se ejecuta el servidor en modo test
            if (NanoFiles.testModeUDP) 
            {
                dir.runTest();
            } 
            // Como no está activo el modo test UDP, ejecutamos el servidor en modo normal
            else 
            {
                dir.run();
            }
        } catch (SocketException e) {
        	// El puerto UDP ya está en uso
            System.err.println("Directory cannot create UDP socket");
            System.err.println("Most likely a Directory process is already running and listening on that port...");
            System.exit(-1);
        } catch (IOException e) {
        	// Otros errores
            e.printStackTrace();
            System.err.println("Unexpected I/O error when running NFDirectoryServer.run");
            System.exit(-1);
        }
    }
}