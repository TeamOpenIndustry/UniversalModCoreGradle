package cam72cam.universalmodcore;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class UMCPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("universalmodcore", Config.class, project);

        project.getTasks().create("umc", Setup.class);
    }
}
