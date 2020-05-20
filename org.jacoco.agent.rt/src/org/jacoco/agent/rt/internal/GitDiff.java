package org.jacoco.agent.rt.internal;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GitDiff {
    private static Git git;
    private static Repository repository;

    // 获取repository对象--单例DCL模式
    public static Repository getRepository() {
        if (null == repository) {
            synchronized (GitDiff.class) {
                if (null == repository) {
                    try {
                        String projectPath = System.getProperty("user.dir");
                        repository = new FileRepositoryBuilder()
                                .setGitDir(new File(projectPath + "/.git"))
                                .build();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return repository;
    }

    // 获取git对象--单例DCL模式
    public static Git getGit(Repository repository) {
        if (null == git) {
            synchronized (GitDiff.class) {
                if (null == git) {
                    git = new Git(getRepository());
                }
            }
        }
        return git;
    }

    /**
     * @param repository 代码仓库
     * @param branchName 例如"refs/heads/master"
     * @return
     * @throws IOException
     */
    private static AbstractTreeIterator prepareTreeParserBranch(
            Repository repository, String branchName) throws IOException {

        ObjectId objectId = repository.exactRef(branchName).getObjectId();

        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            walk.dispose();
            return treeParser;
        }
    }

    /**
     * diff by branch
     *
     * @param repository
     * @param baseBranch
     * @param diffBranch
     * @throws GitAPIException
     * @throws IOException
     */
    public static List<DiffEntry> getDiffByBranch(Repository repository,
                                                  String baseBranch, String diffBranch) {
        List<DiffEntry> diffs = null;
        AbstractTreeIterator baseBranchTree;
        AbstractTreeIterator diffBranchTree;
        try {
            baseBranchTree = prepareTreeParserBranch(repository, baseBranch);
            diffBranchTree = prepareTreeParserBranch(repository, diffBranch);

            diffs = getGit(getRepository()).diff().setOldTree(baseBranchTree)
                    .setNewTree(diffBranchTree)
                    .setPathFilter(PathSuffixFilter.create(".java")).call();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return diffs;
    }
}
