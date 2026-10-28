.PHONY: all build run test clean jar stats

all: build

build:
	@chmod +x build.sh run.sh
	@./build.sh

run:
	@chmod +x run.sh
	@./run.sh

test:
	@echo "Running WordBook Test Suite..."
	@javac -cp "lib/sqlite-jdbc-3.49.1.0.jar:bin" -d bin $$(find src -name "*.java")
	@java --enable-native-access=ALL-UNNAMED -cp "lib/sqlite-jdbc-3.49.1.0.jar:bin" com.wordbook.WordBookTestSuite

stats:
	@java --enable-native-access=ALL-UNNAMED -cp "lib/sqlite-jdbc-3.49.1.0.jar:bin" com.wordbook.WordBookApplication --stats

jar: build

clean:
	@rm -rf bin wordbook.jar wordbook.db wordbook.db-shm wordbook.db-wal
	@find temp/classes -name "*.class" -delete 2>/dev/null || true
	@find temp/files -type f -not -name ".gitkeep" -delete 2>/dev/null || true
	@echo "Cleaned build artifacts and temporary directories."
