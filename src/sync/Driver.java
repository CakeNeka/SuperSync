package sync;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Driver {
    private static final String SERVER = "localhost";
    private static final int PORT = 21;
    private static final String USER = "miguel";
    private static final String PASSWORD = "1234";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("folder to sync: ");
        String syncedDirPath = scanner.nextLine();
        File syncedDir = new File(syncedDirPath);

        try {
            SuperSync sync = new SuperSync(syncedDir, SERVER, PORT, USER, PASSWORD);
            sync.startSync(2);
            System.out.println("Directory successfully synchronized:)");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
