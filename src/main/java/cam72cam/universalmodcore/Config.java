package cam72cam.universalmodcore;

import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;

import java.io.*;
import java.net.URL;
import java.util.*;

public class Config {
    public String modPackage;
    public String modClass;
    public String modName;
    public String modId;
    public String modVersion;

    public String umcVersion;
    public String umcPath;

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


        if (umcPath != null && umcPath.length() != 0) {
            File jar = new File(umcPath, String.format("UniversalModCore-%s-%s.jar", loaderVersion, umcVersion));
            if (!jar.exists()) {
                throw new RuntimeException(String.format("Unable to find UMC jar: %s", jar));
            }
            vars.put("UMC_REPO", String.format("repositories { flatDir { dirs '%s' } }", jar.getParent()));
            vars.put("UMC_DEPENDENCY", String.format("name: '%s'", jar.getName().replace(".jar", "")));
            vars.put("UMC_FILE", jar.getPath());
        } else {
            vars.put("UMC_DOWNLOAD", String.format("https://teamopenindustry.cc/maven/cam72cam/universalmodcore/UniversalModCore/%s-%s/UniversalModCore-%s-%s.jar", loaderVersion, version, loaderVersion, version));
            vars.put("UMC_DEPENDENCY", String.format("'cam72cam.universalmodcore:UniversalModCore:%s-%s'", loaderVersion, version));
            vars.put("UMC_REPO", "repositories { maven { url = \"https://teamopenindustry.cc/maven\" }}");

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
        return new FileInputStream(vars.get("UMC_FILE"));
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
