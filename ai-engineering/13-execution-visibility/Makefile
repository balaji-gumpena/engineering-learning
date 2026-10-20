.DEFAULT_GOAL := help
PORT ?= 8000
COMMERCE_PORT ?= 8080
LIFECYCLE := support/lifecycle/learning/lifecycle/ModuleLifecycle.java
.PHONY: help start restart stop status build test lifecycle-test
help:
	@printf '%s\n' 'make start | restart | stop | status | build | test' 'UI: http://127.0.0.1:$(PORT)' 'Override ports with PORT=... COMMERCE_PORT=...'
start:
	@java $(LIFECYCLE) start "$(PORT)" "$(COMMERCE_PORT)"
stop:
	@java $(LIFECYCLE) stop "$(PORT)" "$(COMMERCE_PORT)"
restart:
	@$(MAKE) --no-print-directory stop
	@$(MAKE) --no-print-directory start
status:
	@java $(LIFECYCLE) status "$(PORT)" "$(COMMERCE_PORT)"
build:
	@./gradlew --no-daemon --console=plain bootJar
	@if [ -d commerce ]; then cd commerce && ./gradlew --no-daemon --console=plain bootJar; fi
test:
	@./gradlew --no-daemon --console=plain test bootJar
	@if [ -d commerce ]; then cd commerce && ./gradlew --no-daemon --console=plain test bootJar; fi
	@node --test ui-tests/chat.test.cjs
lifecycle-test:
	@java support/lifecycle/learning/lifecycle/LifecycleSmokeTest.java
