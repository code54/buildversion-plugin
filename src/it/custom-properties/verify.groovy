

// looking for timestamp with custom format yyyy-MM-dd

def build_log = new File("target/it/custom-properties/build.log").text

boolean tstamp = (build_log =~ /build-tstamp: \d\d\d\d-\d\d-\d\d/)

boolean customLowerCaseProperty = ( build_log =~ /build-tag-lowercase: n\/a/ )

return tstamp && customLowerCaseProperty

