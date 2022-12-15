package cam72cam.universalmodcore;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;

public class Util {
    public static String gitRevision(File path) throws IOException, GitAPIException {
        Git repo = Git.open(path);
        RevCommit commit = repo.log().setMaxCount(1).call().iterator().next();
        String name = repo.getRepository().newObjectReader().abbreviate(commit, 7).name();
        repo.close();
        return name;
    }

    public static void gitClone(String repository, String branch, File clonePath, Boolean useSSH) throws IOException, GitAPIException {
        String uri = repository;

        boolean wantsHttp = useSSH != null && !useSSH;
        boolean wantsGit = useSSH != null && useSSH;
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


        try (Git repo = Git.cloneRepository()
                .setDirectory(clonePath)
                .setURI(uri)
                .setNoCheckout(true)
                .setCloneAllBranches(true)
                .call()) {
            repo.checkout()
                    .setCreateBranch(true)
                    .setName(branch)
                    .setStartPoint("origin/" + branch)
                    .call();
        }
    }
}
