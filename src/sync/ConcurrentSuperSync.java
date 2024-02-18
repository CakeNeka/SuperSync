package sync;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ConcurrentSuperSync {

    private File syncedDir;
    private FTPClient ftpClient;
    private List<Thread> uploadingThreads = new ArrayList<>();

    public ConcurrentSuperSync(File syncedDir, String ftpServer, int ftpPort, String ftpUser, String ftpPassword) throws IOException {
        if (syncedDir == null || !syncedDir.exists() || !syncedDir.isDirectory()) {
            throw new IOException("Invalid directory file, could not sync");
        }
        this.syncedDir = syncedDir;
        fptConnect(ftpServer, ftpPort, ftpUser, ftpPassword);
    }

    private void fptConnect(String server, int port, String user, String password) throws IOException {
        ftpClient = new FTPClient();
        ftpClient.connect(server, port);
        int replyCode = ftpClient.getReplyCode();

        if (!FTPReply.isPositiveCompletion(replyCode)) {
            ftpClient.disconnect();
            throw new IOException("Unable to establish connection");
        }
        if (!ftpClient.login(user, password)) {
            throw new IOException("FTP login failed");
        }

        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    }

    /**
     * Se ejecuta cada x segundos,
     * compara carpetas local y remota
     * Thread.sleep() no es lo más óptimo, pero he encontrado problemas
     * utilizando ExecutorService que no surgen con Thread.sleep().
     */
    public void startSync(int interval) {
        while (true) {
            try {
                mainLoop();
                Thread.sleep(interval * 1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    List<String> localFiles = new ArrayList<>();

    private void mainLoop() {
        localFiles.clear();
        analyzeLocalDir(syncedDir);
        cleanRemoteDir("/");
    }

    /**
     * Recorre archivos en carpeta local y los compara con las de la carpeta remota,
     * pueden darse tres casos:
     * 1. La carpeta/fichero ya existe en el servidor FTP y no ha sido modificada.
     * En este caso: no hacemos nada
     * 2. La carpeta/fichero ya existe en el servidor FTP y ha sido modificada.
     * En este caso: Subir la carpeta actualizada
     * 3. La carpeta no existe en el servidor FTP
     * En este caso: subir la nueva carpeta
     */
    private void analyzeLocalDir(File dir) {
        File[] children = dir.listFiles();
        for (File child : children) {
            localFiles.add(toFtpPath(child));
            if (child.isDirectory()) {
                analyzeLocalDir(child); // Recorrer recursivamente estructura de ficheros
            } else {
                try {
                    if (!existsOnFtp(child)) // si no existe en el servidor, lo sube
                        upload(child);
                } catch (IOException e) {
                    System.err.println("Unable to upload " + child);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Recorre los archivos en el servidor y los elimina si no tienen
     * correspondencia en la carpeta local sincronizada.
     * <p>
     * La lista localFiles es rellenada en el método analyzeLocalDir(File dir)
     * que recorre los archivos locales.
     */
    private void cleanRemoteDir(String parent) {
        try {
            ftpClient.changeWorkingDirectory(parent);
            FTPFile[] ftpFiles = ftpClient.listFiles();
            if (parent.equals("/")) {
                parent = "";
            }
            for (FTPFile ftpFile : ftpFiles) {
                String ftpFilePath = parent + "/" + ftpFile.getName();
                if (ftpFile.isDirectory()) {
                    ftpFilePath += "/";
                }
                if (!localFiles.contains(ftpFilePath)) {
                    ftpClient.changeWorkingDirectory("/");
                    if (ftpClient.deleteFile(ftpFilePath)) {
                        System.out.println("Remote file " + ftpFilePath + " deleted");
                    }
                } else if (ftpFile.isDirectory()) {
                    cleanRemoteDir(parent + "/" + ftpFile.getName());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Devuelve true si el archivo local existe en el servidor
     * Para que esto funcione antes he tenido que establecer la
     * fecha de última modificación en el archivo del servidor
     * al subir el archivo (en el método upload())
     */
    private boolean existsOnFtp(File file) throws IOException {
        String remotePath = toFtpPath(file);
        ftpClient.changeWorkingDirectory("/");
        FTPFile[] ftpFiles = ftpClient.listFiles(remotePath);
        if (ftpFiles.length == 0)
            return false;
        assert ftpFiles.length == 1;

        String localLastModified = timeStampToString(file.lastModified());
        String serverLastModified = ftpClient.getModificationTime(remotePath).substring(0, 14);
        return localLastModified.equals(serverLastModified);
    }


    private static void debugTimestamps(File file, FTPFile[] ftpFiles, String serverLastModified, String localLastModified) {
        long remoteTimestamp = ftpFiles[0].getTimestamp().getTimeInMillis(); // ftpFile.getTimestamp().getTimeInMillis();
        System.out.println("Remote timestamp: " + remoteTimestamp);
        System.out.println("Local timestamp: " + file.lastModified());
        System.out.println("remote formatted date: " + serverLastModified);
        System.out.println("local formatted date: " + localLastModified);
    }

    private String timeStampToString(long timestamp) {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(timestamp));
    }


    private void upload(File localFile) throws IOException {
        System.out.println("Uploading " + localFile);
        String ftpPath = toFtpPath(localFile);

        // Crear directorios padres en el servidor si es necesario
        String ftpPathParents = ftpPath.substring(0, ftpPath.lastIndexOf('/'));
        ftpCreateDirectoryTree(ftpPathParents);

        ftpClient.changeWorkingDirectory("/");

        InputStream is = new FileInputStream(localFile);
        ftpClient.storeFile(ftpPath, is);
        is.close();

        // Establecer la misma fecha de modificación que en el archivo local
        String ftpDate = timeStampToString(localFile.lastModified());
        ftpClient.setModificationTime(ftpPath, ftpDate);
    }

    /**
     * Pasar de la ruta local a la ruta en ftp, por ejemplo
     * <p>
     * Ruta de carpeta sincronizada local -> ficheros/pruebas/syncedfolder
     * Ruta de archivo local ->              ficheros/pruebas/syncedfolder/bullets/romance.mp3
     * Salida ->                             /bullets/romance.mp3
     * <a href="https://stackoverflow.com/questions/204784/how-to-construct-a-relative-path-in-java-from-two-absolute-paths-or-urls">Fuente</a>
     */
    private String toFtpPath(File localFile) {
        return "/" + syncedDir
                .toURI()
                .relativize(localFile.toURI())
                .getPath()
                .replace('\\', '/');
    }

    /**
     * Crea una jerarquía de directorios en el servidor ftp
     * <a href="https://stackoverflow.com/questions/4078642/create-a-folder-hierarchy-through-ftp-in-java">Fuente</a>
     *
     * @param dirTree Directorios separados por '/', sin nombre de archivo
     */
    private void ftpCreateDirectoryTree(String dirTree) throws IOException {
        boolean dirExists = true;
        //tokenize the string and attempt to change into each directory level.  If you cannot, then start creating.
        String[] directories = dirTree.split("/");
        for (String dir : directories) {
            if (!dir.isEmpty()) {
                if (dirExists) {
                    dirExists = ftpClient.changeWorkingDirectory(dir);
                }
                if (!dirExists) {
                    if (!ftpClient.makeDirectory(dir)) {
                        throw new IOException("Unable to create remote directory '" + dir + "'.  error='" + ftpClient.getReplyString() + "'");
                    }
                    if (!ftpClient.changeWorkingDirectory(dir)) {
                        throw new IOException("Unable to change into newly created remote directory '" + dir + "'.  error='" + ftpClient.getReplyString() + "'");
                    }
                }
            }
        }
    }


}