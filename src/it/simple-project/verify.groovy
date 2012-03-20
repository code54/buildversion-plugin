
def targetString = "descriptive-version: 1.2.0-SNAPSHOT"

println "*** Checking build log output. Looking for ${targetString}"

def build_log = new File("target/it/simple-project/build.log").text

return build_log.contains(targetString)
