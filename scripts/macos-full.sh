#!/bin/sh

cd installer || exit

echo "Compiling installer JAR"
./gradlew build

cd .. || exit

echo "Copying installer JAR to wrapper"
cp installer/build/libs/installer.jar wrapper/resources/

cd wrapper || exit

echo "Making sure required targets are added"
rustup target add x86_64-apple-darwin
rustup target add aarch64-apple-darwin

echo "Building wrapper"
cargo build --release --target x86_64-apple-darwin
cargo build --release --target aarch64-apple-darwin

cd ../scripts || exit

echo "Constructing final .app"
./make-macos-app.sh
