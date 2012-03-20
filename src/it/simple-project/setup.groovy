// def gitcmd = "git"^M
// if (env['OS'] =~ /^Windows/)^M
// gitcmd = "cmd /c ${gitcmd}"^M



// make this test project a "git" reposioty by copying the .git directory
// from the sample project used on unit tests

if(! new File("target/sample_project/.git").isDirectory() ) {
    throw new FileNotFoundException("Couldn't find .git directory at target/sample_project/.git")
}

if(! new File("target/it/simple-project").isDirectory() ) {
    throw new FileNotFoundException("Couldn't find target directory at target/sample_project")
}

println "*** Copying .git dir into target/it/simple-project"
def p = "cp -r target/sample_project/.git/ target/it/simple-project".execute()
println p.text


println "*** Checking out 'develop' branch"
p = "git --git-dir=target/it/simple-project/.git checkout -f develop".execute()
println p.text
p.waitFor()

return true



