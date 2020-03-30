package cam72cam.setup;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Config {
    private final Map<String, String> vars;

    public Config(Path config, String loaderVersion) {
        try {
            this.vars = Files.readAllLines(config).stream()
                    .filter(l -> !l.trim().isEmpty())
                    .map(l -> {
                        if (!l.contains("=")) {
                            throw new RuntimeException("Invalid line: " + l);
                        }
                        return l.split("=", 2);
                    })
                    .collect(Collectors.toMap(l -> l[0].trim(), l -> l[1].trim()));

            require("PACKAGE");
            require("CLASS");
            require("NAME");
            require("ID");
            require("VERSION");

            vars.put("PACKAGEPATH", vars.get("PACKAGE").replace(".", File.separator));

            require("UMC_VERSION");
            String[] versionParts = vars.get("UMC_VERSION").split("\\.");
            int major = Integer.parseInt(versionParts[0]);
            int minor = Integer.parseInt(versionParts[1]);
            vars.put("UMC_API", String.format("%d.%d", major, minor));
            vars.put("UMC_API_NEXT", String.format("%d.%d", major, minor + 1));

            if (vars.containsKey("UMC_JAR")) {
                String jar = vars.get("UMC_JAR");
                if (!Files.exists(Paths.get(jar))) {
                    throw new RuntimeException(String.format("Unable to find UMC jar: %s", jar));
                }
                vars.put("UMC_DEPENDENCY", String.format("files ( \"%s\" )", jar));
            } else {
                //
                List<CurseForge.CurseAsset> assets = CurseForge.getAssets(277736);
                Optional<CurseForge.CurseAsset> asset = assets.stream()
                        .filter(a -> a.fileName.contains(vars.get("UMC_VERSION")))
                        .filter(a -> a.fileName.contains(loaderVersion))
                        .findFirst();
                if (!asset.isPresent()) {
                    throw new RuntimeException(String.format("Unable to locate version '%s' for Minecraft '%s'", vars.get("UMC_VERSION"), loaderVersion));
                }
                vars.put("UMC_DOWNLOAD", asset.get().downloadUrl);
                vars.put("UMC_DEPENDENCY", String.format("curse.maven:universalmodcore:%s", asset.get().id));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read config file " + config, e);
        }
    }

    public InputStream openJarStream() throws IOException {
        if (vars.containsKey("UMC_DOWNLOAD")) {
            return new URL(vars.get("UMC_DOWNLOAD")).openStream();
        }
        return new FileInputStream(vars.get("UMC_JAR"));
    }

    private void require(String s) {
        if (!vars.containsKey(s)) {
            throw new RuntimeException(String.format("Missing variable %s in config", s));
        }
    }

    public String replace(String s, boolean hash) {
        List<String> keys = new ArrayList<>(vars.keySet());
        keys.sort(Comparator.comparingInt(String::length).reversed());
        for (String var : keys) {
            String val = vars.get(var);
            if (hash) {
                var = String.format("#%s#", var);
            }
            s = s.replace(var, val);
        }
        return s;
    }

    public InputStream replaceAll(InputStream input, boolean hash) throws IOException {
        String data = IOUtils.toString(input);
        data = replace(data, true);
        return new ByteArrayInputStream(data.getBytes());
    }
}
