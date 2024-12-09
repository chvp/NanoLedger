# Default goal for "make"
.DEFAULT_GOAL = ledger

# Shell for target commands
SHELL = /bin/bash

# Disable built-in rules
MAKEFLAGS += --no-builtin-rules
.SUFFIXES:

## Path to build directory
BUILD ?= build

# Path to resulting AAR
AAR_PATH = $(BUILD)/ledger-release.aar

# Source file dependencies for accurate rebuild tracking
LEDGER_SOURCE_DEPS += .gitmodules

## Clean the build directory
clean:
	@rm -rf $(BUILD)

# === Git helpers ==============

# Ensure submodules are initialized after cloning the repository
check-submodules: $(BUILD)/.check-submodules

$(BUILD)/.check-submodules:
	@git submodule update --init
	@mkdir -p $(@D)
	@touch $@

# Install gomobile
install:
	@go install golang.org/x/mobile/cmd/gomobile@latest

# Initialize gomobile
init:
	@gomobile init

## Build Ledger AAR
ledger: $(AAR_PATH)

$(AAR_PATH):
	@mkdir -p $(@D)
	@gomobile bind -target=android -o $(AAR_PATH) .
	@touch $@

