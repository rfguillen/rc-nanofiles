package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Clase que modela los mensajes binarios intercambiados entre peers en el protocolo TCP de NanoFiles.
 * Cada mensaje tiene un opcode que determina su tipo y los campos relevantes para cada tipo de mensaje.
 * Los métodos de serialización y deserialización permiten enviar y recibir mensajes a través de streams binarios.
 */
public class PeerMessage
{
	// Opcode que identifica el tipo de mensaje
    private byte opcode;

    // Campos para los distintos tipos de mensaje
    private byte[] reqFileHash; //para mensajes que identifican un fichero (hash de 20 bytes o nombre de fichero)
    private long offset; // desplazamiento dentro del fichero (para chunks)
    private int size; // tamaño del chunk o longitud del nombre de fichero
    private byte[] chunkData; // datos binarios de un fragmento de fichero
    
    /**
     * Constructor por defecto. Crea un mensaje inválido.
     */
    public PeerMessage()
    {
        opcode = PeerMessageOps.OPCODE_INVALID_CODE;
    }

    /**
     * Constructor para mensajes de control sin datos extra.
     * @param op Opcode del mensaje.
     */
    public PeerMessage(byte op)
    {
        opcode = op;
    }

    /**
     * Constructor para mensajes que incluyen un hash de fichero (como FILEREQUEST o UPLOAD).
     * @param op Opcode del mensaje.
     * @param reqFileHash Hash del fichero en formato hexadecimal.
     */
    public PeerMessage(byte op, String reqFileHash)
    {
        opcode = op;
        this.reqFileHash = hexStringToBytes(reqFileHash);
    }

    /**
     * Constructor para mensajes que incluyen un nombre de fichero (como FILENAME_TO_SAVE).
     * @param op Opcode del mensaje.
     * @param size Longitud del nombre de fichero.
     * @param fileName Nombre del fichero.
     */
    public PeerMessage(byte op, int size, String fileName)
    {
        this.opcode = op;
        this.size = size;
        this.reqFileHash = fileName.getBytes(); // En este mensaje, reqFileHash almacena el nombre del fichero
    }

    /**
     * Constructor para mensajes de chunk (datos de un fragmento de fichero).
     * @param op Opcode del mensaje.
     * @param offset Desplazamiento dentro del fichero.
     * @param size Tamaño del chunk.
     * @param chunkData Datos binarios del chunk.
     */
    public PeerMessage(byte op, long offset, int size, byte[] chunkData)
    {
        opcode = op;
        this.offset = offset;
        this.size = size;
        this.chunkData = chunkData;
    }

    /**
     * Constructor para mensajes de solicitud de chunk (sin datos).
     * @param op Opcode del mensaje.
     * @param offset Desplazamiento dentro del fichero.
     * @param size Tamaño del chunk solicitado.
     */
    public PeerMessage(byte op, long offset, int size)
    {
        opcode = op;
        this.offset = offset;
        this.size = size;
    }

    /**
     * Devuelve el opcode del mensaje.
     */
    public byte getOpcode()
    {
        return opcode;
    }

    /**
     * Establece el opcode del mensaje.
     */
    public void setOpcode(byte opcode)
    {
        this.opcode = opcode;
    }

    /**
     * Devuelve el hash del fichero en formato hexadecimal (String).
     */
    public String getReqFileHash()
    {
        if (reqFileHash == null) return null;
        return bytesToHex(reqFileHash);
    }

    /**
     * Establece el hash del fichero a partir de una cadena hexadecimal.
     */
    public void setReqFileHash(String reqFileHash)
    {
        this.reqFileHash = hexStringToBytes(reqFileHash);
    }

    /**
     * Establece el hash del fichero a partir de un array de bytes.
     */
    public void setReqFileHash(byte[] reqFileHash)
    {
        this.reqFileHash = reqFileHash;
    }

    /**
     * Devuelve el offset (desplazamiento) del chunk.
     */
    public long getOffset()
    {
        return offset;
    }

    /**
     * Establece el offset (desplazamiento) del chunk.
     */
    public void setOffset(long offset)
    {
        this.offset = offset;
    }

    /**
     * Devuelve el tamaño del chunk o del campo relevante.
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Establece el tamaño del chunk o del campo relevante.
     */
    public void setSize(int size)
    {
        this.size = size;
    }

    /**
     * Devuelve los datos binarios del chunk.
     */
    public byte[] getChunkData()
    {
        return chunkData;
    }
    
    /**
     * Establece los datos binarios del chunk.
     */
    public void setData(byte[] chunkData)
    {
        this.chunkData = chunkData;
    }

    /**
     * Devuelve el nombre de fichero (solo válido para OPCODE_FILENAME_TO_SAVE).
     * @return Nombre del fichero como String.
     * @throws IllegalStateException si el opcode no es OPCODE_FILENAME_TO_SAVE.
     */
    public String getFileName()
    {
        if (opcode != PeerMessageOps.OPCODE_FILENAME_TO_SAVE)
        {
            throw new IllegalStateException("getFileName() solo es válido para OPCODE_FILENAME_TO_SAVE");
        }
        return new String(reqFileHash); // reqFileHash contiene el nombre del fichero en este caso
    }

    /**
     * Establece el nombre de fichero (solo válido para OPCODE_FILENAME_TO_SAVE).
     */
    public void setFileName(String fileName)
    {
        if (opcode != PeerMessageOps.OPCODE_FILENAME_TO_SAVE)
        {
            throw new IllegalStateException("setFileName() solo es válido para OPCODE_FILENAME_TO_SAVE");
        }
        this.reqFileHash = fileName.getBytes();
        this.size = fileName.length();
    }


    /**
     * Lee un mensaje PeerMessage desde un DataInputStream y lo construye según el opcode.
     * @param dis Stream de entrada binario.
     * @return Objeto PeerMessage con los campos rellenados.
     * @throws IOException Si ocurre un error de E/S.
     */
    public static PeerMessage readMessageFromInputStream(DataInputStream dis) throws IOException
    {
        PeerMessage message = new PeerMessage();
        byte opcode = dis.readByte();
        message.opcode = opcode;
        switch (opcode)
        {
        	case PeerMessageOps.OPCODE_UPLOAD:
        	case PeerMessageOps.OPCODE_FILEREQUEST:
        	    message.reqFileHash = dis.readNBytes(20);
        	    break;
        	case PeerMessageOps.OPCODE_CHUNKREQUEST:
        	    message.offset = dis.readLong();
        	    message.size = dis.readInt();
        	    message.reqFileHash = dis.readNBytes(20);
        	    break;
        	case PeerMessageOps.OPCODE_CHUNK:
        	    message.offset = dis.readLong();
        	    message.size = dis.readInt();
        	    message.chunkData = dis.readNBytes(message.size);
        	    break;
        	case PeerMessageOps.OPCODE_FILENAME_TO_SAVE:
        	    message.size = dis.readInt();
        	    message.reqFileHash = dis.readNBytes(message.size);
        	    break;
        	// Mensajes de control sin datos extra
        	case PeerMessageOps.OPCODE_STOP:
        	case PeerMessageOps.OPCODE_FILEREQUEST_ACCEPTED:
        	case PeerMessageOps.OPCODE_FILE_NOT_FOUND:
        	case PeerMessageOps.OPCODE_CHUNKREQUEST_OUTOFRANGE:
        	case PeerMessageOps.OPCODE_FILE_ALREADY_EXISTS:
        	    break;
        	default:
        	    System.err.println("PeerMessage.readMessageFromInputStream: opcode desconocido " + opcode);
        }
        return message;
    }
    
    /**
     * Escribe el mensaje PeerMessage en un DataOutputStream según el opcode.
     * @param dos Stream de salida binario.
     * @throws IOException Si ocurre un error de E/S.
     */
    public void writeMessageToOutputStream(DataOutputStream dos) throws IOException
    {
        dos.writeByte(opcode);
        switch (opcode)
        {
        	case PeerMessageOps.OPCODE_UPLOAD:
        	case PeerMessageOps.OPCODE_FILEREQUEST:
        	    dos.write(reqFileHash);
        	    break;
        	case PeerMessageOps.OPCODE_CHUNKREQUEST:
        	    dos.writeLong(offset);
        	    dos.writeInt(size);
        	    dos.write(reqFileHash);
        	    break;
        	case PeerMessageOps.OPCODE_CHUNK:
        	    dos.writeLong(offset);
        	    dos.writeInt(size);
        	    dos.write(chunkData);
        	    break;
        	case PeerMessageOps.OPCODE_FILENAME_TO_SAVE:
        	    dos.writeInt(size);
        	    dos.write(reqFileHash);
        	    break;
        	// Mensajes de control sin datos extra
        	case PeerMessageOps.OPCODE_STOP:
        	case PeerMessageOps.OPCODE_FILEREQUEST_ACCEPTED:
        	case PeerMessageOps.OPCODE_FILE_NOT_FOUND:
        	case PeerMessageOps.OPCODE_CHUNKREQUEST_OUTOFRANGE:
        	case PeerMessageOps.OPCODE_FILE_ALREADY_EXISTS:
        	    break;
        	default:
        	    System.err.println("PeerMessage.writeMessageToOutputStream: opcode desconocido " + opcode);
        }
    }

    // Utilidades para convertir entre String y byte[]
    
    /**
     * Convierte una cadena hexadecimal en un array de bytes.
     * @param s Cadena hexadecimal.
     * @return Array de bytes correspondiente.
     */
    private static byte[] hexStringToBytes(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                  + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Convierte un array de bytes en una cadena hexadecimal.
     * @param bytes Array de bytes.
     * @return Cadena hexadecimal.
     */
    public static String bytesToHex(byte[] bytes)
    {
        final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++)
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
