
// looking for usage of "git.sh", because we specified that as the git command to use

def build_log = new File("target/it/setting-git-cmd/build.log").text

return build_log.contains("git.sh")

