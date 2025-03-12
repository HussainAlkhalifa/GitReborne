import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class GitReCommand {

    public static void init() {
        File gitDir = new File(".gitRE");
        if (!gitDir.exists()){
            gitDir.mkdir();
            System.out.println("Empty git repository has been born");
        }else {
            System.out.println("Repository is already born");
        }
    }

    public static String hashObject(String filePath) throws IOException, NoSuchAlgorithmException {

        byte[] content = Files.readAllBytes(new File(filePath).toPath());

        String header = "blob" + content.length + "\0";

        byte [] store = concat(header.getBytes(), content);

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1 = md.digest(store);

        String hash = bytesToHex(sha1);
        System.out.println("Hash for the file: " + hash);

        File objectDir = new File(".gitRE/objectsRE");
        objectDir.mkdir();

        File file = new File(".gitRE/objectsRE/" + hash.substring(0, 2) + "/" + hash.substring(2));
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), store);

        System.out.println("File saved with hash:" + file.getAbsolutePath());
        return hash;

    }

    public static void commit(String blobHash, String message) throws Exception{
        String father = "";
        File headFile = new File(".gitRE/HEAD");

        if (headFile.exists()){
            father = Files.readString(headFile.toPath()).trim();
        }

        String commitContent = "father: " + father + "\n" +
                "blob:" + blobHash + "\n" +
                "message" + message + "\n";

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1 = md.digest(commitContent.getBytes());
        String commitHash = bytesToHex(sha1);

        File commitDir = new File(".gitRE/commitsRE");
        commitDir.mkdirs();

        File commitFile = new File(commitDir, commitHash);
        Files.writeString(commitFile.toPath(), commitContent);

        Files.writeString(headFile.toPath(), commitHash);

        System.out.println("Commit created:" + commitHash);
        System.out.println("Message:" + message);

    }

    public static void log() throws IOException{
        File headFile = new File(".gitRE/HEAD");

        if (!headFile.exists()){
            System.out.println("no commits found");
            return;
        }

        String currentCommitHash = Files.readString(headFile.toPath()).trim();

        while (!currentCommitHash.isEmpty()){
            File commitFile = new File(".gitRE/commitsRE", currentCommitHash);
            if (!commitFile.exists()){
                break;
            }

            String content = Files.readString(commitFile.toPath());
            System.out.println("Commit: " + currentCommitHash);
            System.out.println(content);
            System.out.println("------------------------------------");

            String[] lines = content.split("\n");
            String fatherLine = lines[0];
            currentCommitHash = fatherLine.substring(8).trim();
        }

    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] concat(byte[] a , byte[] b) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write( a );
        outputStream.write( b );
        return outputStream.toByteArray();
    }

}
