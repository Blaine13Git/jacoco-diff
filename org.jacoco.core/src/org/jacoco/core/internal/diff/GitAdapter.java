package org.jacoco.core.internal.diff;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Git操作类
 */
public class GitAdapter {
    private Git git;
    private Repository repository;
    private String gitFilePath;

    //  Git授权
    public static final String USERNAME = "qa-jenkins"; //qa-jenkins
    public static final String PASSWORD = "*6OGZD9hY5Ylkk$d!Mjv"; //*6OGZD9hY5Ylkk$d!Mjv
    public static final UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider = new UsernamePasswordCredentialsProvider(USERNAME, PASSWORD);

    public GitAdapter(String gitFilePath) {
        this.gitFilePath = gitFilePath;
        this.initGit(gitFilePath);
    }

    private void initGit(String gitFilePath) {
        try {
            git = Git.open(new File(gitFilePath));
            repository = git.getRepository();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getGitFilePath() {
        return gitFilePath;
    }

    public Git getGit() {
        return git;
    }

    public Repository getRepository() {
        return repository;
    }

    /**
     * git授权。需要设置拥有所有权限的用户
     *
     * @param username git用户名
     * @param password git用户密码
     */
    public static void setCredentialsProvider(String username, String password) {
        if (usernamePasswordCredentialsProvider == null) {
            System.out.println("No");
        }
    }

    /**
     * 获取指定分支的指定文件内容
     *
     * @param branchName 分支名称
     * @param javaPath   文件路径
     * @return java类
     * @throws IOException
     */
    public String getBranchSpecificFileContent(String branchName, String javaPath) throws IOException {
        Ref branch = repository.exactRef("refs/heads/" + branchName);
        ObjectId objId = branch.getObjectId();
        RevWalk walk = new RevWalk(repository);
        RevTree tree = walk.parseTree(objId);
        TreeWalk treeWalk = TreeWalk.forPath(repository, javaPath, tree);
        ObjectId blobId = treeWalk.getObjectId(0);
        ObjectLoader loader = repository.open(blobId);
        byte[] bytes = loader.getBytes();
        walk.dispose();
        return new String(bytes);
    }

    /**
     * 分析分支树结构信息
     *
     * @param localRef 本地分支
     * @return
     * @throws IOException
     */
    public AbstractTreeIterator prepareTreeParser(Ref localRef) throws IOException {
        RevWalk walk = new RevWalk(repository);
        RevCommit commit = walk.parseCommit(localRef.getObjectId());
        RevTree tree = walk.parseTree(commit.getTree().getId());
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        ObjectReader reader = repository.newObjectReader();
        treeParser.reset(reader, tree.getId());
        walk.dispose();
        return treeParser;
    }

    /**
     * 切换分支
     *
     * @param branchName 分支名称
     * @throws GitAPIException
     */
    public void checkOut(String branchName) throws GitAPIException {
        //  切换分支
        git.checkout()
                .setCreateBranch(false)
                .setName(branchName)
                .call();
    }

    /**
     * TODO gitHub无法更新
     * 更新分支代码
     *
     * @param localRef   本地分支
     * @param branchName 分支名称
     * @throws GitAPIException
     */
    public void checkOutAndPull(Ref localRef, String branchName) throws GitAPIException {
        boolean isCreateBranch = localRef == null;

        if (!isCreateBranch && checkBranchNewVersion(localRef)) {
            return;
        }

        //  切换分支
        git.checkout()
                .setCreateBranch(isCreateBranch)
                .setName(branchName)
                .setStartPoint("origin/" + branchName)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .call();

        //  拉取最新代码
        git.pull()
                .setCredentialsProvider(usernamePasswordCredentialsProvider)
                .call();
    }

    /**
     * 判断本地分支是否是最新版本。目前不考虑分支在远程仓库不存在，本地存在
     *
     * @param localRef 本地分支
     * @return boolean
     * @throws GitAPIException
     */
    private boolean checkBranchNewVersion(Ref localRef) throws GitAPIException {
        String localRefName = localRef.getName();
        String localRefObjectId = localRef.getObjectId().getName();

        //  获取远程所有分支
        Collection<Ref> remoteRefs = git.lsRemote()
                .setCredentialsProvider(usernamePasswordCredentialsProvider)
                .setHeads(true)
                .call();

        for (Ref remoteRef : remoteRefs) {
            String remoteRefName = remoteRef.getName();
            String remoteRefObjectId = remoteRef.getObjectId().getName();
            if (remoteRefName.equals(localRefName)) {
                if (remoteRefObjectId.equals(localRefObjectId)) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }


    public static void main(String[] args) throws Exception {
        String oldBranchName = "master";
        GitAdapter gitAdapter = new GitAdapter("/Users/changfeng/work/jacoco/codes/live/.git");

        Ref localMasterRef = gitAdapter.getRepository().exactRef("refs/heads/" + oldBranchName);
        gitAdapter.checkOutAndPull(localMasterRef,oldBranchName);
    }
}
