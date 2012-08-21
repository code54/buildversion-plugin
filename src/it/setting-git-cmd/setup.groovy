
// make git.sh wrapper  executable.
// Need to do this because maven-invoker-plugin's cloneProjectsTo doesn't seem to preserve
// permissions
println "*** Ensure git.sh is executable"
def p = "chmod +x target/it/setting-git-cmd/git.sh".execute()
p.waitFor()

return true



