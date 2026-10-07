package es.um.redes.nanoFiles.shell;

import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.logic.NFControllerLogicDir;
import es.um.redes.nanoFiles.logic.NFControllerLogicP2P;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFShell 
{
	/**
	 * Scanner para leer comandos de usuario de la entrada estándar
	 */
	private Scanner reader;

	byte command = NFCommands.COM_INVALID;
	String[] commandArgs = new String[0];

	boolean enableComSocketIn = false;
	private boolean skipValidateArgs;

	/*
	 * Testing-related: print command to stdout (when reading commands from stdin)
	 */
	public static final String FILENAME_TEST_SHELL = ".nanofiles-test-shell";
	public static boolean enableVerboseShell = false;

	private NFControllerLogicDir logicDir;
	
	// Añadido: referencia a la lógica P2P (puede ser null si no se usa)
	private NFControllerLogicP2P logicP2P;

	/**
	 * Constructor del shell.
	 * @param logicP2P Lógica P2P para ejecutar comandos relacionados con el servidor de ficheros.
	 */
	public NFShell(NFControllerLogicDir logicDir, NFControllerLogicP2P logicP2P)
	{
		this.logicDir = logicDir;
		this.logicP2P = logicP2P;
		reader = new Scanner(System.in);

		System.out.println("NanoFiles shell");
		System.out.println("For help, type 'help'");
	}

	// Devuelve el comando introducido por el usuario
	public byte getCommand() 
	{
		return command;
	}

	// Devuelve los argumentos del comando introducido
	public String[] getCommandArguments()
	{
		return commandArgs;
	}

	/**
	 * Bucle que espera hasta obtener un comando válido del usuario.
	 * Valida los argumentos según el comando introducido.
	 */
	public void readGeneralCommand() 
	{
		boolean validArgs;
		do 
		{
			commandArgs = readGeneralCommandFromStdIn();
			// si el comando tiene parámetros hay que validarlos
			validArgs = validateCommandArguments(commandArgs);
		} while (!validArgs);
	}

	/**
	 * Permite al usuario elegir la dirección del servidor de directorio.
	 * Pregunta si se quiere usar la dirección por defecto o introducir otra.
	 * @param defaultDirectory Dirección por defecto sugerida
	 * @return Dirección seleccionada por el usuario
	 */
	public String chooseDirectory(String defaultDirectory) 
	{
		char response;
		String directory = null;
		do 
		{
			System.out.print(
					"Do you want to use '" + defaultDirectory + "' as location of the directory server? (y/n): ");
			String input = reader.nextLine().trim().toLowerCase();
			if (input.length() == 1)  // Verificar que la entrada es un solo carácter
			{	
				response = input.charAt(0);
				if (response == 'y') 
				{
					directory = defaultDirectory;
				} 
				
				else if (response == 'n') 
				{
					System.out.print("Enter the directory hostname/IP:");
					directory = reader.nextLine().trim().toLowerCase();
				} 
				
				else 
				{
					System.out.println("Invalid key! Please, answer 'y' or 'n'.");
				}
			}
		} while (directory == null);
		System.out.println("Using directory location: " + directory);
		return directory;
	}

	/**
	 * Lee una línea de comandos de la entrada estándar, la tokeniza y
	 * traduce el primer token a un comando válido.
	 * Devuelve el resto de tokens como argumentos.
	 * @return Array de argumentos del comando
	 */
	private String[] readGeneralCommandFromStdIn() 
	{
		String[] args = new String[0];
		Vector<String> vargs = new Vector<String>();
		while (true) 
		{
			System.out.print("(nanoFiles@" + NanoFiles.sharedDirname + ") ");
			// Obtenemos la línea tecleada por el usuario
			String input = reader.nextLine();
			StringTokenizer st = new StringTokenizer(input);
			// Si no hay ni comando entonces volvemos a empezar
			if (st.hasMoreTokens() == false) 
			{
				continue;
			}
			// Traducimos la cadena del usuario en el código de comando correspondiente
			command = NFCommands.stringToCommand(st.nextToken());
			if (enableVerboseShell) 
			{
				System.out.println(input);
			}
			skipValidateArgs = false;
			// Según el comando procesamos los argumentos
			switch (command) 
			{
				case NFCommands.COM_INVALID:
					// El comando no es válido
					System.out.println("Invalid command");
					continue;
				case NFCommands.COM_HELP:
					// Mostramos la ayuda
					NFCommands.printCommandsHelp();
					continue;
				case NFCommands.COM_QUIT:
				case NFCommands.COM_FILELIST:
				case NFCommands.COM_MYFILES:
				case NFCommands.COM_PING:
					// Estos comandos son válidos sin parámetros
					break;
				case NFCommands.COM_SERVE:
					// Permitir un argumento opcional (el puerto)
				    while (st.hasMoreTokens()) 
				    {
				        vargs.add(st.nextToken());
				    }
				    break;
				case NFCommands.COM_DOWNLOAD:
				case NFCommands.COM_UPLOAD:
					// Estos requieren dos parámetro
					while (st.hasMoreTokens()) 
					{
						vargs.add(st.nextToken());
					}
					break;
				default:
					// Comando no reconocido
					skipValidateArgs = true;
					System.out.println("Invalid command");
					;
			}
			break;
		}
		return vargs.toArray(args);
	}

	/**
	 * Valida el número de argumentos para los comandos que lo requieren.
	 * Muestra un mensaje de uso correcto si hay error.
	 * @param args Argumentos introducidos por el usuario
	 * @return true si los argumentos son correctos, false en caso contrario
	 */
	private boolean validateCommandArguments(String[] args) 
	{
		if (skipValidateArgs)
			return false;
		switch (this.command) 
		{
			case NFCommands.COM_DOWNLOAD:
				if (args.length != 2)
				{
					System.out.println("Correct use:" + NFCommands.commandToString(command) + " <filename_substring> <local_filename>");
					return false;
				}
				break;
			case NFCommands.COM_UPLOAD:
				if (args.length != 2) 
				{
					System.out.println("Correct use:" + NFCommands.commandToString(command) + " <filename_substring> <remote_server>");
					return false;
				}
				break;
				
			case NFCommands.COM_SERVE:
			    if (args.length > 1) 
			    {
			        System.out.println("Correct use: serve [port]");
			        return false;
			    }
			    break;
				
			default:
		}
		// El resto no requieren argumentos
		return true;
	}

	public static void enableVerboseShell() 
	{
		enableVerboseShell = true;
	}

	/**
	 * Bucle principal del shell: procesa comandos introducidos por el usuario.
	 * El shell recoge comandos y argumentos del usuario, y delega la ejecución 
	 * real en el controlador principal. La lógica de negocio no está aquí, 
	 * sino en los controladores
	 */
	public void runShellLoop() 
	{
	    boolean quit = false;
	    while (!quit) 
	    {
	        readGeneralCommand();
	        switch (command) 
	        {
	            case NFCommands.COM_HELP:
	                // Ya se imprime en readGeneralCommandFromStdIn
	                break;
	            case NFCommands.COM_QUIT:
	                quit = true;
	                break;
	            case NFCommands.COM_PING:
	                if (logicDir != null) {
	                    logicDir.ping();
	                } else {
	                    System.out.println("Funcionalidad de ping no disponible.");
	                }
	                break;
	            case NFCommands.COM_FILELIST:
	                if (logicDir != null) {
	                    logicDir.getAndPrintFileList();
	                } else {
	                    System.out.println("Funcionalidad de filelist no disponible.");
	                }
	                break;
	            case NFCommands.COM_MYFILES:
	                FileInfo.printToSysout(NanoFiles.db.getFiles());
	                break;
	            case NFCommands.COM_SERVE:
	                if (logicP2P != null) {
	                    int port = 10000; // Valor por defecto
	                    if (commandArgs.length == 1) {
	                        try {
	                            port = Integer.parseInt(commandArgs[0]);
	                        } catch (NumberFormatException e) {
	                            System.out.println("Invalid port, using default 10000");
	                        }
	                    }
	                    if (logicP2P.startFileServer(port)) {
	                        System.out.println("Servidor de ficheros arrancado en segundo plano en el puerto " + port + ".");
	                    } else {
	                        System.out.println("No se pudo arrancar el servidor de ficheros.");
	                    }
	                } else {
	                    System.out.println("Funcionalidad P2P no disponible.");
	                }
	                break;
	            case NFCommands.COM_DOWNLOAD:
	                // Solo recoge argumentos, la lógica real está en NFController
	                break;
	            case NFCommands.COM_UPLOAD:
	                if (logicP2P != null) {
	                    String[] args = getCommandArguments();
	                    if (args.length == 2) {
	                        FileInfo[] matches = FileInfo.lookupFilenameSubstring(NanoFiles.db.getFiles(), args[0]);
	                        if (matches.length == 1) {
	                            logicP2P.uploadFileToServer(matches[0], args[1]);
	                        } else if (matches.length == 0) {
	                            System.out.println("No se encontró ningún archivo que contenga: " + args[0]);
	                        } else {
	                            System.out.println("Subcadena ambigua, varios archivos coinciden:");
	                            FileInfo.printToSysout(matches);
	                        }
	                    } else {
	                        System.out.println("Uso correcto: upload <filename_substring> <remote_server>");
	                    }
	                } else {
	                    System.out.println("Funcionalidad de upload no disponible.");
	                }
	                break;
	            default:
	                System.out.println("Comando no implementado en el shell.");
	        }
	    }
	    System.out.println("Bye.");
	}
}