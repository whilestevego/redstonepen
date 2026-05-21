# @file Makefile
# @author Stefan Wilhelm (wile)
# @license MIT
#
# GNU Make makefile based build relay.
# Note for reviewers/clones: This file is a auxiliary script for my setup.
# It's not needed to build the mod.
#
TOOLS_DIR    := $(HOME)/.local/analysis-tools
KTFMT_JAR    := $(TOOLS_DIR)/ktfmt.jar
KTLINT_BIN   := $(TOOLS_DIR)/ktlint
DETEKT_JAR   := $(TOOLS_DIR)/detekt.jar
KRIT_JAR     := $(TOOLS_DIR)/krit.jar
KTFMT_VERSION  := 0.62
KTLINT_VERSION := 1.5.0
DETEKT_VERSION := 1.23.8
KRIT_VERSION   := 0.3.0

MOD_JAR_PREFIX=redstonepen-
MOD_JAR=$(filter-out %-sources.jar,$(wildcard build/libs/${MOD_JAR_PREFIX}*.jar))
ifneq ($(JDK_HOME_22_0),)
export JAVA_HOME=$(JDK_HOME_22_0)
export JDK_HOME=$(JDK_HOME_22_0)
endif

ifeq ($(OS),Windows_NT)
GRADLE=gradlew.bat --no-daemon
GRADLE_STOP=gradlew.bat --stop
else
GRADLE=./gradlew --no-daemon
GRADLE_STOP=./gradlew --stop
endif
TASK=djs -s ../../zmeta/lib/tasks.js

wildcardr=$(foreach d,$(wildcard $1*),$(call wildcardr,$d/,$2) $(filter $(subst *,%,$2),$d))

#
# Targets
#
.PHONY: default mod data init clean clean-all mrproper all run install sanitize dist-check dist start-server assets test coverage verify check format format-fix lint lint-fix analyze analyze-typed krit

default: mod

all: clean clean-all mod | install

mod:
	@echo "[1.21] Building mod using gradle ..."
	@$(GRADLE) build $(GRADLE_OPTS)

assets:
	@echo "[1.21] Running asset generators ..."
	@$(TASK) assets

data:
	@echo "[1.21] Running data generators ..."
	@$(TASK) datagen

clean:
	@echo "[1.21] Cleaning ..."
	@rm -rf src/generated
	@rm -rf mcmodsrepo
	@rm -f build/libs/*

clean-all:
	@echo "[1.21] Cleaning ..."
	@rm -rf mcmodsrepo
	@rm -f dist/*
	@rm -rf build/
	@rm -rf out/
	@rm -rf logs/
	@rm -rf run/logs/
	@rm -rf run/crash-reports/

mrproper: clean-all
	@rm -f meta/*.*
	@rm -rf run/
	@rm -f .project
	@rm -f .classpath

init:
	@echo "[1.21] Initialising eclipse workspace using gradle ..."
	@$(GRADLE) idea

sanitize:
	@echo "[1.21] Running sanitising tasks ..."
	@$(TASK) sanitize
	@$(TASK) sync-languages
	@$(TASK) version-check
	@$(TASK) update-json
	@git status -s .

install: $(MOD_JAR) |
	@$(TASK) install

start-server: install
	@$(TASK) start-server

dist-check:
	@echo "[1.21] Running dist checks ..."
	@$(TASK) dist-check

dist-files: clean-all init mod
	@echo "[1.21] Distribution files ..."
	@mkdir -p dist
	@cp build/libs/$(MOD_JAR_PREFIX)* dist/
	@$(TASK) dist

dist: sanitize dist-check dist-files
	@$(TASK) dist-sign

run:
	@$(GRADLE) runClient

test:
	@$(GRADLE) test

coverage:
	@$(GRADLE) coverage
	@echo "Report: build/reports/jacoco/test/index.html"

verify:
	@$(GRADLE) jacocoTestCoverageVerification

# --- Analysis tools (ktfmt, ktlint, detekt) ----------------------------------
# Tools are downloaded on first use to $(TOOLS_DIR) and cached there.
# 'check' mirrors what CI runs; 'format-fix' and 'lint-fix' apply corrections.

$(KTFMT_JAR):
	@mkdir -p $(TOOLS_DIR)
	@echo "Downloading ktfmt $(KTFMT_VERSION)..."
	@curl -sSLo $@ https://github.com/facebook/ktfmt/releases/download/v$(KTFMT_VERSION)/ktfmt-$(KTFMT_VERSION)-with-dependencies.jar

$(KTLINT_BIN):
	@mkdir -p $(TOOLS_DIR)
	@echo "Downloading ktlint $(KTLINT_VERSION)..."
	@curl -sSLo $@ https://github.com/pinterest/ktlint/releases/download/$(KTLINT_VERSION)/ktlint
	@chmod a+x $@

$(DETEKT_JAR):
	@mkdir -p $(TOOLS_DIR)
	@echo "Downloading detekt $(DETEKT_VERSION)..."
	@curl -sSLo $@ https://github.com/detekt/detekt/releases/download/v$(DETEKT_VERSION)/detekt-cli-$(DETEKT_VERSION)-all.jar

check: format lint analyze

format: $(KTFMT_JAR)
	@echo "Checking formatting (ktfmt)..."
	@java -jar $(KTFMT_JAR) --kotlinlang-style --dry-run --set-exit-if-changed \
	  $$(find . -name "*.kt" -not -path "*/build/*" -not -path "*/bin/*")

format-fix: $(KTFMT_JAR)
	@echo "Applying formatting (ktfmt)..."
	@java -jar $(KTFMT_JAR) --kotlinlang-style \
	  $$(find . -name "*.kt" -not -path "*/build/*" -not -path "*/bin/*")

lint: $(KTLINT_BIN)
	@echo "Linting (ktlint)..."
	@$(KTLINT_BIN) --reporter=plain '**/*.kt' '!**/build/**' '!**/bin/**'

lint-fix: $(KTLINT_BIN)
	@echo "Auto-correcting lint (ktlint)..."
	@$(KTLINT_BIN) --format --reporter=plain '**/*.kt' '!**/build/**' '!**/bin/**'

analyze: $(DETEKT_JAR)
	@echo "Analyzing common..."
	@java -jar $(DETEKT_JAR) --config config/detekt/detekt.yml --build-upon-default-config \
	  --baseline config/detekt/baseline-common.xml --input common/src --parallel
	@echo "Analyzing neoforge..."
	@java -jar $(DETEKT_JAR) --config config/detekt/detekt.yml --build-upon-default-config \
	  --baseline config/detekt/baseline-neoforge.xml --input neoforge/src --parallel
	@echo "Analyzing fabric..."
	@java -jar $(DETEKT_JAR) --config config/detekt/detekt.yml --build-upon-default-config \
	  --baseline config/detekt/baseline-fabric.xml --input fabric/src --parallel

analyze-typed:
	@echo "Analyzing with type resolution (detekt + classpath)..."
	@$(GRADLE) analyzeTyped

# --- krit (semantic analysis with K2 + IDE inspections) ---------------
KRIT_JAVA := $(shell \
  if [ -n "$$JAVA_HOME" ] && [ -x "$$JAVA_HOME/bin/java" ]; then \
    ver=$$($$JAVA_HOME/bin/java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p'); \
    [ "$${ver:-0}" -ge 25 ] 2>/dev/null && { printf '%s' "$$JAVA_HOME/bin/java"; exit 0; }; \
  fi; \
  printf '%s' "java")
KRIT := "$(KRIT_JAVA)" --enable-native-access=ALL-UNNAMED \
  --add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
  -jar $(KRIT_JAR) --common-checks --format text

$(KRIT_JAR): | $(TOOLS_DIR)
	@echo "Downloading krit $(KRIT_VERSION)..."
	@curl -sSLo $@ https://github.com/whilestevego/krit/releases/download/v$(KRIT_VERSION)/krit.jar

$(TOOLS_DIR):
	@mkdir -p $(TOOLS_DIR)

krit: $(KRIT_JAR)
	@echo "Resolving compile classpaths via Gradle..."
	@$(GRADLE) :common:writeCompileClasspath :neoforge:writeCompileClasspath :fabric:writeCompileClasspath -q
	@echo "Analyzing common..."
	@$(KRIT) --input common/src/main/kotlin --input common/src/main/java \
	  --classpath $$(cat common/build/compile-classpath.txt)
	@echo "Analyzing neoforge..."
	@$(KRIT) --input neoforge/src/main/kotlin --input neoforge/src/main/java \
	  --classpath $$(cat neoforge/build/compile-classpath.txt)
	@echo "Analyzing fabric..."
	@$(KRIT) --input fabric/src/main/kotlin --input fabric/src/main/java \
	  --classpath $$(cat fabric/build/compile-classpath.txt)
