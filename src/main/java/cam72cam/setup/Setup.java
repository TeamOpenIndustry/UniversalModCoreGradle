package cam72cam.setup;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Setup {
    public static void main(String[] args) {
        Config c = new Config(Paths.get(args[0]), "1.12.2");
        try (ZipInputStream zip = new ZipInputStream(c.openJarStream())) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                String path = entry.getName();
                if (path.startsWith("template") && !path.endsWith(File.separator)) {
                    InputStream input = zip;
                    if (!path.endsWith(".jar")) {
                        input = c.replaceAll(input, true);
                    }
                    Path out = Paths.get(".", c.replace(path.replace("template" + File.separator, ""), false));

                    System.out.println(out);
                    out.getParent().toFile().mkdirs();
                    out.toFile().delete();
                    Files.copy(input, out);
                    if (out.toString().equals("./gradlew")) {
                        out.toFile().setExecutable(true);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
