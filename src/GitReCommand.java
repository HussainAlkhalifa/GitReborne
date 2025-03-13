import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitReCommand {

    private static Map<String, String> index = new HashMap<>();

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
        loadIndex();
        String father = "";
        File headFile = new File(".gitRE/HEAD");

        if (headFile.exists()){
            father = Files.readString(headFile.toPath()).trim();
        }

        StringBuilder treeContent = new StringBuilder();
        for (Map.Entry<String, String> entry : index.entrySet()) {
            treeContent.append(entry.getKey())
                    .append(" ")
                    .append(entry.getValue())
                    .append("\n");
        }

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1 = md.digest(treeContent.toString().getBytes());
        String treeHash = bytesToHex(sha1);

        File treeFile = new File(".git/trees/" + treeHash);
        treeFile.getParentFile().mkdirs();
        Files.writeString(treeFile.toPath(), treeContent.toString());

        String commitContent = "father: " + father + "\n" +
                "tree:" + treeHash + "\n" +
                "message" + message + "\n";

        byte[] commitSha1 = md.digest(commitContent.getBytes());
        String commitHash = bytesToHex(commitSha1);

        File commitDir = new File(".gitRE/commitsRE");
        commitDir.mkdirs();

        File commitFile = new File(commitDir, commitHash);
        Files.writeString(commitFile.toPath(), commitContent);

        Files.writeString(headFile.toPath(), commitHash);

        index.clear();
        saveIndex();

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

    public static void add(String filePath) throws IOException, NoSuchAlgorithmException{
        String hash = hashObject(filePath);
        index.put(filePath , hash);

        saveIndex();
        System.out.println("Added" + filePath + " to the staging area.");
    }

    public static void checkout(String commitHash) throws IOException{
        File commitFile = new File(".gitRE/commitsRE/" + commitHash);
        if (!commitFile.exists()){
            System.out.println("commit not found!");
            return;
        }

        String commitContent = Files.readString(commitFile.toPath());
        String treeHash = commitHash.split("\n")[1].split(": ")[1];

        File treeFile = new File(".gitRE/trees/" + treeHash);
        if (!treeFile.exists()){
            System.out.println("tree not found!");
            return;
        }

        List<String> treeContent = Files.readAllLines(treeFile.toPath());
        for (String line : treeContent){
            String[] parts = line.split(" ");
            String filePath = parts[0];
            String fileHash = parts[1];

            restoreFile(filePath , fileHash);
        }

        File headFile = new File(".gitRE/HEAD");
        Files.writeString(headFile.toPath() , commitHash);

        System.out.println("checked out to commit:" + commitHash);

    }

    private static void restoreFile(String filePath, String fileHash) throws IOException {
        File blobFile = new File(".gitRE/objectsRE/" + fileHash.substring(0, 2) + "/" + fileHash.substring(2));
        if (!blobFile.exists()) {
            System.out.println("Blob not found for file: " + filePath);
            return;
        }

        byte[] content = Files.readAllBytes(blobFile.toPath());

        Files.write(Paths.get(filePath), content);
        System.out.println("Restored file: " + filePath);
    }

    public static void saveIndex() throws IOException {

        StringBuilder indexContent = new StringBuilder();

        for (Map.Entry<String , String > entry : index.entrySet()){
            indexContent.append(entry.getKey())
                    .append(" ")
                    .append(entry.getValue())
                    .append("\n");
        }

        Files.writeString(new File(".gitRE/index").toPath()  , indexContent.toString());

    }

    private static void loadIndex() throws IOException{
        index.clear();

        File indexFile = new File(".gitRE/index");
        if (!indexFile.exists()){
            return;
        }

        for (String line : Files.readAllLines(indexFile.toPath())){
            String[] parts = line.split(" ");
            if (parts.length == 2){
                index.put(parts[0] , parts[1]);
            }
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
