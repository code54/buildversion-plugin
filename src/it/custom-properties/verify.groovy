

// looking for timestamp with custom format yyyy-MM-dd

def build_log = new File("target/it/custom-properties/build.log").text

boolean tstamp = (build_log =~ /build-tstamp: \d\d\d\d-\d\d-\d\d/)


// expect an all-uppercased git commit hash 
boolean customProperty = ( build_log =~ /build-commit-uppercase: [0-9A-F]{40}/ )

return tstamp && customProperty

