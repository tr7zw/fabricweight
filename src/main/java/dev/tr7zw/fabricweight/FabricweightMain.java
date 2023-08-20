package dev.tr7zw.fabricweight;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dev.tr7zw.fabricweight.util.FileUtil;
import dev.tr7zw.fabricweight.util.GitUtil;

public class FabricweightMain {

    private static List<String> fabricweightGitignore = Arrays.asList(".gradle/", "upstream/", "tmp/", "workspace/",
            "scripts/", "patcher", ".project", "/bin/");

    public static void main(String[] args) throws IOException {
        if (!GitUtil.gitAvailable()) {
            System.out.println("Git not found!");
            System.exit(1);
        }
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }
        if ("patch".equals(args[0])) {
            setupWorkspace(new File("."));
        } else if ("rb".equals(args[0])) {
            rebuildPatches(new File("workspace"), new File("patches"));
        } else if ("rebase".equals(args[0])) {
            int amount = 1;
            if (args.length >= 2) {
                amount = Integer.parseInt(args[1]);
            }
            GitUtil.runGitCommand(new File("workspace"), new String[] { "git", "rebase", "-i", "HEAD~" + amount });
        } else if ("commit".equals(args[0])) {
            GitUtil.runGitCommand(new File("workspace"), new String[] { "git", "add", "*" });
            GitUtil.runGitCommand(new File("workspace"), new String[] { "git", "rebase", "--continue" });
        } else if ("convert".equals(args[0])) {
            if (args.length < 4) {
                printUsage();
                System.exit(1);
            }
            convertBranch(args[1], args[2], args[3]);
        } else {
            printUsage();
            System.exit(1);
        }
    }

    public static void printUsage() {
        System.err.println("Arguments:");
        System.err.println(" - 'patch' to create a workspace from upstream with applied patched");
        System.err.println(" - 'rb' to rebuild the patches based on the workspace");
        System.err.println(" - 'rebase <n>' to modify the last n(by default 1) commits");
        System.err.println(" - 'commit' complete the current rebase by adding all changes to the commit");
        System.err.println(" - 'convert <new branch name> <upstream> <sha>' convert the current branch into a fork");
    }

    public static void convertBranch(String branchName, String upstream, String sha) throws IOException {
        // switch to a new orphan branch
        GitUtil.runGitCommand(new File("."), new String[] { "git", "checkout", "--orphan", branchName });
        // move all current data into a placeholder folder
        File tmp = new File("tmp");
        tmp.mkdirs();
        FileUtil.moveFiles(new File("."), tmp, file -> {
            return file.getName().equals("tmp") || file.getName().toLowerCase().startsWith("fabricweight")
                    || file.getName().equals(".git");
        });
        // setup fabricweight
        Files.write(new File("repo").toPath(), upstream.getBytes());
        Files.write(new File("sha").toPath(), sha.getBytes());
        new File("patches").mkdirs();
        Files.write(new File(".gitignore").toPath(), String.join("\n", fabricweightGitignore).getBytes());
        // pull upstream
        setupWorkspace(new File("."));
        // copy in the current branch into the workspace
        File workspace = new File("workspace");
        FileUtil.delete(workspace, file -> {
            return file.getName().equals("workspace") || (file.getAbsolutePath().contains(".git")
                    && !file.getAbsolutePath().contains(".github") && !file.getAbsolutePath().contains(".gitignore"));
        });
        FileUtil.moveFiles(tmp, workspace, file -> false);
        // create commit
        GitUtil.runGitCommand(new File("workspace"), new String[] { "git", "add", "*" });
        GitUtil.runGitCommand(new File("workspace"), new String[] { "git", "commit" });
        rebuildPatches(new File("workspace"), new File("patches"));
        FileUtil.deleteEmptyDirectories(new File("."), file -> file.getName().equals("patches"));
        System.out.println("Conversion done.");
    }

    public static File setupWorkspace(File folder) throws IOException {
        checkoutUpstream(folder);
        File upstream = new File(folder, "upstream");
        File outDir;
        if (new File(upstream, "repo").exists()) { // upstream is a fork too
            outDir = setupWorkspace(upstream);
        } else {
            outDir = upstream;
        }
        File workspace = new File(folder, "workspace");
        createWorkspace(workspace, outDir);
        File patchDir = new File(folder, "patches");
        applyPatches(workspace, patchDir);
        return workspace;
    }

    private static void checkoutUpstream(File dir) throws IOException {
        File repoFile = new File(dir, "repo");
        File shaFile = new File(dir, "sha");
        if (!repoFile.exists() || !shaFile.exists()) {
            System.out.println("Config files not found!");
            System.exit(1);
        }
        String repo = new String(Files.readAllBytes(repoFile.toPath()));
        String sha = new String(Files.readAllBytes(shaFile.toPath()));
        System.out.println("Preparing " + repo + " " + sha);
        FileUtil.delete(new File(dir, "upstream"));
        GitUtil.runGitCommand(dir, new String[] { "git", "clone", repo, "upstream" });
        GitUtil.runGitCommand(new File(dir, "upstream"), new String[] { "git", "branch", "-f", "upstream", sha });
        GitUtil.runGitCommand(new File(dir, "upstream"), new String[] { "git", "checkout", "upstream" });
    }

    private static void createWorkspace(File workspaceDir, File upstreamDir) throws IOException {
        FileUtil.delete(workspaceDir);
        workspaceDir.mkdir();
        GitUtil.runGitCommand(workspaceDir, new String[] { "git", "init" });
        GitUtil.runGitCommand(workspaceDir,
                new String[] { "git", "remote", "add", "upstream", upstreamDir.getAbsolutePath() });
        String branchname = GitUtil.runGitCommandGetBranchName(workspaceDir,
                new String[] { "git", "fetch", "upstream" });
        GitUtil.runGitCommand(workspaceDir, new String[] { "git", "reset", "--hard", "upstream/" + branchname });
        GitUtil.runGitCommand(workspaceDir, new String[] { "git", "config", "commit.gpgsign", "false" });
    }

    private static void applyPatches(File workspaceDir, File patchDir) throws IOException {
        System.out.println("Applying patches...");
        List<String> patches = Arrays.stream(patchDir.list()).filter(s -> s.toLowerCase().endsWith(".patch")).sorted()
                .collect(Collectors.toList());
        for (String patch : patches) {
            System.out.println("Applying " + patch);
            boolean fail = GitUtil.runGitCommandCheckFail(workspaceDir, new String[] { "git", "am", "--3way",
                    "--ignore-whitespace", "--reject", new File(patchDir, patch).getAbsolutePath() });
            if (fail) {
                System.out.println(patch + " did not apply cleanly!");
                System.exit(1);
            }
        }
        System.out.println("Patches applied cleanly!");
    }

    private static void rebuildPatches(File workspaceDir, File patchDir) throws IOException {
        String branchname = GitUtil.runGitCommandGetBranchName(workspaceDir, new String[] { "git", "branch", "-r" });
        GitUtil.runGitCommand(workspaceDir, new String[] { "git", "format-patch", "--quiet", "-N", "-o",
                patchDir.getAbsolutePath(), "upstream/" + branchname });
    }

}
