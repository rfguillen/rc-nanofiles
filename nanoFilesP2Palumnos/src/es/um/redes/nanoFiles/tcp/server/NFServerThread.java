package es.um.redes.nanoFiles.tcp.server;

import java.io.IOException;
import java.net.Socket;

/**
 * Clase que modela los hilos de atención a clientes en el servidor TCP de NanoFiles.
 * 
 * Cada vez que un cliente se conecta al servidor (NFServer), se crea un objeto de esta clase,
 * que hereda de Thread. El hilo se encarga de gestionar la comunicación con ese cliente
 * de forma concurrente, permitiendo que el servidor atienda a varios clientes a la vez.
 */
public class NFServerThread extends Thread 
{
	// Socket asociado a la conexión con el cliente
	private Socket clientSocket;

	/**
     * Constructor.
     * @param clientSocket Socket devuelto por ServerSocket.accept(), representa la conexión con un cliente.
     */
    public NFServerThread(Socket clientSocket) 
    {
        this.clientSocket = clientSocket;
    }

    /**
     * Método principal del hilo.
     * Atiende la comunicación con el cliente usando el protocolo binario.
     * Al terminar, cierra el socket del cliente.
     */
    public void run() 
    {
    	// Llama al método que implementa el protocolo de transferencia de ficheros
        NFServer.serveFilesToClient(clientSocket);
        try {
        	// Cierra el socket del cliente al finalizar la comunicación
            clientSocket.close();
        } catch (IOException e) {
        	// No se hace nada si falla el cierre, ya que el hilo termina igualmente
        }
    }
}
