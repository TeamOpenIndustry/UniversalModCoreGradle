package cam72cam.universalmodcore;

import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Config {
    public String modPackage;
    public String modClass;
    public String modName;
    public String modId;
    public String modVersion;

    public String umcVersion;
    public String umcPath;
    public String umcJar;

    public Config(Project project) {
        // What to do here?
    }

    private final Map<String, String> vars = new HashMap<>();

    public void init() {
        vars.clear();
        vars.put("PACKAGE", require("modPackage", modPackage));
        vars.put("PACKAGEPATH", require("modPackage", modPackage).replace(".", File.separator));
        vars.put("CLASS", require("modClass", modClass));
        vars.put("NAME", require("modName", modName));
        vars.put("ID", require("modId", modId));
        vars.put("VERSION", require("modVersion", modVersion));

        String loaderVersion = System.getProperty("umc.loader");
        if (loaderVersion == null || loaderVersion.length() == 0) {
            throw new RuntimeException("Missing command argument: -D umc.loader=<loader>_<version>");
        }

        vars.put("LOADER_VERSION", require("loaderVersion", loaderVersion));

        String version = require("umcVersion", umcVersion);
        vars.put("UMC_VERSION", version);
        String[] versionParts = version.split("\\.");
        int major = Integer.parseInt(versionParts[0]);
        int minor = Integer.parseInt(versionParts[1]);
        vars.put("UMC_API", String.format("%d.%d", major, minor));
        vars.put("UMC_API_NEXT", String.format("%d.%d", major, minor + 1));


        if (umcJar != null && umcJar.length() != 0) {
            if (!Files.exists(Paths.get(umcJar))) {
                throw new RuntimeException(String.format("Unable to find UMC jar: %s", umcJar));
            }
            vars.put("UMC_DEPENDENCY", String.format("files ( \"%s\" )", umcJar));
        } else if (umcPath != null && umcPath.length() != 0) {
            File jar = new File(umcPath, String.format("UniversalModCore-%s-%s.jar", umcVersion, loaderVersion));
            if (!jar.exists()) {
                throw new RuntimeException(String.format("Unable to find UMC jar: %s", jar));
            }
            umcJar = jar.toString();
            vars.put("UMC_DEPENDENCY", String.format("files ( \"%s\" )", umcJar));
        } else {
            List<CurseForge.CurseAsset> assets = CurseForge.getAssets(371784);
            Optional<CurseForge.CurseAsset> asset = assets.stream()
                    .filter(a -> a.fileName.contains(version))
                    .filter(a -> a.fileName.contains(require("loaderVersion", loaderVersion)))
                    .findFirst();
            if (!asset.isPresent()) {
                throw new RuntimeException(String.format("Unable to locate version '%s' for Minecraft '%s'", vars.get("UMC_VERSION"), loaderVersion));
            }
            vars.put("UMC_DOWNLOAD", asset.get().downloadUrl);
            vars.put("UMC_DEPENDENCY", String.format("'curse.maven:universalmodcore:%s'", asset.get().id));
        }
    }


    private String require(String name, String s) {
        if (s == null || s.trim().length() == 0) {
            throw new RuntimeException(String.format("Missing variable %s in config: %s", name, s));
        }
        return s;
    }

    public InputStream openJarStream() throws IOException {
        if (vars.containsKey("UMC_DOWNLOAD")) {
            return new URL(vars.get("UMC_DOWNLOAD")).openStream();
        }
        return new FileInputStream(umcJar);
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
