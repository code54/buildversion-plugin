

// looking for timestamp with custom format yyyy-MM-dd

def build_log = new File("target/it/custom-properties/build.log").text
boolean success = (build_log =~ /build-tstamp: \d\d\d\d-\d\d-\d\d/)
return success

