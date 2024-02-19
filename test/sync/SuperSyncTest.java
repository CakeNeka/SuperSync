package sync;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SuperSyncTest {
    private static final String SERVER = "localhost";
    private static final int PORT = 21;
    private static final String USER = "miguel";
    private static final String PASSWORD = "1234";
    private static final int INTERVAL = 1;
    private SuperSync sync;

    @BeforeEach
    public void setUp() throws IOException {
        sync = new SuperSync(new File("testdir"), SERVER, PORT, USER, PASSWORD);
        sync.startSync(INTERVAL);
    }

    @AfterEach
    public void end() {
        sync.stopSync();
    }

    @Test
    @DisplayName("Prueba de estrés")
    public void test() throws IOException, InterruptedException {
        String basePath = "testdir/stress/";
        File dir = new File(basePath);
        if (dir.exists()) {
            deleteDir(dir); // Elimina directorio y sus contenidos
        }
        dir.mkdir();

        // Crea archivos en la carpeta establecida
        for (int i = 0; i < 300; i++) {
            String fileName = basePath + "file_" + i;
            File file = new File(fileName);
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write("Este el maravilloso archivo de prueba " + i + " 🔥🔥🔥🔥");
        }

        // Nos aseguramos de que al sistema le dé tiempo a subir los archivos
        Thread.sleep(Duration.ofSeconds(INTERVAL * 2));
    }

    @Test
    @DisplayName("Subir fichero binario")
    public void binaryTest() throws IOException, InterruptedException {
        Path originalFile = Path.of("media/super_sync.gif");
        Path syncedFile = Path.of("testdir/super_sync_ftp.gif");
        Files.copy(originalFile, syncedFile, StandardCopyOption.REPLACE_EXISTING);
        Thread.sleep(Duration.ofSeconds(INTERVAL * 2));
    }

    private void deleteDir(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory())
                deleteDir(file);
            else
                file.delete();
        }
        dir.delete();
    }
}