#!/bin/bash
set -e

LIBS_DIR="libs"
mkdir -p $LIBS_DIR

# 下载/libs/的依赖文件
wget -O $LIBS_DIR/IceAndFireCE-2.0-beta.7-1.21.1-neoforge.jar "https://cdn.modrinth.com/data/VpmCsizY/versions/SoRz04Bu/IceAndFireCE-2.0-beta.7-1.21.1-neoforge.jar"


echo "Dependencies downloaded successfully"