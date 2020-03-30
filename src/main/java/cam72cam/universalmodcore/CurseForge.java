package cam72cam.universalmodcore;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class CurseForge {
    private CurseForge() {}

    public static List<CurseAsset> getAssets(int projectId) {
        try {
            String data = IOUtils.toString(new URL(String.format("https://addons-ecs.forgesvc.net/api/v2/addon/%d/files", projectId)).openStream());
            CurseAsset[] assets = (new Gson()).fromJson(data, CurseAsset[].class);
            return Arrays.asList(assets);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //@JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurseAsset {
        public String id;
        public String fileName;
        public int releaseType;
        public int fileStatus;
        public String downloadUrl;
        public boolean isAvailable;
        public List<String> gameVersions;
    }
}
