.PHONY: repl test emulator clean

repl:
	@JAVA_HOME=`/System/Library/Frameworks/JavaVM.framework/Versions/A/Commands/java_home -v "1.8"` \
	DATASTORE_EMULATOR_HOST=localhost:8081 \
	lein repl

test:
	@JAVA_HOME=`/System/Library/Frameworks/JavaVM.framework/Versions/A/Commands/java_home -v "1.8"` \
	DATASTORE_EMULATOR_HOST=localhost:8081 \
	lein test

emulator:
	gcloud beta emulators datastore start --no-store-on-disk

clean:
	-@lein clean
