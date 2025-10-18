{
  description = "Nanoledger android app";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    devshell = {
      url = "github:numtide/devshell";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs = { self, nixpkgs, devshell, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; config = { android_sdk.accept_license = true; allowUnfree = true; }; overlays = [ devshell.overlays.default ]; };
        buildToolsVersion = "36.0.0";
        composed = pkgs.androidenv.composeAndroidPackages {
          buildToolsVersions = [ "35.0.0" buildToolsVersion ];
          platformVersions = [ "36" ];
        };
        fhsEnv = pkgs.buildFHSEnv {
          name = "android-sdk-env";
          targetPkgs = pkgs: (with pkgs; [ glibc ]);
          profile = ''
            export ANDROID_SDK_ROOT="${composed.androidsdk}/libexec/android-sdk/"
          '';
        };
      in
      {
        devShells = rec {
          default = nanoledger;
          nanoledger = pkgs.devshell.mkShell {
            name = "Nanoledger";
            packages = [ pkgs.jdk17 pkgs.nixpkgs-fmt ];
            env = [
              { name = "ANDROID_SDK_ROOT"; eval = "${composed.androidsdk}/libexec/android-sdk/"; }
              { name = "BUILD_TOOLS_PATH"; eval = "$ANDROID_SDK_ROOT/build-tools/${buildToolsVersion}"; }
              { name = "APK_DIR"; eval = "$PRJ_ROOT/app/build/outputs/apk/release"; }
            ];
            devshell = {
              motd = "";
              startup = {
                # Hack to get the nix-managed SDK in android studio
                "link-devshell-dir".text = ''
                  mkdir -p $PRJ_DATA_DIR
                  ln -snf $DEVSHELL_DIR $PRJ_DATA_DIR/devshell
                  ln -snf ${composed.androidsdk}/libexec/android-sdk/ $PRJ_DATA_DIR/sdk
                '';
              };
            };
            commands = [
              {
                name = "gradle";
                category = "tools";
                help = "Working gradle invocation";
                command = "${fhsEnv}/bin/android-sdk-env \"$PRJ_ROOT/gradlew\" $@";
              }
              {
                name = "sign-release";
                category = "tools";
                help = "Build a signed APK";
                command = ''
                  rm -rf "$APK_DIR/"*
                  gradle assembleRelease
                  "$BUILD_TOOLS_PATH/zipalign" -v -p 4 "$APK_DIR/app-release-unsigned.apk" "$APK_DIR/app-release-unsigned-aligned.apk"

                  "$BUILD_TOOLS_PATH/apksigner" sign --ks "$PRJ_ROOT/keystore.jks" --out "$APK_DIR/app-release.apk" "$APK_DIR/app-release-unsigned-aligned.apk"
                  "$BUILD_TOOLS_PATH/apksigner" verify "$APK_DIR/app-release.apk"
                '';
              }
              {
                name = "install-debug-signed-release";
                category = "tools";
                help = "Install a debug signed release APK";
                command = ''
                  rm -rf "$APK_DIR/"*
                  gradle assembleRelease
                  "$BUILD_TOOLS_PATH/zipalign" -v -p 4 "$APK_DIR/app-release-unsigned.apk" "$APK_DIR/app-release-unsigned-aligned.apk"
                  echo android | "$BUILD_TOOLS_PATH/apksigner" sign --ks "$HOME/.android/debug.keystore" --out "$APK_DIR/app-release.apk" "$APK_DIR/app-release-unsigned-aligned.apk"
                  adb install -r "$APK_DIR/app-release.apk"
                '';
              }
            ];
          };
        };
      }
    );
}
