package cam72cam.universalmodcore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Setup {
    public static void main(String[] args) throws IOException, GitAPIException {
        JsonObject configObj = JsonParser.parseReader(
                new InputStreamReader(new ByteArrayInputStream(
                        Files.readAllBytes(Paths.get("umc.json"))
                ))
        ).getAsJsonObject();

        if (args.length == 0) {
            System.err.println("No loader branch specified! Available branches can be found in the UniversalModCore GitHub repository.");
            return;
        }

        String loaderBranch = args[0];
        String[] split = loaderBranch.split("-");
        if (split.length < 2 || !split[0].matches("1\\.\\d*\\.\\d*") || !split[1].matches("[\\w-]+")) {
            System.err.println("Invalid loader branch! It should be in the format '<minecraft-version>-<loader>'. For example, '1.12.2-forge'.");
            return;
        }

        boolean useSSH = args.length >= 2 && args[1].equals("ssh");
        Config config = new Config(configObj, loaderBranch, useSSH);

        ZipInputStream zip = new ZipInputStream(config.openJarStream());
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            String path = entry.getName();
            if (path.startsWith("template") && !path.endsWith(File.separator)) {
                InputStream input = zip;
                if (!path.endsWith(".jar")) {
                    input = config.replaceAll(input, true);
                }

                if (path.endsWith("/")) {
                    continue;
                }

                if (path.endsWith("gradle-wrapper.jar")) {
                    // TODO removeme windows hack
                    continue;
                }

                path = path.replace("template/", "");

                Path out = Paths.get(System.getProperty("user.dir"), config.replace(path, false));
                System.out.println(out);

                out.getParent().toFile().mkdirs();

                out.toFile().delete();
                Files.copy(input, out);
                if (out.toString().equals("./gradlew")) {
                    out.toFile().setExecutable(true);
                }
            }
        }

        Path buildGradle = Paths.get(System.getProperty("user.dir"), "build.gradle");
        List<String> build = Files.readAllLines(buildGradle);
        Map<String, List<String>> parts = new HashMap<>();
        List<String> part = null;
        for (String line : build) {
            if (line.matches("//.*//")) {
                part = new ArrayList<>();
                parts.put(line, part);
            } else {
                // should NPE if bork'd build.gradle
                part.add(line);
            }
        }

        List<String> template = Files.readAllLines(Paths.get(System.getProperty("user.dir"), "template.gradle"));
        List<String> output = new ArrayList<>();
        for (String line : template) {
            if (parts.containsKey(line)) {
                output.addAll(parts.get(line));
                parts.remove(line);
            } else {
                output.add(line);
            }
        }
        Files.write(buildGradle, output.stream().map(config::replace).collect(Collectors.toList()));

        for (String key : parts.keySet()) {
            System.out.printf("WARNING: Missing template block %s!  Build is likely broken%n", key);
        }

        if (config.integration != null) {
            Util.gitClone(
                    config.integration.repo,
                    String.format(config.integration.branch, config.minecraftLoader),
                    Paths.get(System.getProperty("user.dir"), config.integration.path).toFile(),
                    useSSH
            );
        }
    }
}
