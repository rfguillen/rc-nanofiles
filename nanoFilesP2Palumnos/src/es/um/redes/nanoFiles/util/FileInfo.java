package es.um.redes.nanoFiles.util;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import es.um.redes.nanoFiles.shell.NFShell;

/**
 * @author rtitos
 * 
 * Clase de utilidad con métodos estáticos para abstraer el manejo de metadatos de ficheros,
 * carga de ficheros compartidos, búsqueda por subcadena de nombre, etc.
 * 
 * Esta clase representa la información de un fichero compartido en el sistema NanoFiles.
 * Permite gestionar tanto ficheros locales como remotos, y almacenar la lista de servidores
 * que comparten cada fichero.
 */
public class FileInfo 
{
	// Hash del fichero
	public String fileHash;
	// Nombre del fichero
	public String fileName;
	// Ruta local del fichero en el sistema de archivos
	public String filePath;
	// Tamaño del fichero en bytes
	public long fileSize = -1;
	// Conjunto de servidores (en formato "ip:puerto") que comparten este fichero
	private HashSet<String> servers;
	// Lista de servidores para el filelist ampliado
	public String[] serversList;

	/**
     * Constructor por defecto. Inicializa el conjunto de servidores con "Unknown Origin".
     */
	public FileInfo() 
	{
		servers = new HashSet<String>(1);
		servers.add("Unknown Origin");
	}
	
	/**
     * Constructor principal para ficheros locales.
     * 
     * @param hash  Hash del fichero (SHA-1)
     * @param name  Nombre del fichero
     * @param size  Tamaño en bytes
     * @param path  Ruta local del fichero
     */
	public FileInfo(String hash, String name, long size, String path) 
	{
		fileHash = hash;
		fileName = name;
		fileSize = size;
		filePath = path;
		servers = new HashSet<String>(1);
		servers.add("Unknown Origin");
	}
	
	/**
     * Constructor para crear un FileInfo a partir de otro y un conjunto de servidores.
     * 
     * @param file    Objeto FileInfo base
     * @param servers Conjunto de servidores que comparten el fichero
     */
	public FileInfo(FileInfo file, java.util.Set<String> servers) 
	{
	    this.fileHash = file.fileHash;
	    this.fileName = file.fileName;
	    this.filePath = file.filePath;
	    this.fileSize = file.fileSize;
	    this.servers = new HashSet<>(servers);
	}
	
	/**
     * Constructor para filelist ampliado (incluye lista de servidores).
     * 
     * @param hash        Hash del fichero
     * @param name        Nombre del fichero
     * @param size        Tamaño en bytes
     * @param path        Ruta local (puede ser null)
     * @param serversList Lista de servidores en formato "ip:puerto"
     */
	public FileInfo(String hash, String name, long size, String path, String[] serversList) 
    {
        fileHash = hash;
        fileName = name;
        fileSize = size;
        filePath = path;
        this.serversList = serversList;
    }

	/**
     * Devuelve una representación en String del fichero, incluyendo nombre, tamaño, hash y servidores.
     */
	public String toString() 
	{
		StringBuilder strBuf = new StringBuilder();
        strBuf.append(String.format("%-30s", fileName));
        strBuf.append(String.format("%10s", fileSize));
        strBuf.append(String.format(" %-45s", fileHash));
        if (serversList != null && serversList.length > 0)
        {
            strBuf.append(" Servidores: ");
            for (int i = 0; i < serversList.length; i++)
            {
                strBuf.append(serversList[i]);
                if (i < serversList.length - 1) strBuf.append(", ");
            }
        }
        return strBuf.toString();
    }

	/**
     * Imprime por pantalla una tabla con la información de los ficheros.
     * 
     * @param files Array de FileInfo a mostrar
     */
	public static void printToSysout(FileInfo[] files) 
	{
		StringBuffer strBuf = new StringBuffer();
		strBuf.append(String.format("%1$-30s", "Name"));
		strBuf.append(String.format("%1$10s", "Size"));
		strBuf.append(String.format(" %1$-45s", "Hash"));
		System.out.println(strBuf);
		for (FileInfo file : files) 
		{
			System.out.println(file);
		}
	}
	
	// Getters
	public String getHash()
	{
		return fileHash;
	}
	
	public String getName()
	{
		return fileName;
	}
	
	public long getSize()
	{
		return fileSize;
	}
	
	public String getPath()
	{
		return filePath;
	}
	
	public String[] getServersList() 
	{
        return serversList;
    }

	/**
     * Escanea recursivamente una carpeta y devuelve un array de FileInfo de todos los ficheros encontrados.
     * 
     * @param sharedFolderPath Ruta de la carpeta a escanear
     * @return Array de FileInfo con los metadatos de los ficheros encontrados
     */
	public static FileInfo[] loadFilesFromFolder(String sharedFolderPath) 
	{
		File folder = new File(sharedFolderPath);

		List<FileInfo> files = FileInfo.loadFileListFromFolder(folder);

		FileInfo[] fileinfoarray = new FileInfo[files.size()];
		Iterator<FileInfo> itr = files.iterator();
		int numFiles = 0;
		while (itr.hasNext()) 
		{
			fileinfoarray[numFiles++] = itr.next();
		}
		return fileinfoarray;
	}

	/**
     * Escanea recursivamente una carpeta y devuelve un mapa <hash, FileInfo> de todos los ficheros encontrados.
     * 
     * @param folder Carpeta a escanear
     * @return Mapa de hash a FileInfo
     */
	protected static List<FileInfo> loadFileListFromFolder(final File folder) 
	{
    	List<FileInfo> files = new ArrayList<FileInfo>();
    	scanFolderRecursive(folder, files);
    	return files;
	}

	/**
     * Método auxiliar recursivo para escanear carpetas y subcarpetas.
     * 
     * @param folder Carpeta actual
     * @param files  Mapa donde se añaden los ficheros encontrados
     */
	private static void scanFolderRecursive(final File folder, List<FileInfo> files) 
	{
		if (folder.exists() == false) 
		{
			System.err.println("scanFolder cannot find folder " + folder.getPath());
			return;
		}
		
		if (folder.canRead() == false) 
		{
			System.err.println("scanFolder cannot access folder " + folder.getPath());
			return;
		}

		for (final File fileEntry : folder.listFiles()) 
		{
			if (fileEntry.isDirectory()) 
			{
				scanFolderRecursive(fileEntry, files);
			} 
			
			else 
			{
				String fileName = fileEntry.getName();
				String filePath = fileEntry.getPath();
				String fileHash = FileDigest.computeFileChecksumString(filePath);
				long fileSize = fileEntry.length();
				if (fileSize > 0) 
				{
					files.add(new FileInfo(fileHash, fileName, fileSize, filePath));
				} 
				
				else 
				{
					if (fileName.equals(NFShell.FILENAME_TEST_SHELL)) 
					{
						NFShell.enableVerboseShell();
						System.out.println("[Enabling verbose shell]");
					} 
					
					else 
					{
						System.out.println("Ignoring empty file found in shared folder: " + filePath);
					}
				}
			}
		}
	}

	/**
     * Busca ficheros cuyo nombre contenga una subcadena
     * 
     * @param files           Array de FileInfo donde buscar
     * @param filenameSubstr  Subcadena a buscar
     * @return Array de FileInfo que coinciden con la subcadena
     */
	public static FileInfo[] lookupFilenameSubstring(FileInfo[] files, String filenameSubstr) 
	{
	    String needle = filenameSubstr.toLowerCase();
	    java.util.List<FileInfo> matches = new java.util.ArrayList<>();
	    for (FileInfo file : files) 
	    {
	        if (file.fileName != null && file.fileName.toLowerCase().contains(needle)) 
	        {
	            matches.add(file);
	        }
	    }
	    return matches.toArray(new FileInfo[0]);
	}
	
	/**
     * Devuelve la lista de servidores (InetSocketAddress) que comparten este fichero.
     * 
     * @return Array de InetSocketAddress de los servidores
     */
	public InetSocketAddress[] getServers()
	{
		HashSet<InetSocketAddress> peerSet = new HashSet<>();
		for (var peer: servers)
		{
			String[] peerFields = peer.split(":");
			if (peerFields.length != 2)
			{
				continue;
			}
			InetAddress addr = null;
			try {
				addr = InetAddress.getByName(peerFields[0]);
			} catch (UnknownHostException e) {
				continue;
			}
			
			int port = Integer.parseInt(peerFields[1]);
			peerSet.add(new InetSocketAddress(addr, port));
		}
		
		return peerSet.toArray(new InetSocketAddress[0]);
	}
	
	/**
     * Añade un servidor a la lista de servidores que comparten este fichero.
     * 
     * @param host IP o nombre del host
     * @param port Puerto TCP
     */
	public void addServer(String host, int port)
	{
		servers.add(host + ":" + port);
	}
	
	/**
     * Añade varios servidores de golpe.
     * 
     * @param servers Colección de servidores en formato "ip:puerto"
     */
	public void addServers(java.util.Collection<String> servers) 
	{
	    this.servers.addAll(servers);
	}
	
	/**
     * Elimina un servidor de la lista.
     * 
     * @param host IP o nombre del host
     * @param port Puerto TCP
     */
	public void removeServer(String host, int port)
	{
		servers.remove(host + ":" + port);
	}
	
	/**
     * Crea un FileInfo remoto a partir de los datos recibidos del directorio.
     * 
     * @param hash        Hash del fichero
     * @param name        Nombre del fichero
     * @param size        Tamaño en bytes
     * @param path        Ruta local (puede ser null)
     * @param serversList Lista de servidores en formato "ip:puerto,ip:puerto"
     * @return FileInfo con la información y los servidores añadidos
     */
	public static FileInfo fromDirectory(String hash, String name, long size, String path, String serversList) 
	{
	    FileInfo info = new FileInfo(hash, name, size, path);
	    if (serversList != null && !serversList.isEmpty()) 
	    {
	        String[] servers = serversList.split(",");
	        for (String s : servers) {
	            String[] parts = s.split(":");
	            if (parts.length == 2) 
	            {
	                info.addServer(parts[0], Integer.parseInt(parts[1]));
	            }
	        }
	    }
	    return info;
	}
	
	/**
     * Busca un fichero por su hash en un array de FileInfo.
     * 
     * @param files Array de FileInfo
     * @param hash  Hash a buscar
     * @return FileInfo correspondiente, o null si no se encuentra
     */
	public static FileInfo lookupHash(FileInfo[] files, String hash) 
	{
	    for (FileInfo file : files) 
	    {
	        if (file.getHash().equals(hash)) 
	        {
	            return file;
	        }
	    }
	    return null;
	}	
}
