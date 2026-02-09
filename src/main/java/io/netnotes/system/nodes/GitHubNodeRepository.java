package io.netnotes.system.nodes;

import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.engine.utils.github.GitHubAPI;
import io.netnotes.engine.utils.github.GitHubInfo;

public class GitHubNodeRepository extends Repository {
    private final GitHubInfo githubInfo;
    private final String packagePath;
    private final String keyPath;

    public GitHubNodeRepository(String id, String name, GitHubInfo gitHubInfo, String packagePath, String keyPath, boolean enabled) {
        this(new NoteBytesReadOnly(id), name, gitHubInfo, packagePath, keyPath, enabled);
    }

    public GitHubNodeRepository(NoteBytesReadOnly id, String name, GitHubInfo gitHubInfo, String packagePath, String keyPath, boolean enabled) {
        super(id, name,
            GitHubAPI.GITHUB_USER_CONTENT + "/" + gitHubInfo.getUser() + "/" +gitHubInfo.getProject() 
                + "/" + gitHubInfo.getBranch() + "/" + packagePath,
            GitHubAPI.GITHUB_USER_CONTENT + "/" + gitHubInfo.getUser() + "/" + gitHubInfo.getProject() 
                + "/" + gitHubInfo.getBranch() + "/" + keyPath,
            enabled
        );
        this.githubInfo = gitHubInfo;
        this.packagePath = packagePath;
        this.keyPath = keyPath;
    }

    public GitHubInfo getGithubInfo() {
        return githubInfo;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public String getKeyPath() {
        return keyPath;
    }
    
    
}
