package cam72cam.universalmodcore;

import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Config {
    public final Mod mod;
    public final Integration integration;
    public final UMC umc;
    private final Map<String, String> vars = new HashMap<>();
    public final String minecraftLoader;

    public static class Mod {
        public final String pkg;
        public final String cls;
        public final String name;
        public final String id;
        public final String version;
        public final List<Dependency> dependencies;

        public static class Dependency {
            public final String id;
            public final String versions;
            public Dependency(String id, JsonObject data) {
                this.id = id;
                this.versions = data.get("versions").getAsString();
            }
            public Dependency(String id, String versions) {
                this.id = id;
                this.versions = versions;
            }
        }

        public Mod(JsonObject data) {
            this.pkg = data.get("pkg").getAsString();
            this.cls = data.get("cls").getAsString();
            this.name = data.get("name").getAsString();
            this.id = data.get("id").getAsString();
            this.version = data.get("version").getAsString();
            this.dependencies = data.get("dependencies").getAsJsonObject().entrySet().stream()
                    .map((e) -> new Dependency(e.getKey(), e.getValue().getAsJsonObject()))
                    .collect(Collectors.toList());
        }
    }

    public static class Integration {
        public final String repo;
        public final String branch;
        public final String path;

        public Integration(JsonObject data) {
            this.repo = data.get("repo").getAsString();
            this.branch = data.get("branch").getAsString();
            this.path = data.get("path").getAsString();
        }
    }

    public static class UMC {
        public final String version;
        public final String path;

        public UMC(JsonObject data) {
            this.version = data.get("version").getAsString();
            this.path = data.has("path") ? data.get("path").getAsString() : null;
        }
    }

    public Config(JsonObject data, String loaderBranch, boolean useSSH) throws GitAPIException, IOException {
        mod = new Mod(data.get("mod").getAsJsonObject());
        integration = data.has("integration") ? new Integration(data.get("integration").getAsJsonObject()) : null;
        umc = new UMC(data.get("umc").getAsJsonObject());
        String minecraft = loaderBranch.split("-")[0];
        String loader = loaderBranch.split("-")[1];
        this.minecraftLoader = minecraft + "-" + loader;

        vars.clear();
        vars.put("PACKAGE", require("mod.package", mod.pkg));
        vars.put("PACKAGEPATH", require("mod.package", mod.pkg).replace(".", File.separator));
        vars.put("CLASS", require("mod.class", mod.cls));
        vars.put("NAME", require("mod.name", mod.name));
        vars.put("ID", require("mod.id", mod.id));
        vars.put("VERSION", require("mod.version", mod.version));
        vars.put("LOADER_VERSION", loaderBranch);
        vars.put("MINECRAFT", minecraft);
        vars.put("LOADER", loader);

        String version = require("umc.version", umc.version);

        if (version.equals("latest")) {
            File path;
            File temp = null;
            if (umc.path == null) {
                temp = Files.createTempDirectory("umc-loader").toFile();
                Util.gitClone("https://github.com/TeamOpenIndustry/UniversalModCore.git", loaderBranch, temp, useSSH);
                path = temp;
            } else {
                path = Paths.get(System.getProperty("user.dir"), umc.path).toFile();
            }
            version = Files.readAllLines(Paths.get(path.getPath(), "build.gradle")).stream()
                    .filter(x -> x.startsWith("String umcVersion = "))
                    .findFirst()
                    .get()
                    .replace("String umcVersion = ", "")
                    .replace("\"", "")
                    .trim();
            version += "-" + Util.gitRevision(path);

            if (temp != null) {
                FileUtils.deleteDirectory(temp);
            }
        }

        String[] versionParts = version.split("\\.");
        int major = Integer.parseInt(versionParts[0]);
        int minor = Integer.parseInt(versionParts[1]);
        vars.put("UMC_API", String.format("%d.%d", major, minor));
        vars.put("UMC_API_NEXT", String.format("%d.%d", major, minor + 1));

        vars.put("UMC_VERSION", version);


        if (umc.path != null && umc.path.length() != 0) {
            File jar = new File(umc.path, String.format("build/libs/UniversalModCore-%s-%s.jar", loaderBranch, version));
            if (!jar.exists()) {
                throw new RuntimeException(String.format("Unable to find UMC jar: %s", jar));
            }
            vars.put("UMC_REPO", String.format("repositories { flatDir { dirs '%s' } }", jar.getParent()));
            vars.put("UMC_DEPENDENCY", String.format("name: '%s'", jar.getName().replace(".jar", "")));
            vars.put("UMC_FILE", jar.getPath());
        } else {
            vars.put("UMC_REPO", "repositories { maven { url = \"https://teamopenindustry.cc/maven\" }}");
            vars.put("UMC_DEPENDENCY", String.format("'cam72cam.universalmodcore:UniversalModCore:%s-%s'", loaderBranch, version));
            vars.put("UMC_DOWNLOAD", String.format("https://teamopenindustry.cc/maven/cam72cam/universalmodcore/UniversalModCore/%s-%s/UniversalModCore-%s-%s.jar", loaderBranch, version, loaderBranch, version));
        }

        ArrayList<Mod.Dependency> dependencies = new ArrayList<>(mod.dependencies);
        dependencies.add(new Mod.Dependency(
                "universalmodcore",
                String.format("[%s, %s)", vars.get("UMC_API"), vars.get("UMC_API_NEXT"))
        ));


        String forgeStringDependencies = dependencies.stream()
                .map(dep -> String.format("required-after:%s@%s", dep.id, dep.versions))
                .collect(Collectors.joining("; "));
        vars.put("FORGE_STRING_DEPENDENCIES", forgeStringDependencies);

        String forgeTomlDependencies = dependencies.stream()
                .map(dep ->
                        String.format("[[dependencies.%s]]%n", mod.id) +
                                String.format("    modId=\"%s\"%n", dep.id) +
                                String.format("    mandatory=true%n") +
                                String.format("    versionRange=\"%s\"%n", dep.versions) +
                                String.format("    ordering=\"BEFORE\"%n") +
                                String.format("    side=\"BOTH\"%n")
                )
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
        vars.put("FORGE_TOML_DEPENDENCIES", forgeTomlDependencies);

        vars.put("FABRIC_SOMETHING_DEPENDENCIES", "TODO");
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

    public String replace(String s) {
        return replace(s, true);
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
        data = replace(data);
        return new ByteArrayInputStream(data.getBytes());
    }
}
