package cam72cam.universalmodcore;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

public class Util {
    public static String GitRevision() throws IOException, GitAPIException {
        return Git.open(new File(System.getProperty("user.dir"))).log().setMaxCount(1).call().iterator().next().abbreviate(6).name();
    }

    public static void GitClone(String repository, String branch, String path) throws IOException, GitAPIException {
        File clonePath = new File(System.getProperty("user.dir"), path);



        String useSSH = System.getProperty("ssh.http");
        if (useSSH != null) {
            useSSH = useSSH.toLowerCase();
        }

        String uri = repository;

        boolean wantsHttp = "yes".equals(useSSH) || "true".equals(useSSH);
        boolean wantsGit = "no".equals(useSSH) || "false".equals(useSSH);
        boolean isHttp = uri.startsWith("http");
        boolean isGit = uri.startsWith("git@");

        if (isHttp && wantsGit) {
            uri = uri.replaceFirst("https://", "git@");
            uri = uri.replaceFirst("http://", "git@");
            uri = uri.replaceFirst("/", ":");
        }
        if (isGit && wantsHttp) {
            uri = uri.replaceFirst(":", "/");
            uri = uri.replaceFirst("git@", "https://");
        }

        if (clonePath.exists()) {
            System.out.println("Removing " + clonePath);
            FileUtils.deleteDirectory(clonePath);
        }

        clonePath.mkdirs();

        System.out.println("Cloning " + uri + " into " + clonePath);


        Git.cloneRepository()
                .setDirectory(clonePath)
                .setURI(uri)
                .setNoCheckout(true)
                .setCloneAllBranches(true)
                .call()
                .checkout()
                .setCreateBranch(true)
                .setName(branch)
                .setStartPoint("origin/" + branch)
                .call();
    }
}
