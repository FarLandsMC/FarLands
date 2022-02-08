#! /bin/bash
# How to run this script: `bash create-fl-server.bash`
#                           (It takes no arguments)
#
# Note: This script requires `wget` and `jq` to run properly.
#
# This script will create a new directory called `fl-server` where the server files, like the paper jar, will be stored.
# It will create another script in `fl-server` called `start-server.bash` that can be used for starting the server.
# It will also create a `plugins` directory in `fl-server` where the necessary plugins will be downloaded to.
# There will be another script in `plugins` called `update-plugins.bash` that can be used to update the dependency plugins.

CYAN='\e[1;36m'
GREEN='\e[1;32m'
BLUE='\e[1;34m'
RED='\e[1;31m'
END='\e[0m'

echo -e "${CYAN}Starting to create FarLands server..."
echo -e "If you have any issues with this script please contact Majekdor.${END}"
echo

mkdir -p fl-server
cd fl-server || exit

# Download latest paper build
echo "Downloading latest Paper version..."
VERSION=$(wget -q -O- https://papermc.io/api/v2/projects/paper | jq -r '.versions[-1]')
BUILD=$(wget -q -O- https://papermc.io/api/v2/projects/paper/versions/"${VERSION}" | jq -r '.builds[-1]')
FILE="paper-${VERSION}-${BUILD}.jar"
URL="https://papermc.io/api/v2/projects/paper/versions/1.18.1/builds/${BUILD}/downloads/${FILE}"
wget -q -O 'paper-server.jar' "$URL"
if [ $? -ne 0 ]; then
  echo -e "${RED}Failed to get latest Paper jar.${END}"
else
  echo -e "${GREEN}Downloaded Paper version ${BLUE}${VERSION} ${GREEN}build ${BLUE}${BUILD} ${GREEN}to paper-server.jar${END}"
fi

# Create start server script
echo "#! /bin/bash
      # bash start-server.bash

      CYAN='\e[1;36m'
      GREEN='\e[1;32m'
      BLUE='\e[1;34m'
      RED='\e[1;31m'
      END='\e[0m'

      echo -e \"\${CYAN}Starting server...\"
      echo -e \"If you have any issues with this script please contact Majekdor.\${END}\"
      echo

      # Ask if they want to go ahead and accept the eula
      read -p \"Would you like to download the latest Paper jar? [Y/N] \" -n 1 -r
      echo
      if [[ \$REPLY =~ ^[Yy]$ ]]
      then
        # Download latest paper build
        echo \"Downloading latest Paper version...\"
        VERSION=\$(wget -q -O- https://papermc.io/api/v2/projects/paper | jq -r '.versions[-1]')
        BUILD=\$(wget -q -O- https://papermc.io/api/v2/projects/paper/versions/\"\${VERSION}\" | jq -r '.builds[-1]')
        FILE=\"paper-\${VERSION}-\${BUILD}.jar\"
        URL=\"https://papermc.io/api/v2/projects/paper/versions/1.18.1/builds/\${BUILD}/downloads/\${FILE}\"
        wget -q -O 'paper-server.jar' \"\$URL\"
        if [ \$? -ne 0 ]; then
          echo -e \"\${RED}Failed to get latest Paper jar.\${END}\"
        else
          echo -e \"\${GREEN}Downloaded Paper version \${BLUE}\${VERSION} \${GREEN}build \${BLUE}\${BUILD} \${GREEN}to paper-server.jar\${END}\"
        fi
      fi

      echo \"Starting server...\"
      java -jar paper-server.jar nogui
" > start-server.bash

# Ask if they want to go ahead and accept the eula
read -p "Would you like to go ahead and accept the Minecraft EULA (https://account.mojang.com/documents/minecraft_eula)? [Y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
  echo "Writing accepted EULA..."
  echo "eula=true" > eula.txt
fi

echo "Beginning to download plugins..."
mkdir -p plugins
cd plugins || exit

# Create update plugins script
echo "#! /bin/bash
      # bash update-plugins.bash

      CYAN='\e[1;36m'
      GREEN='\e[1;32m'
      BLUE='\e[1;34m'
      RED='\e[1;31m'
      END='\e[0m'

      # download_jar_from_gh <user|org> <repo> <jar-output>
      download_jar_from_gh () {
        TAG=\$(wget -q -O- \"https://api.github.com/repos/\$1/\$2/releases/latest\" | jq -r '.tag_name')
        DL=\$(wget -q -O- \"https://api.github.com/repos/\$1/\$2/releases/latest\" | jq -r '.assets[0].browser_download_url')
        wget -q -O \"\$3\" \"\$DL\"
        if [ \$? -ne 0 ]; then
          rm \"\$3\"
          echo -e \"\${RED}Failed to get latest \$3\${END}\"
        else
          echo -e \"\${GREEN}Downloaded \${BLUE}\$3 \${GREEN}from tag \${BLUE}\${TAG}\${END}\"
        fi
      }
      # download_jar_from_majek <group-id> <artifact-id> <jar-output>
      # use / not . in artifact id
      download_jar_from_majek () {
        LATEST_VER=\$(wget -q -O- \"https://repo.majek.dev/snapshots/\$1/\$2/latest\")
        FILE=\$(wget -q -O- https://repo.majek.dev/api/snapshots/\$1/\$2/\"\${LATEST_VER}\" | jq -r '[ .files[] | select((.name | contains(\"javadoc\") | not) and (.name | contains(\"sources\") | not) and (.contentType == \"application/java-archive\")) ][0].name')
        wget -q -O \"\$3\" \"https://repo.majek.dev/snapshots/\$1/\$2/\${LATEST_VER}/\${FILE}\"
        if [ \$? -ne 0 ]; then
          rm \"\$3\"
          echo -e \"\${RED}Failed to get latest \$3\${END}\"
        else
          echo -e \"\${GREEN}Downloaded \${BLUE}\$3 \${GREEN}from snapshot file \${BLUE}\${FILE}\${END}\"
        fi
      }

      # Download Region Protection
      echo 'Downloading Region Protection...'
      download_jar_from_majek com/kicas/rp regionprotection RegionProtection.jar

      # Download Chest Shops
      echo 'Downloading Chest Shops...'
      download_jar_from_majek com/kicasmads/cs chestshops ChestShops.jar

      # Download ProtocolLib
      echo 'Downloading ProtocolLib...'
      download_jar_from_gh dmulloy2 ProtocolLib ProtocolLib.jar

      # Download NuVotifier
      echo 'Downloading NuVotifier...'
      download_jar_from_gh NuVotifier NuVotifier NuVotifier.jar

      # Download CoreProtect
      echo 'Downloading CoreProtect...'
      download_jar_from_gh PlayPro CoreProtect CoreProtect.jar

      # Ask if they want to download the FarLands plugin
      read -p \"Would you like to go ahead and download the latest FarLands plugin? If you have your own to test you don't need this. [Y/N] \" -n 1 -r
      echo
      if [[ \$REPLY =~ ^[Yy]$ ]]
      then
        echo 'Downloading FarLands...'
        download_jar_from_majek net/farlands/sanctuary farlands FarLands.jar
      fi

      echo 'Finished downloading dependency plugins.'
" > update-plugins.bash

# Download plugins
bash update-plugins.bash

# Ask if they want to download spark
read -p "Would you like to download spark for performance profiling? [Y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
  echo "Downloading spark..."
  FILE=$(wget -q -O- https://ci.lucko.me/job/spark/lastStableBuild/api/json | jq -r "[ .artifacts[] | select(.fileName | contains(\"bukkit\")) ][0].displayPath")
  wget -q -O "spark.jar" "https://ci.lucko.me/job/spark/lastStableBuild/artifact/spark-bukkit/build/libs/${FILE}"
  if [ $? -ne 0 ]; then
    rm 'spark.jar'
    echo -e "${RED}Failed to get latest spark.jar${END}"
  else
    echo -e "${GREEN}Downloaded latest spark (${BLUE}${FILE}${GREEN}) from jenkins${END}"
  fi
fi

echo
echo -e "${CYAN}Finished! Now all you need to do is cd fl-server and run the start-server.bash script."
echo -e "Note: There is another script in the plugins folder called update-plugins.bash that will download the latest version of dependency plugins.${END}"
