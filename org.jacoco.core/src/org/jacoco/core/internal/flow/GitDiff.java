package org.jacoco.core.internal.flow;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class GitDiff {
    private Logger log = LoggerFactory.getLogger(GitDiff.class);
    private static Git git;
    private static Repository repository;
    private static final String PREFIX = "refs/heads/";

    public GitDiff() {
        // 获取git对象--单例DCL模式
        if (null == repository) {
            synchronized (GitDiff.class) {
                if (null == repository) {
                    try {

                        // 方式1
                        FileRepositoryBuilder builder = new FileRepositoryBuilder();
                        repository = builder.readEnvironment().findGitDir().build();

                        // 方式2
//                        String projectPath = System.getProperty("user.dir");
//                        repository = builder.setGitDir(new File(projectPath + "/.git")).build();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // 获取repository对象--单例DCL模式
        if (null == git) {
            synchronized (GitDiff.class) {
                if (null == git) {
                    git = new Git(repository);
                }
            }
        }

    }

    /**
     * @param repository 代码仓库
     * @param branchName 例如"refs/heads/master"
     * @return
     * @throws IOException
     */
    private static AbstractTreeIterator prepareTreeParserBranch(Repository repository, String branchName) throws IOException {
        Ref ref = repository.exactRef(branchName);
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(ref.getObjectId());
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
     * @param baseBranch
     * @param diffBranch
     * @return Diff DiffEntry list
     */
    public List<DiffEntry> getDiffEntriesByBranch(String baseBranch, String diffBranch) {
        List<DiffEntry> diffs = null;
        try {

            AbstractTreeIterator baseBranchTree = prepareTreeParserBranch(repository, PREFIX + baseBranch);
            AbstractTreeIterator diffBranchTree = prepareTreeParserBranch(repository, PREFIX + diffBranch);

            diffs = git.diff()
                    .setOldTree(baseBranchTree)
                    .setNewTree(diffBranchTree)
                    .setPathFilter(PathSuffixFilter.create(".java"))
                    .call();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return diffs;
    }

    /**
     * 获取修改的内容
     *
     * @param baseBranch
     * @param diffBranch
     * @return 返回修改的list
     */
    public List<DiffEntry> getModify(String baseBranch, String diffBranch) {
        List<DiffEntry> diffEntries = getDiffEntriesByBranch(baseBranch, diffBranch);
        List<DiffEntry> modifyList = diffEntries.stream().filter(diffEntry -> diffEntry.getChangeType().toString().equals("MODIFY")).collect(Collectors.toList());
        return modifyList;
    }

    /**
     * 获取新增内容
     *
     * @param baseBranch
     * @param diffBranch
     * @return 返回新增的list
     */
    public List<DiffEntry> getAdd(String baseBranch, String diffBranch) {
        List<DiffEntry> diffEntries = getDiffEntriesByBranch(baseBranch, diffBranch);
        List<DiffEntry> addList = diffEntries.stream().filter(diffEntry -> diffEntry.getChangeType().toString().equals("ADD")).collect(Collectors.toList());
        return addList;
    }

    /**
     * 获取删除的内容
     *
     * @param baseBranch
     * @param diffBranch
     * @return 返回删除的list
     */
    public List<DiffEntry> getDelete(String baseBranch, String diffBranch) {
        List<DiffEntry> diffEntries = getDiffEntriesByBranch(baseBranch, diffBranch);
        List<DiffEntry> deleteList = diffEntries.stream().filter(diffEntry -> diffEntry.getChangeType().toString().equals("DELETE")).collect(Collectors.toList());
        return deleteList;
    }

    /**
     * 获取非删除的内容（即modify & add）
     *
     * @param baseBranch
     * @param diffBranch
     * @return 返回删除的list
     */
    public List<DiffEntry> getNotDelete(String baseBranch, String diffBranch) {
        List<DiffEntry> diffEntries = getDiffEntriesByBranch(baseBranch, diffBranch);
        List<DiffEntry> notDeleteList = diffEntries.stream().filter(diffEntry -> !(diffEntry.getChangeType().toString().equals("DELETE"))).collect(Collectors.toList());
        return notDeleteList;
    }

    /**
     * 判断是否为diff文件
     *
     * @param classname
     * @return <code>true</code> 表示是diff文件
     */
    public boolean isDiff(String classname, String baseBranch, String diffBranch) {
        List<DiffEntry> notDeleteDiffEntries = getNotDelete(baseBranch, diffBranch);
        for (DiffEntry diffEntry : notDeleteDiffEntries) {
            if (diffEntry.getNewPath().contains(classname)) {
                return true;
            }
        }
        return false;
    }


    public static void main(String[] args) {

//        GitDiff gitDiff = new GitDiff();
//        List<DiffEntry> diffs = gitDiff.getDiffEntriesByBranch("master", "home-test");
//
//        List<DiffEntry> modify = gitDiff.getModify("master", "home-test");
//        List<DiffEntry> add = gitDiff.getAdd("master", "home-test");
//        List<DiffEntry> delete = gitDiff.getDelete("master", "home-test");
//        List<DiffEntry> delete = gitDiff.getNotDelete("master", "home-test");

    }

}

