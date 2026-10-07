package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class PeerMessageTest {

    public static void main(String[] args) throws IOException 
    {
        String nombreArchivo = "peermsg.bin";
        // Borra el archivo antes de escribir para evitar residuos
        File f = new File(nombreArchivo);
        if (f.exists()) f.delete();

        DataOutputStream fos = new DataOutputStream(new FileOutputStream(nombreArchivo));

        // 1. Mensaje FILEREQUEST (descarga por hash)
        PeerMessage fileRequestOut = new PeerMessage(PeerMessageOps.OPCODE_FILEREQUEST, "0123456789abcdef0123");
        fileRequestOut.writeMessageToOutputStream(fos);

        // 2. Mensaje CHUNKREQUEST (solicitud de fragmento)
        PeerMessage chunkRequestOut = new PeerMessage(PeerMessageOps.OPCODE_CHUNKREQUEST, 12345L, 4096);
        chunkRequestOut.setReqFileHash("0123456789abcdef0123");
        chunkRequestOut.writeMessageToOutputStream(fos);

        // 3. Mensaje CHUNK (respuesta con datos)
        byte[] chunkData = new byte[4096];
        for (int i = 0; i < chunkData.length; i++) chunkData[i] = (byte) (i % 256);
        PeerMessage chunkOut = new PeerMessage(PeerMessageOps.OPCODE_CHUNK, 12345L, chunkData.length, chunkData);
        chunkOut.writeMessageToOutputStream(fos);

        // 4. Mensaje FILE_NOT_FOUND (control)
        PeerMessage notFoundOut = new PeerMessage(PeerMessageOps.OPCODE_FILE_NOT_FOUND);
        notFoundOut.writeMessageToOutputStream(fos);

        // 5. Mensaje FILENAME_TO_SAVE (nombre de fichero para subida)
        String fileName = "ejemplo.txt";
        PeerMessage fileNameToSaveOut = new PeerMessage(PeerMessageOps.OPCODE_FILENAME_TO_SAVE, fileName.length(), fileName);
        fileNameToSaveOut.writeMessageToOutputStream(fos);

        fos.close();

        // Ahora leemos y comprobamos los mensajes
        DataInputStream fis = new DataInputStream(new FileInputStream(nombreArchivo));

        try {
            // 1. FILEREQUEST
            PeerMessage fileRequestIn = PeerMessage.readMessageFromInputStream(fis);
            assert fileRequestIn.getOpcode() == PeerMessageOps.OPCODE_FILEREQUEST : "Opcode FILEREQUEST incorrecto";
            assert fileRequestIn.getReqFileHash().equals("0123456789abcdef0123") : "Hash FILEREQUEST incorrecto";

            // 2. CHUNKREQUEST
            PeerMessage chunkRequestIn = PeerMessage.readMessageFromInputStream(fis);
            assert chunkRequestIn.getOpcode() == PeerMessageOps.OPCODE_CHUNKREQUEST : "Opcode CHUNKREQUEST incorrecto";
            assert chunkRequestIn.getOffset() == 12345L : "Offset CHUNKREQUEST incorrecto";
            assert chunkRequestIn.getSize() == 4096 : "Size CHUNKREQUEST incorrecto";
            assert chunkRequestIn.getReqFileHash().equals("0123456789abcdef0123") : "Hash CHUNKREQUEST incorrecto";

            // 3. CHUNK
            PeerMessage chunkIn = PeerMessage.readMessageFromInputStream(fis);
            assert chunkIn.getOpcode() == PeerMessageOps.OPCODE_CHUNK : "Opcode CHUNK incorrecto";
            assert chunkIn.getOffset() == 12345L : "Offset CHUNK incorrecto";
            assert chunkIn.getSize() == 4096 : "Size CHUNK incorrecto";
            assert Arrays.equals(chunkIn.getChunkData(), chunkData) : "Datos CHUNK incorrectos";

            // 4. FILE_NOT_FOUND
            PeerMessage notFoundIn = PeerMessage.readMessageFromInputStream(fis);
            assert notFoundIn.getOpcode() == PeerMessageOps.OPCODE_FILE_NOT_FOUND : "Opcode FILE_NOT_FOUND incorrecto";

            // 5. FILENAME_TO_SAVE
            PeerMessage fileNameToSaveIn = PeerMessage.readMessageFromInputStream(fis);
            assert fileNameToSaveIn.getOpcode() == PeerMessageOps.OPCODE_FILENAME_TO_SAVE : "Opcode FILENAME_TO_SAVE incorrecto";
            assert fileNameToSaveIn.getSize() == fileName.length() : "Size FILENAME_TO_SAVE incorrecto";
            assert fileNameToSaveIn.getFileName().equals(fileName) : "Nombre de fichero FILENAME_TO_SAVE incorrecto";

            // Si intentamos leer más, saltará EOFException
            // (no hay bucle aquí, pero si accidentalmente se lee de más, no se imprimirá "opcode desconocido")
        } catch (EOFException eof) {
            // Fin del archivo alcanzado, no hacemos nada
        } finally {
            fis.close();
        }

        System.out.println("¡Todas las pruebas de PeerMessage pasaron correctamente!");
    }
}