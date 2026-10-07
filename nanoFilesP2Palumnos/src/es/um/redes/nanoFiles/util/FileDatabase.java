package es.um.redes.nanoFiles.util;

import java.io.File;
import java.util.List;

/**
 * @author rtitos
 * 
 *  Esta clase actúa como una "base de datos" de los ficheros locales que el peer comparte.
 * 	Permite cargar los archivos de una carpeta, consultarlos y buscar rutas por hash.
 */

public class FileDatabase 
{
	// Mapa que asocia el hash de cada fichero con su FileInfo correspondiente
	private List<FileInfo> files;

	/**
     * Constructor: carga todos los ficheros de la carpeta compartida indicada.
     * Si la carpeta no existe, la crea.
     * Si no hay ficheros, muestra una advertencia.
     * 
     * @param sharedFolder Ruta a la carpeta compartida por este peer.
     */
	public FileDatabase(String sharedFolder) 
	{
		File theDir = new File(sharedFolder);
		if (!theDir.exists()) 
		{
			theDir.mkdirs();
		}
		this.files = FileInfo.loadFileListFromFolder(new File(sharedFolder));
		if (files.size() == 0) 
		{
			System.err.println("*WARNING: No files found in folder " + sharedFolder);
		}
	}

	/**
     * Devuelve un array con todos los ficheros compartidos por este peer.
     * 
     * @return Array de FileInfo con los metadatos de los ficheros compartidos.
     */
	public FileInfo[] getFiles() 
	{
		return files.toArray(new FileInfo[0]);
	}

	/**
     * Busca la ruta local de un fichero a partir de su hash.
     * 
     * @param fileHash Hash del fichero a buscar.
     * @return Ruta local del fichero, o null si no se encuentra.
     */
	public String lookupFilePath(String fileHash) 
	{
		for (FileInfo f : files)
		{
			if (f.fileHash.equals(fileHash))
			{
				return f.filePath;
			}
		}

		return null;
	}
}
