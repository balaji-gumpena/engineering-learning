.DEFAULT_GOAL := help
MODULE ?= 01-basic-llm-call
.PHONY: help start restart stop status build test verify
help:
	@printf '%s\n' 'make start MODULE=01-basic-llm-call' 'make restart | stop | status | build | test MODULE=...' 'make verify (all implemented modules)'
start restart stop status build test:
	@test -f "ai-engineering/$(MODULE)/Makefile" || { printf '%s\n' 'Choose an implemented module; planned folders remain empty.'; exit 1; }
	@$(MAKE) --no-print-directory -C "ai-engineering/$(MODULE)" $@
verify:
	@./verify.sh
